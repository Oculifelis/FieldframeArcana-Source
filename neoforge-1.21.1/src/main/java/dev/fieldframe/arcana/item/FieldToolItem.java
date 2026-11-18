package dev.fieldframe.arcana.item;

import dev.fieldframe.arcana.data.ArcanaTuning;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An RF tool: no durability, each block costs {@code <group>.rf_per_block}. Sneak-use cycles {@link #modes()}, kept
 * in the {@code fieldframe_arcana:mode} component. An empty tool digs at a crawl ({@link ArcanaToolEvents}).
 */
public abstract class FieldToolItem extends Item implements Charge.Powered {
    protected FieldToolItem(Properties props) {
        super(props);
    }

    /** Mode names (lang keys {@code mode.fieldframe_arcana.<name>}); index 0 is the default. */
    protected abstract String[] modes();

    public static int mode(ItemStack stack) {
        return stack.getOrDefault(ArcanaComponents.MODE.get(), 0);
    }

    public int rfPerBlock() {
        return (int) ArcanaTuning.get(tuningGroup() + ".rf_per_block");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        ItemStack stack = player.getItemInHand(hand);
        int next = (mode(stack) + 1) % modes().length;
        stack.set(ArcanaComponents.MODE.get(), next);
        if (!level.isClientSide()) {
            dev.fieldframe.arcana.item.Charge.overlay(player, Component.translatable("fieldframe_arcana.mode_set",
                    Component.translatable("mode.fieldframe_arcana." + modes()[next])).withStyle(ChatFormatting.AQUA));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (!level.isClientSide() && state.getDestroySpeed(level, pos) != 0f) {
            Charge.use(stack, Math.min(Charge.stored(stack), rfPerBlock()));
        }
        return true;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 14;
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
        out.add(Component.translatable("tooltip.fieldframe_arcana.mode", Component.translatable("mode.fieldframe_arcana." + modes()[mode(stack)]))
                .withStyle(ChatFormatting.AQUA));
        out.add(Component.translatable("tooltip.fieldframe_arcana." + tuningGroup()).withStyle(ChatFormatting.DARK_PURPLE));
    }

    /** RF pickaxe and shovel: normal, 3x3 and refining (smelts drops, paid in RF and the chunk's vis). */
    public static class Drill extends FieldToolItem {
        public static final int NORMAL = 0, AREA = 1, REFINING = 2;

        public Drill(Properties props) {
            super(props);
        }

        @Override
        public String tuningGroup() {
            return "field_drill";
        }

        @Override
        protected String[] modes() {
            return new String[] {"normal", "area", "refining"};
        }
    }

    /** RF axe and shears: normal, and felling (a whole tree at once, paid per log plus a little vis). */
    public static class Saw extends FieldToolItem {
        public static final int NORMAL = 0, FELLING = 1;

        public Saw(Properties props) {
            super(props);
        }

        @Override
        public String tuningGroup() {
            return "field_saw";
        }

        @Override
        protected String[] modes() {
            return new String[] {"normal", "felling"};
        }
    }
}
