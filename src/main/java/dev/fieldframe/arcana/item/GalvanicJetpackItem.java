package dev.fieldframe.arcana.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The Galvanic Jetpack, worn on the chest. Sneak-use cycles on, hover and off ({@code fieldframe_arcana:mode} 0, 2,
 * 1, so {@link GalvanicArmorItem#enabled} still means "on"). What it does while worn is in {@link ArcanaArmorEvents}.
 * The Armored Galvanic Jetpack is the same pack under thaumium chest plating: it flies the same, holds more charge
 * (its own tuning group) and keeps its armour when empty.
 */
public class GalvanicJetpackItem extends GalvanicArmorItem {
    public static final int ON = 0, OFF = 1, HOVER = 2;

    private final String group;

    public GalvanicJetpackItem(Holder<ArmorMaterial> material, Properties props) {
        this(material, props, "galvanic_jetpack");
    }

    public GalvanicJetpackItem(Holder<ArmorMaterial> material, Properties props, String group) {
        super(material, Type.CHESTPLATE, props, group);
        this.group = group;
    }

    /** True for the plated pack; the worn layer draws its own texture. */
    public boolean armored() {
        return !group.equals("galvanic_jetpack");
    }

    public static int mode(ItemStack stack) {
        return stack.getOrDefault(ArcanaComponents.MODE.get(), ON);
    }

    private static String name(int mode) {
        return mode == HOVER ? "hover" : mode == OFF ? "off" : "on";
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        }
        ItemStack stack = player.getItemInHand(hand);
        int next = switch (mode(stack)) {
            case ON -> HOVER;
            case HOVER -> OFF;
            default -> ON;
        };
        stack.set(ArcanaComponents.MODE.get(), next);
        if (!level.isClientSide()) {
            dev.fieldframe.arcana.item.Charge.overlay(player, Component.translatable("fieldframe_arcana.mode_set",
                    Component.translatable("mode.fieldframe_arcana.jetpack_" + name(next))).withStyle(ChatFormatting.AQUA));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, java.util.List<Component> out, TooltipFlag flag) {
        out.add(Charge.tooltip(stack));
        out.add(Component.translatable("tooltip.fieldframe_arcana.mode",
                Component.translatable("mode.fieldframe_arcana.jetpack_" + name(mode(stack)))).withStyle(ChatFormatting.GRAY));
        out.add(Component.translatable("tooltip.fieldframe_arcana.galvanic_jetpack").withStyle(ChatFormatting.DARK_PURPLE));
        if (armored()) {
            out.add(Component.translatable("tooltip.fieldframe_arcana.armored_galvanic_jetpack").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}
