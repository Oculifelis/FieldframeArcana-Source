package dev.fieldframe.arcana.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * RF-powered armour piece (Galvanic Boots of the Traveller, Galvanic Wings). What it does while worn is in
 * {@link ArcanaArmorEvents}; sneak-use toggles it on and off ({@code fieldframe_arcana:mode}: 0 on, 1 off).
 */
public class GalvanicArmorItem extends Item implements Charge.Powered {
    private final String group;

    public GalvanicArmorItem(Properties props, String group) {
        super(props);
        this.group = group;
    }

    @Override
    public String tuningGroup() {
        return group;
    }

    public static boolean enabled(ItemStack stack) {
        return stack.getOrDefault(ArcanaComponents.MODE.get(), 0) == 0;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        }
        ItemStack stack = player.getItemInHand(hand);
        boolean on = !enabled(stack);
        stack.set(ArcanaComponents.MODE.get(), on ? 0 : 1);
        if (!level.isClientSide()) {
            player.sendOverlayMessage(Component.translatable(on ? "fieldframe_arcana.toggle_on" : "fieldframe_arcana.toggle_off",
                    stack.getHoverName()).withStyle(ChatFormatting.AQUA));
        }
        return InteractionResult.SUCCESS;
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
        out.accept(Component.translatable(enabled(stack) ? "tooltip.fieldframe_arcana.on" : "tooltip.fieldframe_arcana.off")
                .withStyle(ChatFormatting.AQUA));
        out.accept(Component.translatable("tooltip.fieldframe_arcana." + group).withStyle(ChatFormatting.DARK_PURPLE));
    }
}
