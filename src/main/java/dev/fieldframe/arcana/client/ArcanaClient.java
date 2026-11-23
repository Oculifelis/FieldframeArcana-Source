package dev.fieldframe.arcana.client;

import dev.fieldframe.arcana.ARegistry;
import dev.fieldframe.arcana.FieldframeArcana;
import dev.fieldframe.arcana.item.GalvanicSwordItem;
import dev.fieldframe.arcana.item.JetpackInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/** Client-only setup: the worn jetpack and wings layers, the bow and sword model predicates, and jetpack key sync. */
public final class ArcanaClient {
    private static int lastKeys = -1;

    private ArcanaClient() {
    }

    public static void init(IEventBus bus) {
        bus.addListener((EntityRenderersEvent.RegisterLayerDefinitions e) -> e.registerLayerDefinition(JetpackLayer.LAYER, JetpackLayer::create));
        bus.addListener((EntityRenderersEvent.AddLayers e) -> {
            for (PlayerSkin.Model skin : e.getSkins()) {
                if (e.getSkin(skin) instanceof PlayerRenderer r) {
                    r.addLayer(new JetpackLayer(r, e.getEntityModels()));
                    r.addLayer(new WingsLayer(r, e.getEntityModels()));
                }
            }
        });
        bus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(ArcanaClient::itemProperties));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> sendKeys());
    }

    private static void itemProperties() {
        ResourceLocation pull = ResourceLocation.withDefaultNamespace("pull");
        ResourceLocation pulling = ResourceLocation.withDefaultNamespace("pulling");
        ItemProperties.register(ARegistry.GALVANIC_BOW.get(), pull, (stack, level, entity, seed) -> entity == null
                || entity.getUseItem() != stack ? 0f : (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20f);
        ItemProperties.register(ARegistry.GALVANIC_BOW.get(), pulling, (stack, level, entity, seed) -> entity != null
                && entity.isUsingItem() && entity.getUseItem() == stack ? 1f : 0f);
        ItemProperties.register(ARegistry.GALVANIC_SWORD.get(), FieldframeArcana.id("active"),
                (stack, level, entity, seed) -> GalvanicSwordItem.active(stack) ? 1f : 0f);
    }

    /** Sends the held movement keys when they change, for the jetpack on the server. */
    private static void sendKeys() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            lastKeys = -1;
            return;
        }
        Input in = mc.player.input;
        int keys = (in.jumping ? JetpackInput.JUMP : 0) | (in.shiftKeyDown ? JetpackInput.SHIFT : 0) | (in.up ? JetpackInput.FORWARD : 0)
                | (in.down ? JetpackInput.BACK : 0) | (in.left ? JetpackInput.LEFT : 0) | (in.right ? JetpackInput.RIGHT : 0);
        if (keys != lastKeys) {
            lastKeys = keys;
            PacketDistributor.sendToServer(new JetpackInput((byte) keys));
        }
    }
}
