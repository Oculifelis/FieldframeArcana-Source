package dev.fieldframe.arcana.item;

import dev.fieldframe.arcana.data.ArcanaTuning;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * The Galvanic Thaumium Bow: never wears out. Charged, it needs no arrows: each shot costs
 * {@code galvanic_bow.rf_per_shot} and fires a bolt (an arrow nobody can pick up) that draws in {@code draw_ticks}
 * instead of 20, hits for {@code bonus_damage} more and arcs to the nearest hostile mob within {@code arc_range} of
 * what it hit for {@code arc_damage}. Empty, it is a plain bow that shoots the player's arrows.
 *
 * <p>Sneak-use switches the mode ({@code fieldframe_arcana:mode}): draw (0, hold and release as above) or quick fire
 * (1): while charged, each use fires a full-power bolt at once for {@code rf_per_shot * quick_rf_multiplier}, then the
 * bow cools down for {@code quick_cooldown_ticks}. Empty, quick fire draws like a plain bow.
 */
public class GalvanicBowItem extends BowItem implements Charge.Powered {
    /** Entity tag on bolts, read when they hit ({@link #register()}). */
    public static final String BOLT = "fieldframe_arcana.galvanic_bolt";
    /** Set while a paid shot is being fired, so {@link #customArrow} makes it a bolt. */
    private static final ThreadLocal<Boolean> FIRING_BOLT = ThreadLocal.withInitial(() -> false);

    public static final int DRAW = 0, QUICK = 1;

    public GalvanicBowItem(Properties props) {
        super(props);
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(GalvanicBowItem::hurt);
    }

    @Override
    public String tuningGroup() {
        return "galvanic_bow";
    }

    private static boolean charged(ItemStack bow) {
        return Charge.has(bow, (int) ArcanaTuning.get("galvanic_bow.rf_per_shot"));
    }

    public static int mode(ItemStack bow) {
        return bow.getOrDefault(ArcanaComponents.MODE.get(), DRAW);
    }

    private static String name(int mode) {
        return mode == QUICK ? "bow_quick" : "bow_draw";
    }

    /** RF one quick-fire shot costs. */
    public static int quickCost() {
        return (int) Math.round(ArcanaTuning.get("galvanic_bow.rf_per_shot") * ArcanaTuning.get("galvanic_bow.quick_rf_multiplier"));
    }

    /** How far drawn the bow is, 0 to 1, the vanilla curve stretched over {@code draw_ticks} when charged. */
    public static float power(ItemStack bow, int ticks) {
        float draw = charged(bow) ? (float) ArcanaTuning.get("galvanic_bow.draw_ticks") : MAX_DRAW_DURATION;
        float pow = ticks / Math.max(1f, draw);
        return Math.min(1f, (pow * pow + pow * 2f) / 3f);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack bow = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            int next = mode(bow) == QUICK ? DRAW : QUICK;
            bow.set(ArcanaComponents.MODE.get(), next);
            if (!level.isClientSide()) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_LOADING_END.value(),
                        SoundSource.PLAYERS, 0.6f, next == QUICK ? 1.6f : 1.1f);
                dev.fieldframe.arcana.item.Charge.overlay(player, Component.translatable("fieldframe_arcana.mode_set",
                        Component.translatable("mode.fieldframe_arcana." + name(next))).withStyle(ChatFormatting.AQUA));
            }
            return InteractionResultHolder.sidedSuccess(bow, level.isClientSide());
        }
        if (mode(bow) == QUICK && Charge.has(bow, quickCost())) {
            quickFire(level, player, hand, bow);
            return InteractionResultHolder.sidedSuccess(bow, level.isClientSide());
        }
        if (charged(bow)) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(bow);
        }
        return super.use(level, player, hand);
    }

    /** Quick fire: one full-power bolt now, paid for, then the item cooldown. */
    public void quickFire(Level level, Player player, InteractionHand hand, ItemStack bow) {
        if (level instanceof ServerLevel server) {
            Charge.use(bow, quickCost());
            fireBolt(server, player, hand, bow, 1f);
        }
        playShot(level, player, 1f);
        player.getCooldowns().addCooldown(this, (int) ArcanaTuning.get("galvanic_bow.quick_cooldown_ticks"));
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    private void fireBolt(ServerLevel server, Player player, InteractionHand hand, ItemStack bow, float pow) {
        FIRING_BOLT.set(true);
        try {
            shoot(server, player, hand, bow, List.of(new ItemStack(Items.ARROW)), pow * 3f, 1f, pow == 1f, null);
        } finally {
            FIRING_BOLT.set(false);
        }
    }

    private static void playShot(Level level, Player player, float pow) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1f,
                1f / (level.getRandom().nextFloat() * 0.4f + 1.2f) + pow * 0.5f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.4f, 1.8f);
    }

    @Override
    public void releaseUsing(ItemStack bow, Level level, LivingEntity entity, int remainingTime) {
        if (!(entity instanceof Player player)) {
            return;
        }
        if (!charged(bow)) {
            super.releaseUsing(bow, level, entity, remainingTime);
            return;
        }
        int held = net.neoforged.neoforge.event.EventHooks.onArrowLoose(bow, level, player, getUseDuration(bow, entity) - remainingTime, true);
        float pow = held < 0 ? 0f : power(bow, held);
        if (pow < 0.1f) {
            return;
        }
        if (level instanceof ServerLevel server) {
            Charge.use(bow, (int) ArcanaTuning.get("galvanic_bow.rf_per_shot"));
            fireBolt(server, player, player.getUsedItemHand(), bow, pow);
        }
        playShot(level, player, pow);
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow, ItemStack projectile, ItemStack bow) {
        if (FIRING_BOLT.get()) {
            arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            arrow.setBaseDamage(2.0 + ArcanaTuning.get("galvanic_bow.bonus_damage"));   // vanilla arrows start at 2
            arrow.addTag(BOLT);
        }
        return arrow;
    }

    private static void hurt(LivingDamageEvent.Post e) {
        if (!(e.getEntity().level() instanceof ServerLevel level) || e.getSource().getDirectEntity() == null
                || !e.getSource().getDirectEntity().getTags().contains(BOLT)) {
            return;
        }
        LivingEntity shooter = e.getSource().getEntity() instanceof LivingEntity l ? l : null;
        LivingEntity next = GalvanicArc.nearest(level, e.getEntity(), shooter, ArcanaTuning.get("galvanic_bow.arc_range"));
        if (next != null) {
            GalvanicArc.zap(level, e.getEntity(), next, shooter, (float) ArcanaTuning.get("galvanic_bow.arc_damage"));
        }
        GalvanicArc.burst(level, e.getEntity());
    }

    /** Paying for a shot changes the charge; that alone never re-equips the bow. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
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
    public void appendHoverText(ItemStack stack, TooltipContext ctx, java.util.List<Component> out, TooltipFlag flag) {
        out.add(Charge.tooltip(stack));
        out.add(Component.translatable("tooltip.fieldframe_arcana.mode",
                Component.translatable("mode.fieldframe_arcana." + name(mode(stack)))).withStyle(ChatFormatting.GRAY));
        out.add(Component.translatable("tooltip.fieldframe_arcana.galvanic_bow").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
