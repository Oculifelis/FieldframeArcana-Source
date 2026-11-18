package dev.fieldframe.arcana.wand;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.wands.IWandRodOnUpdate;
import com.leclowndu93150.thaumaturge.api.wands.IWandVisStorage;
import com.leclowndu93150.thaumaturge.api.wands.WandVis;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import dev.fieldframe.arcana.data.ArcanaTuning;
import dev.fieldframe.arcana.item.ArcanaComponents;
import dev.fieldframe.arcana.item.Charge;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The Voltaic wand and staff core (silverwood tier). The wand keeps its vis in Thaumaturge's own component, but every
 * time vis leaves a primal, {@code voltaic_core.rf_share} of what left is put back and paid from an RF buffer in the
 * wand's {@code fieldframe_arcana:energy} component instead, at {@code rf_per_vis} RF per vis. With an empty buffer
 * the wand pays in full, like any other. Every {@code interval_ticks} the buffer tops itself up by
 * {@code rf_per_pulse} from an energy item the player carries. All numbers: {@link ArcanaTuning}.
 */
public final class VoltaicCore implements IWandRodOnUpdate, IWandVisStorage {
    private final boolean staff;

    public VoltaicCore(boolean staff) {
        this.staff = staff;
    }

    public int bufferCapacity() {
        return (int) (ArcanaTuning.get("voltaic_core.buffer") * (staff ? ArcanaTuning.get("voltaic_core.staff_multiplier") : 1));
    }

    public static int buffer(ItemStack wand) {
        return wand.getOrDefault(ArcanaComponents.ENERGY.get(), 0);
    }

    @Override
    public WandVis getVis(ItemStack wand) {
        return wand.getOrDefault(TTDataComponents.WAND_VIS.get(), WandVis.EMPTY);
    }

    @Override
    public void setVis(ItemStack wand, WandVis next) {
        WandVis old = getVis(wand);
        double share = ArcanaTuning.get("voltaic_core.rf_share");
        double rfPerVis = ArcanaTuning.get("voltaic_core.rf_per_vis");
        int buffer = buffer(wand);
        int spent = 0;
        WandVis out = next;
        for (ResourceKey<IAspect> p : TTAspects.PRIMALS) {
            int drop = old.amount(p) - next.amount(p);
            if (drop <= 0) {
                continue;
            }
            int refund = (int) Math.round(drop * share);
            int rf = (int) Math.ceil(refund * rfPerVis / 100.0);
            if (rf > buffer - spent) {
                refund = (int) Math.floor((buffer - spent) * 100.0 / rfPerVis);
                rf = (int) Math.ceil(refund * rfPerVis / 100.0);
            }
            if (refund > 0) {
                out = out.with(p, next.amount(p) + refund);
                spent += rf;
            }
        }
        wand.set(TTDataComponents.WAND_VIS.get(), out);
        if (spent > 0) {
            wand.set(ArcanaComponents.ENERGY.get(), buffer - spent);
        }
    }

    @Override
    public void onUpdate(ItemStack wand, Player player) {
        if (player.level().isClientSide() || player.level().getGameTime() % (int) ArcanaTuning.get("voltaic_core.interval_ticks") != 0) {
            return;
        }
        int room = bufferCapacity() - buffer(wand);
        int rf = Math.min(room, (int) ArcanaTuning.get("voltaic_core.rf_per_pulse"));
        if (rf > 0 && Charge.payFromInventory(player, wand, rf)) {
            wand.set(ArcanaComponents.ENERGY.get(), buffer(wand) + rf);
        }
    }
}
