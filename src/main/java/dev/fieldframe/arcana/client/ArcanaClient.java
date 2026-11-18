package dev.fieldframe.arcana.client;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only setup: the worn jetpack layer on every player renderer. */
public final class ArcanaClient {
    private ArcanaClient() {
    }

    public static void init(IEventBus bus) {
        bus.addListener((EntityRenderersEvent.RegisterLayerDefinitions e) -> e.registerLayerDefinition(JetpackLayer.LAYER, JetpackLayer::create));
        bus.addListener((EntityRenderersEvent.AddLayers e) -> {
            for (PlayerModelType skin : e.getSkins()) {
                AvatarRenderer<AbstractClientPlayer> r = e.getPlayerRenderer(skin);
                if (r != null) {
                    r.addLayer(new JetpackLayer(r, e.getEntityModels()));
                }
            }
        });
    }
}
