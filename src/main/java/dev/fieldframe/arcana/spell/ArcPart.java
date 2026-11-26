package dev.fieldframe.arcana.spell;

import com.leclowndu93150.thaumaturge.api.spell.Spell;
import com.leclowndu93150.thaumaturge.api.spell.behavior.AbstractModifierBehavior;
import com.leclowndu93150.thaumaturge.api.spell.behavior.SpellBehaviorType;
import com.leclowndu93150.thaumaturge.api.spell.part.SpellPartKind;
import com.leclowndu93150.thaumaturge.api.spell.event.SpellCastEvent;
import com.leclowndu93150.thaumaturge.api.spell.part.SpellPart;
import com.mojang.serialization.MapCodec;
import dev.fieldframe.arcana.FieldframeArcana;
import dev.fieldframe.arcana.data.ArcanaTuning;
import dev.fieldframe.arcana.item.Charge;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Arc spell part: a modifier that fits in any focus. Its behaviour does nothing to the spell itself; a spell that
 * holds it pays {@code arc_part.vis_share} of its vis in RF, {@code arc_part.rf_per_vis} per vis, from an energy item
 * the caster carries. With no charged cell the spell costs its full vis. The part itself is the data file
 * {@code thaumaturge/spell_part/arc.json}.
 */
public final class ArcPart extends AbstractModifierBehavior {
    public static final ArcPart INSTANCE = new ArcPart();
    public static final MapCodec<ArcPart> CODEC = MapCodec.unit(INSTANCE);

    public static final DeferredRegister<SpellBehaviorType<?>> BEHAVIORS = DeferredRegister.create(SpellBehaviorType.REGISTRY_KEY, FieldframeArcana.MODID);
    public static final DeferredHolder<SpellBehaviorType<?>, SpellBehaviorType<ArcPart>> TYPE = BEHAVIORS.register("arc",
            () -> new SpellBehaviorType<>(SpellPartKind.MODIFIER, CODEC));
    public static final ResourceKey<SpellPart> KEY = ResourceKey.create(SpellPart.REGISTRY_KEY, FieldframeArcana.id("arc"));

    private ArcPart() {
    }

    @Override
    public SpellBehaviorType<?> type() {
        return TYPE.get();
    }

    public static void register(IEventBus bus) {
        BEHAVIORS.register(bus);
        NeoForge.EVENT_BUS.addListener(ArcPart::onCast);
    }

    /** True when any node of the spell is the Arc part. */
    public static boolean has(Spell spell) {
        boolean[] found = {false};
        spell.root().forEach(node -> found[0] |= KEY.equals(node.part()));
        return found[0];
    }

    private static void onCast(SpellCastEvent.Pre e) {
        if (!(e.caster() instanceof Player player) || player.level().isClientSide() || !has(e.spell())) {
            return;
        }
        float saved = e.visCost() * (float) ArcanaTuning.get("arc_part.vis_share");
        int rf = (int) Math.ceil(saved * ArcanaTuning.get("arc_part.rf_per_vis"));
        if (saved > 0 && Charge.payFromInventory(player, e.focus(), rf)) {
            e.setVisCost(e.visCost() - saved);
        }
    }
}
