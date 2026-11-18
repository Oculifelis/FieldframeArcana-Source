package dev.fieldframe.arcana.item;

import dev.fieldframe.arcana.FieldframeArcana;
import dev.fieldframe.arcana.data.ArcanaTuning;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Server-side upkeep of the galvanic armour.
 * <ul>
 *   <li>Boots: while on and charged, extra step height and speed, paid {@code galvanic_boots.rf_per_metre} for the
 *   ground covered.</li>
 *   <li>Wings: glide like an elytra; while on, charged and gliding, a gentle thrust
 *   along the look direction costs {@code galvanic_wings.rf_per_tick}; sparks trail from both wing tips while it
 *   pushes.</li>
 *   <li>Jetpack: on and charged, holding jump lifts the wearer ({@code galvanic_jetpack.thrust}, up to
 *   {@code max_rise}) for {@code rf_per_tick}, with a little air control from the movement keys. In hover mode, not
 *   holding jump holds the fall at {@code hover_fall} for {@code hover_rf_per_tick}; sneaking drops normally. Either way
 *   no fall damage builds up and the server's floating kick is held off.</li>
 * </ul>
 */
public final class ArcanaArmorEvents {
    private static final ResourceLocation STEP = FieldframeArcana.id("galvanic_boots_step");
    private static final ResourceLocation SPEED = FieldframeArcana.id("galvanic_boots_speed");
    /** Fractions of RF owed for distance, per player, so short steps are still paid for. */
    private static final Map<UUID, Double> OWED = new WeakHashMap<>();

    private ArcanaArmorEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ArcanaArmorEvents::tick);
    }

    private static void tick(PlayerTickEvent.Post e) {
        Player player = e.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        boots(player, player.getItemBySlot(EquipmentSlot.FEET));
        wings(player, player.getItemBySlot(EquipmentSlot.CHEST));
        if (player instanceof ServerPlayer sp) {
            jetpack(sp, sp.getItemBySlot(EquipmentSlot.CHEST));
        }
    }

    private static void boots(Player player, ItemStack boots) {
        boolean active = boots.getItem() instanceof GalvanicArmorItem g && "galvanic_boots".equals(g.tuningGroup())
                && GalvanicArmorItem.enabled(boots) && Charge.stored(boots) > 0;
        if (active) {
            double metres = Math.hypot(player.getX() - player.xo, player.getZ() - player.zo);
            double owed = OWED.getOrDefault(player.getUUID(), 0.0) + metres * ArcanaTuning.get("galvanic_boots.rf_per_metre");
            int whole = (int) Math.floor(owed);
            if (whole > 0) {
                Charge.use(boots, Math.min(whole, Charge.stored(boots)));
                owed -= whole;
            }
            OWED.put(player.getUUID(), owed);
        }
        modifier(player, Attributes.STEP_HEIGHT, STEP, active ? ArcanaTuning.get("galvanic_boots.step_height") : 0,
                AttributeModifier.Operation.ADD_VALUE);
        modifier(player, Attributes.MOVEMENT_SPEED, SPEED, active ? ArcanaTuning.get("galvanic_boots.speed_bonus") : 0,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    private static void modifier(Player player, Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) {
            return;
        }
        AttributeModifier current = inst.getModifier(id);
        if (amount == 0) {
            if (current != null) {
                inst.removeModifier(id);
            }
        } else if (current == null || current.amount() != amount) {
            inst.removeModifier(id);
            inst.addTransientModifier(new AttributeModifier(id, amount, op));
        }
    }

    private static void wings(Player player, ItemStack wings) {
        if (!(wings.getItem() instanceof GalvanicArmorItem g) || !"galvanic_wings".equals(g.tuningGroup()) || !player.isFallFlying()
                || !GalvanicArmorItem.enabled(wings)) {
            return;
        }
        if (!Charge.use(wings, (int) ArcanaTuning.get("galvanic_wings.rf_per_tick"))) {
            return;
        }
        Vec3 look = player.getLookAngle();
        double thrust = ArcanaTuning.get("galvanic_wings.thrust");
        Vec3 v = player.getDeltaMovement();
        double max = ArcanaTuning.get("galvanic_wings.max_speed");
        if (v.dot(look) < max) {
            player.setDeltaMovement(v.add(look.scale(thrust)));
            player.hurtMarked = true;
        }
        if (player.tickCount % 2 == 0 && player.level() instanceof ServerLevel level) {
            wingSparks(level, player, look);
        }
    }

    private static void jetpack(ServerPlayer player, ItemStack pack) {
        int mode = pack.getItem() instanceof GalvanicJetpackItem ? GalvanicJetpackItem.mode(pack) : GalvanicJetpackItem.OFF;
        if (mode == GalvanicJetpackItem.OFF || player.getAbilities().flying || player.isFallFlying() || player.isPassenger()) {
            return;
        }
        Vec3 v = player.getDeltaMovement();
        boolean lift = JetpackInput.held(player, JetpackInput.JUMP);
        boolean hover = !lift && mode == GalvanicJetpackItem.HOVER && !player.onGround() && !JetpackInput.held(player, JetpackInput.SHIFT);
        // (No check on v.y: a player's motion is the client's, so the server's copy is stale; hold it every tick.)
        if (!lift && !hover || !Charge.use(pack, (int) ArcanaTuning.get(lift ? "galvanic_jetpack.rf_per_tick" : "galvanic_jetpack.hover_rf_per_tick"))) {
            return;
        }
        double y = lift ? Math.min(v.y + ArcanaTuning.get("galvanic_jetpack.thrust"), ArcanaTuning.get("galvanic_jetpack.max_rise"))
                : ArcanaTuning.get("galvanic_jetpack.hover_fall");
        double steer = ArcanaTuning.get("galvanic_jetpack.air_control");
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        double fwd = (JetpackInput.held(player, JetpackInput.FORWARD) ? 1 : 0) - (JetpackInput.held(player, JetpackInput.BACK) ? 1 : 0);
        double strafe = (JetpackInput.held(player, JetpackInput.LEFT) ? 1 : 0) - (JetpackInput.held(player, JetpackInput.RIGHT) ? 1 : 0);
        double dx = (strafe * Math.cos(yaw) - fwd * Math.sin(yaw)) * steer, dz = (fwd * Math.cos(yaw) + strafe * Math.sin(yaw)) * steer;
        player.setDeltaMovement(v.x + dx, y, v.z + dz);
        player.hurtMarked = true;
        player.resetFallDistance();
        player.connection.aboveGroundTickCount = 0;
        if (player.level() instanceof ServerLevel level) {
            exhaust(level, player, lift);
        }
    }

    /** Two exhaust plumes from the pack's nozzles: sparks and a little smoke while lifting, a faint glow while hovering. */
    private static void exhaust(ServerLevel level, Player player, boolean lift) {
        float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 back = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw)).scale(0.3);
        Vec3 side = new Vec3(Math.cos(yaw), 0, Math.sin(yaw)).scale(0.16);
        Vec3 c = player.position().add(0, player.getBbHeight() * 0.45, 0).add(back);
        for (Vec3 n : new Vec3[] {c.add(side), c.subtract(side)}) {
            if (lift) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, n.x, n.y, n.z, 2, 0.03, 0.05, 0.03, 0.02);
                level.sendParticles(ParticleTypes.SMOKE, n.x, n.y - 0.1, n.z, 1, 0.02, 0.0, 0.02, 0.01);
            } else if (player.tickCount % 3 == 0) {
                level.sendParticles(ParticleTypes.END_ROD, n.x, n.y - 0.1, n.z, 1, 0.01, 0.0, 0.01, 0.0);
            }
        }
        if (lift && player.tickCount % 8 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_BURN, SoundSource.PLAYERS, 0.25f, 1.7f);
        }
    }

    /** Electric sparks and a faint glow behind each wing tip, seen by everyone nearby. */
    private static void wingSparks(ServerLevel level, Player player, Vec3 look) {
        Vec3 side = look.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize().scale(0.8);
        Vec3 c = player.position().add(0, player.getBbHeight() * 0.5, 0).subtract(look.scale(0.5));
        for (Vec3 tip : new Vec3[] {c.add(side), c.subtract(side)}) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, tip.x, tip.y, tip.z, 3, 0.08, 0.08, 0.08, 0.05);
            level.sendParticles(ParticleTypes.END_ROD, tip.x, tip.y, tip.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }
}
