package dev.fieldframe.arcana.item;

import dev.fieldframe.arcana.data.ArcanaTuning;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Galvanic Thaumium Sword: never wears out. Use switches it on or off (the {@code fieldframe_arcana:active}
 * component, which the item model shows as an animated glowing blade). Off, it is a plain sword and costs nothing. On,
 * it costs {@code galvanic_sword.rf_per_tick} wherever it is carried, each hit costs {@code rf_per_hit} more and adds
 * {@code bonus_damage}, and it arcs to the nearest hostile mob within {@code arc_range} of the target for
 * {@code arc_rf}, dealing {@code arc_damage} as magic. It switches itself off when the charge runs out.
 */
public class GalvanicSwordItem extends Item implements Charge.Powered {
    public GalvanicSwordItem(Properties props) {
        super(props);
    }

    @Override
    public String tuningGroup() {
        return "galvanic_sword";
    }

    public static boolean active(ItemStack stack) {
        return stack.has(ArcanaComponents.ACTIVE.get());
    }

    private static int rfPerHit() {
        return (int) ArcanaTuning.get("galvanic_sword.rf_per_hit");
    }

    private static void switchTo(ItemStack stack, boolean on, Level level, Entity owner) {
        if (on) {
            stack.set(ArcanaComponents.ACTIVE.get(), Unit.INSTANCE);
        } else {
            stack.remove(ArcanaComponents.ACTIVE.get());
        }
        level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), on ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, 0.5f, on ? 1.9f : 1.6f);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean on = !active(stack);
        if (on && !Charge.has(stack, (int) ArcanaTuning.get("galvanic_sword.rf_per_tick") * 20)) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("fieldframe_arcana.galvanic_sword.empty").withStyle(ChatFormatting.RED));
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            switchTo(stack, on, level, player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (!active(stack)) {
            return;
        }
        if (!Charge.use(stack, (int) ArcanaTuning.get("galvanic_sword.rf_per_tick"))) {
            Charge.use(stack, Charge.stored(stack));
            switchTo(stack, false, level, owner);
            return;
        }
        if (slot == EquipmentSlot.MAINHAND && owner.tickCount % 3 == 0) {
            Vec3 look = owner.getLookAngle();
            Vec3 side = look.cross(new Vec3(0, 1, 0)).normalize().scale(0.35);
            Vec3 p = owner.position().add(0, owner.getBbHeight() * 0.6, 0).add(look.scale(0.6)).add(side);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.08, 0.15, 0.08, 0.02);
        }
    }

    @Override
    public float getAttackDamageBonus(Entity target, float damage, DamageSource source) {
        ItemStack weapon = source.getWeaponItem();
        return weapon != null && active(weapon) && Charge.has(weapon, rfPerHit())
                ? (float) ArcanaTuning.get("galvanic_sword.bonus_damage") : 0f;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(attacker.level() instanceof ServerLevel level) || !active(stack) || !Charge.use(stack, rfPerHit())) {
            return;
        }
        LivingEntity next = GalvanicArc.nearest(level, target, attacker, ArcanaTuning.get("galvanic_sword.arc_range"));
        if (next != null && Charge.use(stack, (int) ArcanaTuning.get("galvanic_sword.arc_rf"))) {
            GalvanicArc.zap(level, target, next, attacker, (float) ArcanaTuning.get("galvanic_sword.arc_damage"));
        }
        GalvanicArc.burst(level, target);
    }

    /** The charge changes every tick while it is on; only a different item, slot or on/off state re-equips it. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack) || active(oldStack) != active(newStack);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Charge.barWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Charge.barColor(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        out.accept(Charge.tooltip(stack));
        out.accept(active(stack)
                ? Component.translatable("tooltip.fieldframe_arcana.galvanic_sword.on").withStyle(ChatFormatting.AQUA)
                : Component.translatable("tooltip.fieldframe_arcana.galvanic_sword.off").withStyle(ChatFormatting.GRAY));
        out.accept(Component.translatable("tooltip.fieldframe_arcana.galvanic_sword").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
