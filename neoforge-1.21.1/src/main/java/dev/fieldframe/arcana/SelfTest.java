package dev.fieldframe.arcana;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.wands.WandVis;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import dev.fieldframe.arcana.data.ArcanaTuning;
import dev.fieldframe.arcana.item.JetpackInput;
import dev.fieldframe.arcana.item.ArcanaComponents;
import dev.fieldframe.arcana.item.Charge;
import dev.fieldframe.arcana.item.GalvanicBowItem;
import dev.fieldframe.arcana.item.GalvanicJetpackItem;
import dev.fieldframe.arcana.item.GalvanicSwordItem;
import dev.fieldframe.arcana.wand.VoltaicCore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Dev-server checks of the Voltaic core and the Galvanic sword, run once the server has started when the JVM has
 * {@code -Dfieldframe_arcana.selftest=true} (only the Gradle server run sets it). Logs one {@code SELFTEST PASS} or
 * {@code SELFTEST FAIL} line per check. Chunk 0,0 is force-loaded at start and the checks run 20 ticks later, once its
 * entities can be found (at the moment of start nothing there is visible to entity lookups).
 */
final class SelfTest {
    private static int countdown = -1;

    private SelfTest() {
    }

    static void register() {
        if (Boolean.getBoolean("fieldframe_arcana.selftest")) {
            NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> {
                e.getServer().overworld().setChunkForced(0, 0, true);
                countdown = 20;
            });
            NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
                if (countdown > 0 && --countdown == 0) {
                    ServerLevel level = e.getServer().overworld();
                    run(level);
                    level.setChunkForced(0, 0, false);
                }
            });
        }
    }

    private static void check(String name, boolean ok, String detail) {
        FieldframeArcana.LOG.info("SELFTEST {} {}: {}", ok ? "PASS" : "FAIL", name, detail);
    }

    private static void run(ServerLevel level) {
        try {
            voltaic();
            sword(level);
            bow(level);
            jetpack(level);
            data(level);
        } catch (Exception ex) {
            FieldframeArcana.LOG.error("SELFTEST FAIL crashed", ex);
        }
    }

    /** A tuning file overrides a default and a bad value is skipped; the tool tags carry their default blocks. */
    private static void data(ServerLevel level) {
        java.util.Map<String, Double> saved = ArcanaTuning.snapshot();
        try {
            com.google.gson.JsonObject group = new com.google.gson.JsonObject();
            group.addProperty("bonus_damage", 3);
            group.addProperty("arc_range", 999);
            com.google.gson.JsonObject file = new com.google.gson.JsonObject();
            file.add("galvanic_sword", group);
            ArcanaTuning.load(java.util.Map.of(FieldframeArcana.id("selftest"), file));
            double bonus = ArcanaTuning.get("galvanic_sword.bonus_damage");
            double range = ArcanaTuning.get("galvanic_sword.arc_range");
            check("tuning_override", bonus == 3 && range == 4, "bonus " + bonus + " range " + range);
        } finally {
            ArcanaTuning.restore(saved);
        }
        ItemStack drill = new ItemStack(ARegistry.FIELD_DRILL.get());
        ItemStack saw = new ItemStack(ARegistry.FIELD_SAW.get());
        var stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        var log = net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState();
        var wool = net.minecraft.world.level.block.Blocks.WHITE_WOOL.defaultBlockState();
        check("tool_tags", stone.is(ARegistry.DRILL_MINEABLE) && log.is(ARegistry.SAW_MINEABLE) && wool.is(ARegistry.SAW_MINEABLE)
                && drill.getDestroySpeed(stone) == 10f && saw.getDestroySpeed(log) == 10f && drill.getDestroySpeed(log) == 1f,
                "drill/stone " + drill.getDestroySpeed(stone) + " saw/log " + saw.getDestroySpeed(log) + " drill/log " + drill.getDestroySpeed(log));
    }

    private static WandVis full(int centivis) {
        Map<ResourceKey<IAspect>, Integer> m = new HashMap<>();
        for (ResourceKey<IAspect> p : TTAspects.PRIMALS) {
            m.put(p, centivis);
        }
        return new WandVis(m);
    }

    private static void voltaic() {
        VoltaicCore core = new VoltaicCore(false);
        double rfPerVis = ArcanaTuning.get("voltaic_core.rf_per_vis");
        int primals = TTAspects.PRIMALS.size();
        // 10 vis drawn from every primal: half comes back, paid with 5 vis of RF per primal.
        ItemStack wand = new ItemStack(Items.STICK);
        wand.set(TTDataComponents.WAND_VIS.get(), full(5000));
        wand.set(ArcanaComponents.ENERGY.get(), 50000);
        core.setVis(wand, full(4000));
        int expectRf = 50000 - primals * (int) Math.ceil(500 * rfPerVis / 100.0);
        WandVis after = core.getVis(wand);
        boolean visOk = TTAspects.PRIMALS.stream().allMatch(p -> after.amount(p) == 4500);
        check("voltaic_half_refund", visOk && VoltaicCore.buffer(wand) == expectRf,
                "vis " + after.centivis().values() + ", buffer " + VoltaicCore.buffer(wand) + " (want 4500 each, " + expectRf + ")");
        // Empty buffer: the wand pays in full and the buffer stays at 0.
        ItemStack dry = new ItemStack(Items.STICK);
        dry.set(TTDataComponents.WAND_VIS.get(), full(5000));
        core.setVis(dry, full(4000));
        WandVis dryAfter = core.getVis(dry);
        check("voltaic_empty_pays_full", TTAspects.PRIMALS.stream().allMatch(p -> dryAfter.amount(p) == 4000) && VoltaicCore.buffer(dry) == 0,
                "vis " + dryAfter.centivis().values() + ", buffer " + VoltaicCore.buffer(dry));
        // Refilling vis (a charger) costs nothing.
        ItemStack fill = new ItemStack(Items.STICK);
        fill.set(TTDataComponents.WAND_VIS.get(), full(1000));
        fill.set(ArcanaComponents.ENERGY.get(), 9000);
        core.setVis(fill, full(3000));
        check("voltaic_refill_free", core.getVis(fill).amount(TTAspects.PRIMALS.get(0)) == 3000 && VoltaicCore.buffer(fill) == 9000,
                "buffer " + VoltaicCore.buffer(fill));
        // A buffer too small for a full refund gives what it can and never goes below zero.
        ItemStack low = new ItemStack(Items.STICK);
        low.set(TTDataComponents.WAND_VIS.get(), full(5000));
        low.set(ArcanaComponents.ENERGY.get(), 1000);
        core.setVis(low, full(4000));
        check("voltaic_partial_buffer", VoltaicCore.buffer(low) >= 0 && VoltaicCore.buffer(low) < 1000,
                "buffer " + VoltaicCore.buffer(low) + ", vis " + core.getVis(low).centivis().values());
    }

    private static Zombie zombie(ServerLevel level, double x, double y, double z) {
        Zombie zb = EntityType.ZOMBIE.create(level);
        zb.setPos(x, y, z);
        zb.setNoAi(true);
        level.addFreshEntity(zb);
        return zb;
    }

    private static void sword(ServerLevel level) {
        level.getChunk(0, 0);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, 8, 8) + 1;
        List<Zombie> spawned = new ArrayList<>();
        Zombie attacker = zombie(level, 6, y, 8), target = zombie(level, 8, y, 8), near = zombie(level, 10, y, 8);
        spawned.add(attacker);
        spawned.add(target);
        spawned.add(near);
        try {
            ItemStack sword = new ItemStack(ARegistry.GALVANIC_SWORD.get());
            Charge.fill(sword);
            attacker.setItemInHand(InteractionHand.MAIN_HAND, sword);
            int full = Charge.stored(sword);
            // Off: a plain sword, no bonus, no arc, no RF.
            float offBonus = sword.getItem().getAttackDamageBonus(target, 7f, level.damageSources().mobAttack(attacker));
            float hp0 = near.getHealth();
            sword.getItem().postHurtEnemy(sword, target, attacker);
            sword.getItem().inventoryTick(sword, level, attacker, 0, true);
            check("sword_off_plain", offBonus == 0f && near.getHealth() == hp0 && Charge.stored(sword) == full,
                    "bonus " + offBonus + ", rf " + Charge.stored(sword) + " of " + full);

            sword.set(ArcanaComponents.ACTIVE.get(), Unit.INSTANCE);
            float bonus = sword.getItem().getAttackDamageBonus(target, 7f, level.damageSources().mobAttack(attacker));
            check("sword_on_bonus", bonus == (float) ArcanaTuning.get("galvanic_sword.bonus_damage"), "bonus " + bonus);
            float hp = near.getHealth();
            sword.getItem().postHurtEnemy(sword, target, attacker);
            int want = full - (int) ArcanaTuning.get("galvanic_sword.rf_per_hit") - (int) ArcanaTuning.get("galvanic_sword.arc_rf");
            check("sword_on_arc", near.getHealth() < hp && Charge.stored(sword) == want,
                    "near hp " + hp + " -> " + near.getHealth() + ", rf " + full + " -> " + Charge.stored(sword) + " (want " + want + ")");
            check("sword_no_self_arc", attacker.getHealth() == attacker.getMaxHealth(), "attacker hp " + attacker.getHealth());
            int before = Charge.stored(sword);
            sword.getItem().inventoryTick(sword, level, attacker, 0, true);
            check("sword_on_drain", before - Charge.stored(sword) == (int) ArcanaTuning.get("galvanic_sword.rf_per_tick"),
                    "rf " + before + " -> " + Charge.stored(sword));

            // Running dry switches it off.
            sword.set(ArcanaComponents.ENERGY.get(), 5);
            sword.getItem().inventoryTick(sword, level, attacker, 0, true);
            check("sword_auto_off", !GalvanicSwordItem.active(sword) && Charge.stored(sword) == 0, "active " + GalvanicSwordItem.active(sword));
            check("sword_no_wear", !sword.isDamageableItem(), "damageable " + sword.isDamageableItem());
        } finally {
            spawned.forEach(Zombie::discard);
        }
    }

    private static FakePlayer player(ServerLevel level, int y) {
        FakePlayer p = FakePlayerFactory.getMinecraft(level);
        p.setPos(8.5, y, 8.5);
        p.setDeltaMovement(Vec3.ZERO);
        p.setOnGround(false);
        return p;
    }

    private static void bow(ServerLevel level) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, 8, 8) + 3;
        FakePlayer p = player(level, y);
        ItemStack bow = new ItemStack(ARegistry.GALVANIC_BOW.get());
        Charge.fill(bow);
        p.setItemInHand(InteractionHand.MAIN_HAND, bow);
        int full = Charge.stored(bow);
        check("bow_fast_draw", GalvanicBowItem.power(bow, (int) ArcanaTuning.get("galvanic_bow.draw_ticks")) == 1f,
                "power at draw_ticks " + GalvanicBowItem.power(bow, (int) ArcanaTuning.get("galvanic_bow.draw_ticks")));
        List<AbstractArrow> before = level.getEntitiesOfClass(AbstractArrow.class, p.getBoundingBox().inflate(8));
        bow.getItem().releaseUsing(bow, level, p, bow.getUseDuration(p) - 20);
        List<AbstractArrow> after = level.getEntitiesOfClass(AbstractArrow.class, p.getBoundingBox().inflate(8),
                a -> !before.contains(a));
        boolean shot = !after.isEmpty();
        boolean bolt = after.size() == 1 && after.get(0).getTags().contains(GalvanicBowItem.BOLT)
                && after.get(0).pickup == AbstractArrow.Pickup.CREATIVE_ONLY;
        check("bow_bolt", shot && bolt && full - Charge.stored(bow) == (int) ArcanaTuning.get("galvanic_bow.rf_per_shot")
                        && p.getInventory().countItem(Items.ARROW) == 0,
                "shot " + shot + ", new arrows " + after.size() + ", bolt " + bolt + ", rf " + full + " -> " + Charge.stored(bow));
        after.forEach(AbstractArrow::discard);
        // Quick fire: sneak-use switches the mode, then a plain use fires a full-power bolt at once and starts the cooldown.
        p.setShiftKeyDown(true);
        bow.getItem().use(level, p, InteractionHand.MAIN_HAND);
        p.setShiftKeyDown(false);
        boolean switched = GalvanicBowItem.mode(bow) == GalvanicBowItem.QUICK;
        int beforeQuick = Charge.stored(bow);
        List<AbstractArrow> beforeQ = level.getEntitiesOfClass(AbstractArrow.class, p.getBoundingBox().inflate(8));
        bow.getItem().use(level, p, InteractionHand.MAIN_HAND);
        List<AbstractArrow> quick = level.getEntitiesOfClass(AbstractArrow.class, p.getBoundingBox().inflate(8), a -> !beforeQ.contains(a));
        boolean quickBolt = quick.size() == 1 && quick.get(0).getTags().contains(GalvanicBowItem.BOLT) && quick.get(0).isCritArrow();
        check("bow_quick_fire", switched && quickBolt && !p.isUsingItem()
                        && beforeQuick - Charge.stored(bow) == GalvanicBowItem.quickCost()
                        && p.getCooldowns().isOnCooldown(bow.getItem()),
                "switched " + switched + ", new arrows " + quick.size() + ", bolt " + quickBolt + ", using " + p.isUsingItem()
                        + ", rf " + beforeQuick + " -> " + Charge.stored(bow) + ", cooldown " + p.getCooldowns().isOnCooldown(bow.getItem()));
        quick.forEach(AbstractArrow::discard);
        ItemStack emptyQuick = new ItemStack(ARegistry.GALVANIC_BOW.get());
        emptyQuick.set(ArcanaComponents.MODE.get(), GalvanicBowItem.QUICK);
        p.setItemInHand(InteractionHand.MAIN_HAND, emptyQuick);
        List<AbstractArrow> beforeE = level.getEntitiesOfClass(AbstractArrow.class, p.getBoundingBox().inflate(8));
        emptyQuick.getItem().use(level, p, InteractionHand.MAIN_HAND);
        check("bow_quick_empty_plain", level.getEntitiesOfClass(AbstractArrow.class, p.getBoundingBox().inflate(8), a -> !beforeE.contains(a)).isEmpty(),
                "empty quick-fire bow fired nothing without arrows");
        p.stopUsingItem();
        ItemStack empty = new ItemStack(ARegistry.GALVANIC_BOW.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, empty);
        List<AbstractArrow> beforeN = level.getEntitiesOfClass(AbstractArrow.class, p.getBoundingBox().inflate(8));
        empty.getItem().releaseUsing(empty, level, p, empty.getUseDuration(p) - 20);
        check("bow_empty_needs_arrows", level.getEntitiesOfClass(AbstractArrow.class, p.getBoundingBox().inflate(8), a -> !beforeN.contains(a)).isEmpty(),
                "shot with no arrows");
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    private static void jetpack(ServerLevel level) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, 8, 8) + 10;
        FakePlayer p = player(level, y);
        ItemStack pack = new ItemStack(ARegistry.GALVANIC_JETPACK.get());
        Charge.fill(pack);
        p.setItemSlot(EquipmentSlot.CHEST, pack);
        int full = Charge.stored(pack);
        JetpackInput.set(p, JetpackInput.JUMP);
        p.fallDistance = 5;
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
        check("jetpack_lift", p.getDeltaMovement().y > 0 && full - Charge.stored(pack) == (int) ArcanaTuning.get("galvanic_jetpack.rf_per_tick")
                && p.fallDistance == 0, "vy " + p.getDeltaMovement().y + ", rf used " + (full - Charge.stored(pack)) + ", fall " + p.fallDistance);
        // Hover: not holding jump while falling fast, the fall is held at hover_fall.
        pack.set(ArcanaComponents.MODE.get(), GalvanicJetpackItem.HOVER);
        JetpackInput.set(p, 0);
        p.setDeltaMovement(0, -0.8, 0);
        int mid = Charge.stored(pack);
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
        check("jetpack_hover", p.getDeltaMovement().y == ArcanaTuning.get("galvanic_jetpack.hover_fall")
                && mid - Charge.stored(pack) == (int) ArcanaTuning.get("galvanic_jetpack.hover_rf_per_tick"), "vy " + p.getDeltaMovement().y);
        // Off: nothing.
        pack.set(ArcanaComponents.MODE.get(), GalvanicJetpackItem.OFF);
        JetpackInput.set(p, JetpackInput.JUMP);
        p.setDeltaMovement(0, -0.5, 0);
        int off = Charge.stored(pack);
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
        check("jetpack_off", p.getDeltaMovement().y == -0.5 && Charge.stored(pack) == off, "vy " + p.getDeltaMovement().y);
        p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        // Armored: flies like the plain pack and wears like a thaumium chestplate, charged or not.
        ItemStack armored = new ItemStack(ARegistry.ARMORED_GALVANIC_JETPACK.get());
        Charge.fill(armored);
        p.setItemSlot(EquipmentSlot.CHEST, armored);
        JetpackInput.set(p, JetpackInput.JUMP);
        p.setDeltaMovement(0, 0, 0);
        int afull = Charge.stored(armored);
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
        check("armored_jetpack_lift", p.getDeltaMovement().y > 0 && afull - Charge.stored(armored) == (int) ArcanaTuning.get("galvanic_jetpack.rf_per_tick")
                && afull == (int) ArcanaTuning.get("armored_galvanic_jetpack.capacity"), "vy " + p.getDeltaMovement().y + ", full " + afull);
        ItemStack plate = new ItemStack(ARegistry.ARMORED_GALVANIC_JETPACK.get());
        double armor = plate.getAttributeModifiers().modifiers().stream()
                .filter(e -> e.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR))
                .mapToDouble(e -> e.modifier().amount()).sum();
        check("armored_jetpack_armor", armor == 7 && Charge.stored(plate) == 0, "armor " + armor);
        p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        JetpackInput.set(p, 0);
    }
}
