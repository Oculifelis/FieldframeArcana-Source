package dev.fieldframe.arcana.item;

import com.mojang.serialization.Codec;
import dev.fieldframe.arcana.FieldframeArcana;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Item state: stored RF, the current mode of a moded tool, and whether a switchable item (the Galvanic sword) is on. All
 * are synced so bars, tooltips and the item model's {@code has_component} check are right.
 */
public final class ArcanaComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE,
            FieldframeArcana.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY = COMPONENTS.registerComponentType("energy",
            b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MODE = COMPONENTS.registerComponentType("mode",
            b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> ACTIVE = COMPONENTS.registerComponentType("active",
            b -> b.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));

    private ArcanaComponents() {
    }

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }
}
