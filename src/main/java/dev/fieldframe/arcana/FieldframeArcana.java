package dev.fieldframe.arcana;

import dev.fieldframe.arcana.data.ArcanaDataSync;
import dev.fieldframe.arcana.data.ArcanaTuning;
import dev.fieldframe.arcana.data.Condensers;
import dev.fieldframe.arcana.data.EssentiaFuels;
import dev.fieldframe.arcana.item.ArcFocus;
import dev.fieldframe.arcana.item.ArcanaArmorEvents;
import dev.fieldframe.arcana.item.ArcanaToolEvents;
import dev.fieldframe.arcana.item.GalvanicBowItem;
import dev.fieldframe.arcana.module.ArcanaModules;
import dev.fieldframe.neoforge.api.FieldframeForgeApi;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
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
        ArcFocus.register();
        SelfTest.register();
        ArcanaDataSync.register(bus);
        if (dist == Dist.CLIENT) {
            dev.fieldframe.arcana.client.ArcanaClient.init(bus);
        }
        NeoForge.EVENT_BUS.addListener((AddServerReloadListenersEvent e) -> {
            e.addListener(id("tuning"), new ArcanaTuning.Listener());
            e.addListener(id("essentia_fuels"), new EssentiaFuels.Listener());
            e.addListener(id("condensers"), new Condensers.Listener());
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> ArcanaModules.serverStopped());
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
