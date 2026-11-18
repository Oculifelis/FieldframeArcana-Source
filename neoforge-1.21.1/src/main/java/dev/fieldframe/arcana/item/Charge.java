package dev.fieldframe.arcana.item;

import dev.fieldframe.arcana.data.ArcanaTuning;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * RF held in an item's {@code fieldframe_arcana:energy} component. Each powered item names a tuning group; its
 * capacity is {@code <group>.capacity}. The NeoForge energy capability on these items uses the same component, so
 * Fieldframe's chargers (and any other mod's) fill them.
 */
public final class Charge {
    /** Implemented by every Arcana item that holds RF. */
    public interface Powered {
        String tuningGroup();
    }

    private Charge() {
    }

    public static int capacity(ItemStack stack) {
        return stack.getItem() instanceof Powered p ? (int) ArcanaTuning.get(p.tuningGroup() + ".capacity") : 0;
    }

    public static int stored(ItemStack stack) {
        return Math.min(capacity(stack), stack.getOrDefault(ArcanaComponents.ENERGY.get(), 0));
    }

    public static boolean has(ItemStack stack, int rf) {
        return stored(stack) >= rf;
    }

    /** Takes exactly {@code rf}, or nothing; true when paid. */
    public static boolean use(ItemStack stack, int rf) {
        if (rf <= 0) {
            return true;
        }
        int s = stored(stack);
        if (s < rf) {
            return false;
        }
        stack.set(ArcanaComponents.ENERGY.get(), s - rf);
        return true;
    }

    public static void fill(ItemStack stack) {
        stack.set(ArcanaComponents.ENERGY.get(), capacity(stack));
    }

    public static int barWidth(ItemStack stack) {
        int cap = capacity(stack);
        return cap <= 0 ? 0 : Math.round(13f * stored(stack) / cap);
    }

    public static int barColor(ItemStack stack) {
        int cap = capacity(stack);
        float f = cap <= 0 ? 0 : (float) stored(stack) / cap;
        return Mth.hsvToRgb(0.55f + 0.2f * f, 0.8f, 1f);
    }

    /** A message on the action bar. */
    public static void overlay(Player player, Component message) {
        player.displayClientMessage(message, true);
    }

    /** True when {@code stack} is the item {@code owner} wears in {@code slot}. */
    public static boolean worn(net.minecraft.world.entity.Entity owner, ItemStack stack, net.minecraft.world.entity.EquipmentSlot slot) {
        return owner instanceof net.minecraft.world.entity.LivingEntity l && l.getItemBySlot(slot) == stack;
    }

    public static Component tooltip(ItemStack stack) {
        return Component.translatable("tooltip.fieldframe_arcana.energy", stored(stack), capacity(stack)).withStyle(net.minecraft.ChatFormatting.GRAY);
    }

    /**
     * Takes exactly {@code rf} from one energy item the player carries, never from {@code except}; true when paid.
     * Arcana items are drawn last, so a carried cell or battery pays before a tool or armour piece does.
     */
    public static boolean payFromInventory(Player player, ItemStack except, int rf) {
        if (rf <= 0) {
            return true;
        }
        Inventory inv = player.getInventory();
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (s.isEmpty() || s == except || (s.getItem() instanceof Powered) != (pass == 1)) {
                    continue;
                }
                IEnergyStorage h = s.getCapability(Capabilities.EnergyStorage.ITEM);
                if (h == null || h.getEnergyStored() < rf || h.extractEnergy(rf, true) != rf) {
                    continue;
                }
                if (h.extractEnergy(rf, false) == rf) {
                    return true;
                }
            }
        }
        return false;
    }
}
