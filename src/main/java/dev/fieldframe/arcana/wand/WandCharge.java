package dev.fieldframe.arcana.wand;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.wands.WandAccess;
import com.leclowndu93150.thaumaturge.content.wands.WandVisHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;

/**
 * Vis arithmetic on a wand, in centivis (100 per vis). Thaumaturge 1.0.2's API reads vis ({@link WandAccess}) but has
 * no capacity query or in-place write, so those two go through {@code WandVisHelper} (see docs/DESIGN.md).
 */
public final class WandCharge {
    private WandCharge() {
    }

    public static int capacity(ItemStack wand) {
        return WandVisHelper.capacityOf(wand);
    }

    /** Centivis still missing over all primals. */
    public static int missing(ItemStack wand) {
        int cap = capacity(wand);
        int sum = 0;
        for (ResourceKey<IAspect> p : TTAspects.PRIMALS) {
            sum += Math.max(0, cap - WandAccess.getVis(wand, p));
        }
        return sum;
    }

    public static double fraction(ItemStack wand) {
        int cap = capacity(wand) * TTAspects.PRIMALS.size();
        return cap <= 0 ? 1 : 1 - (double) missing(wand) / cap;
    }

    /** Adds up to {@code centivis} to {@code wand} in place, always topping up the lowest primal; returns what was added. */
    public static int fill(ItemStack wand, int centivis) {
        return fill(wand, centivis, capacity(wand));
    }

    /** Like {@link #fill(ItemStack, int)} but never raises a primal above {@code ceiling}. */
    public static int fill(ItemStack wand, int centivis, int ceiling) {
        int added = 0;
        while (added < centivis) {
            ResourceKey<IAspect> low = null;
            int lowVis = Integer.MAX_VALUE;
            for (ResourceKey<IAspect> p : TTAspects.PRIMALS) {
                int v = WandAccess.getVis(wand, p);
                if (v < ceiling && v < lowVis) {
                    low = p;
                    lowVis = v;
                }
            }
            if (low == null) {
                break;
            }
            // Raise the lowest primal to the next one up (or the ceiling) in one go, bounded by what is left.
            int next = ceiling;
            for (ResourceKey<IAspect> p : TTAspects.PRIMALS) {
                int v = WandAccess.getVis(wand, p);
                if (v > lowVis && v < next) {
                    next = v;
                }
            }
            int give = Math.max(1, Math.min(next - lowVis, centivis - added));
            WandVisHelper.setStored(wand, low, lowVis + give);
            added += give;
        }
        return added;
    }
}
