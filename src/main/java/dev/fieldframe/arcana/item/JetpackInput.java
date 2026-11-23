package dev.fieldframe.arcana.item;

import dev.fieldframe.arcana.FieldframeArcana;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * The movement keys a client holds, sent to the server when they change. On this game version the server has no copy
 * of the player's input, and the jetpack needs jump, sneak and the four movement keys.
 */
public record JetpackInput(byte keys) implements CustomPacketPayload {
    public static final Type<JetpackInput> TYPE = new Type<>(FieldframeArcana.id("jetpack_input"));
    public static final StreamCodec<FriendlyByteBuf, JetpackInput> CODEC = ByteBufCodecs.BYTE.map(JetpackInput::new, JetpackInput::keys)
            .cast();
    public static final int JUMP = 1, SHIFT = 2, FORWARD = 4, BACK = 8, LEFT = 16, RIGHT = 32;
    private static final Map<UUID, Integer> HELD = new ConcurrentHashMap<>();

    @Override
    public Type<JetpackInput> type() {
        return TYPE;
    }

    public static boolean held(Player player, int key) {
        return (HELD.getOrDefault(player.getUUID(), 0) & key) != 0;
    }

    /** Sets the held keys directly, for the server self-test's fake player. */
    public static void set(Player player, int keys) {
        HELD.put(player.getUUID(), keys);
    }

    public static void register(IEventBus bus) {
        bus.addListener((RegisterPayloadHandlersEvent e) -> e.registrar("1").optional().playToServer(TYPE, CODEC,
                (payload, ctx) -> HELD.put(ctx.player().getUUID(), payload.keys() & 0xFF)));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> HELD.remove(e.getEntity().getUUID()));
    }
}
