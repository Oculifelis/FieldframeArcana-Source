package dev.fieldframe.arcana.wand;

import com.leclowndu93150.thaumaturge.api.wands.IWandRodOnUpdate;
import dev.fieldframe.arcana.data.ArcanaTuning;
import dev.fieldframe.arcana.item.Charge;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The Galvanic rod and staff's update: every {@code galvanic_rod.interval_ticks} it buys {@code centivis_per_pulse}
 * of vis for {@code rf_per_vis} RF per vis from an energy item the player carries, and gives it to the lowest primal
 * (up to {@code max_fill} of capacity). The staff buys {@code staff_multiplier} times as much per pulse. It never
 * touches the aura. All numbers: {@link ArcanaTuning}.
 */
public final class GalvanicRod implements IWandRodOnUpdate {
    private final boolean staff;

    public GalvanicRod(boolean staff) {
        this.staff = staff;
    }

    @Override
    public void onUpdate(ItemStack wand, Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        int interval = (int) ArcanaTuning.get("galvanic_rod.interval_ticks");
        if (player.level().getGameTime() % interval != 0) {
            return;
        }
        int ceiling = (int) Math.floor(WandCharge.capacity(wand) * ArcanaTuning.get("galvanic_rod.max_fill"));
        double pulse = ArcanaTuning.get("galvanic_rod.centivis_per_pulse") * (staff ? ArcanaTuning.get("galvanic_rod.staff_multiplier") : 1);
        ItemStack trial = wand.copy();
        int want = WandCharge.fill(trial, (int) pulse, ceiling);
        if (want <= 0) {
            return;
        }
        int rf = (int) Math.ceil(want * ArcanaTuning.get("galvanic_rod.rf_per_vis") / 100.0);
        if (Charge.payFromInventory(player, wand, rf)) {
            WandCharge.fill(wand, want, ceiling);
        }
    }
}
