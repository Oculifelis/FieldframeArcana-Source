package dev.fieldframe.arcana.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * RF-powered armour piece (Galvanic Boots of the Traveller, Galvanic Wings). What it does while worn is in
 * {@link ArcanaArmorEvents}; sneak-use toggles it on and off ({@code fieldframe_arcana:mode}: 0 on, 1 off).
 */
public class GalvanicArmorItem extends ArmorItem implements Charge.Powered {
    private final String group;

    public GalvanicArmorItem(Holder<ArmorMaterial> material, Type type, Properties props, String group) {
        super(material, type, props);
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
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        }
        ItemStack stack = player.getItemInHand(hand);
        boolean on = !enabled(stack);
        stack.set(ArcanaComponents.MODE.get(), on ? 0 : 1);
        if (!level.isClientSide()) {
            dev.fieldframe.arcana.item.Charge.overlay(player, Component.translatable(on ? "fieldframe_arcana.toggle_on" : "fieldframe_arcana.toggle_off",
                    stack.getHoverName()).withStyle(ChatFormatting.AQUA));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** The wings glide like an elytra, wearing a little every second in the air. */
    @Override
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        return "galvanic_wings".equals(group);
    }

    @Override
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        if (!entity.level().isClientSide() && (flightTicks + 1) % 20 == 0) {
            stack.hurtAndBreak(1, entity, EquipmentSlot.CHEST);
        }
        return "galvanic_wings".equals(group);
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
        out.add(Component.translatable(enabled(stack) ? "tooltip.fieldframe_arcana.on" : "tooltip.fieldframe_arcana.off")
                .withStyle(ChatFormatting.AQUA));
        out.add(Component.translatable("tooltip.fieldframe_arcana." + group).withStyle(ChatFormatting.DARK_PURPLE));
    }
}
