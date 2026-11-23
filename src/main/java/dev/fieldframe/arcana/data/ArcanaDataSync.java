package dev.fieldframe.arcana.data;

import dev.fieldframe.arcana.FieldframeArcana;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Sends the essentia fuel and condenser tables to clients on every datapack sync, so JEI can show the Essentia
 * Dynamo and Primal Condenser pages on a dedicated server too. The machines themselves only read the server copy.
 */
public record ArcanaDataSync(Map<String, EssentiaFuels.Fuel> fuels, List<Condensers.Entry> condensers) implements CustomPacketPayload {
    public static final Type<ArcanaDataSync> TYPE = new Type<>(FieldframeArcana.id("data_sync"));

    private static final StreamCodec<RegistryFriendlyByteBuf, EssentiaFuels.Fuel> FUEL = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, EssentiaFuels.Fuel::rf, ByteBufCodecs.FLOAT, EssentiaFuels.Fuel::flux, EssentiaFuels.Fuel::new);
    private static final StreamCodec<RegistryFriendlyByteBuf, Condensers.Entry> ENTRY = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, Condensers.Entry::id,
            ByteBufCodecs.STRING_UTF8, Condensers.Entry::aspect,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), e -> Optional.ofNullable(e.result()),
            ByteBufCodecs.STRING_UTF8, Condensers.Entry::when,
            ByteBufCodecs.VAR_LONG, Condensers.Entry::rf,
            ByteBufCodecs.VAR_INT, Condensers.Entry::ticks,
            (id, aspect, result, when, rf, ticks) -> new Condensers.Entry(id, aspect, result.orElse(null), when, rf, ticks));
    public static final StreamCodec<RegistryFriendlyByteBuf, ArcanaDataSync> CODEC = StreamCodec.composite(
            ByteBufCodecs.map(java.util.HashMap::new, ByteBufCodecs.STRING_UTF8, FUEL), ArcanaDataSync::fuels,
            ENTRY.apply(ByteBufCodecs.list()), ArcanaDataSync::condensers,
            ArcanaDataSync::new);

    @Override
    public Type<ArcanaDataSync> type() {
        return TYPE;
    }

    public static void register(IEventBus bus) {
        bus.addListener((RegisterPayloadHandlersEvent e) -> e.registrar("1").optional().playToClient(TYPE, CODEC, (payload, ctx) -> {
            EssentiaFuels.set(payload.fuels());
            Condensers.set(payload.condensers());
        }));
        NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent e) -> {
            ArcanaDataSync payload = new ArcanaDataSync(EssentiaFuels.all(), Condensers.all());
            e.getRelevantPlayers().forEach(p -> PacketDistributor.sendToPlayer(p, payload));
        });
    }
}
