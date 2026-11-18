package dev.fieldframe.arcana.item;

import com.leclowndu93150.thaumaturge.api.spell.event.SpellCastEvent;
import com.leclowndu93150.thaumaturge.content.spell.item.FocusItem;
import dev.fieldframe.arcana.data.ArcanaTuning;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The Arc Focus: an ordinary Thaumaturge focus (its size limits come from the {@code thaumaturge:spell_focus} data map)
 * whose spells pay {@code arc_focus.vis_share} of their vis in RF, {@code arc_focus.rf_per_vis} per vis, from an
 * energy item the caster carries. With no charged cell the spell costs its full vis.
 */
public final class ArcFocus {
    private ArcFocus() {
    }

    public static class Item extends FocusItem {
        public Item(Properties props) {
            super(props);
        }
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ArcFocus::onCast);
    }

    private static void onCast(SpellCastEvent.Pre e) {
        if (!(e.focus().getItem() instanceof Item) || !(e.caster() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        float share = (float) ArcanaTuning.get("arc_focus.vis_share");
        float saved = e.visCost() * share;
        int rf = (int) Math.ceil(saved * ArcanaTuning.get("arc_focus.rf_per_vis"));
        if (saved > 0 && Charge.payFromInventory(player, e.focus(), rf)) {
            e.setVisCost(e.visCost() - saved);
        }
    }
}
