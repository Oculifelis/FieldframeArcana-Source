package dev.fieldframe.arcana;

import dev.fieldframe.arcana.data.ArcanaDataSync;
import dev.fieldframe.arcana.data.ArcanaTuning;
import dev.fieldframe.arcana.data.Condensers;
import dev.fieldframe.arcana.data.EssentiaFuels;
import dev.fieldframe.arcana.spell.ArcPart;
import dev.fieldframe.arcana.item.ArcanaArmorEvents;
import dev.fieldframe.arcana.item.ArcanaToolEvents;
import dev.fieldframe.arcana.item.GalvanicBowItem;
import dev.fieldframe.arcana.module.ArcanaModules;
import dev.fieldframe.neoforge.api.FieldframeForgeApi;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Thaumaturge magic crossed with Fieldframe's chassis machines. See docs/DESIGN.md. */
@Mod(FieldframeArcana.MODID)
public final class FieldframeArcana {
    public static final String MODID = "fieldframe_arcana";
    public static final Logger LOG = LoggerFactory.getLogger("Fieldframe Arcana");

    public FieldframeArcana(IEventBus bus, Dist dist) {
        ARegistry.register(bus);
        FieldframeForgeApi.onRegisterModules(ArcanaModules::register);
        ArcanaToolEvents.register();
        ArcanaArmorEvents.register();
        GalvanicBowItem.register();
        ArcPart.register(bus);
        SelfTest.register();
        ArcanaDataSync.register(bus);
        dev.fieldframe.arcana.item.JetpackInput.register(bus);
        if (dist == Dist.CLIENT) {
            dev.fieldframe.arcana.client.ArcanaClient.init(bus);
        }
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> {
            e.addListener(new ArcanaTuning.Listener());
            e.addListener(new EssentiaFuels.Listener());
            e.addListener(new Condensers.Listener());
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> ArcanaModules.serverStopped());
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
