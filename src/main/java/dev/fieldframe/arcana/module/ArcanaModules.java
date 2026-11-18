package dev.fieldframe.arcana.module;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaCrystalAccess;
import dev.fieldframe.api.FieldframeRegistry;
import dev.fieldframe.api.data.FfData;
import dev.fieldframe.api.id.FfId;
import dev.fieldframe.api.module.Archetypes;
import dev.fieldframe.api.module.BindingSpec;
import dev.fieldframe.api.module.ConfigField;
import dev.fieldframe.api.module.ModuleCategory;
import dev.fieldframe.api.module.ModuleDefinition;
import dev.fieldframe.api.module.TickPhase;
import dev.fieldframe.api.module.TunableSpec;
import dev.fieldframe.api.resource.ItemVariant;
import dev.fieldframe.arcana.FieldframeArcana;
import dev.fieldframe.neoforge.api.FieldframeForgeApi;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jspecify.annotations.Nullable;

/** The chassis modules. Their numbers are Fieldframe tunables, so Fieldframe's tuning data and scripts change them. */
public final class ArcanaModules {
    private static final String NS = FieldframeArcana.MODID;
    public static final FfId ESSENTIA_DYNAMO = FfId.of(NS, "essentia_dynamo");
    public static final FfId VIS_CHARGER = FfId.of(NS, "vis_charger");
    public static final FfId AURA_TAP = FfId.of(NS, "aura_tap");
    public static final FfId PRIMAL_CONDENSER = FfId.of(NS, "primal_condenser");
    public static final FfId ETHERIC_CRUSHER = FfId.of(NS, "etheric_crusher");
    public static final FfId ETHEREALIZE = FfId.of(NS, "etherealize");
    public static final FfId FLUX_SCRUBBER = FfId.of(NS, "flux_scrubber");
    public static final FfId NODE_CRADLE = FfId.of(NS, "node_cradle");
    public static final List<FfId> ALL = List.of(ESSENTIA_DYNAMO, VIS_CHARGER, AURA_TAP, PRIMAL_CONDENSER, ETHERIC_CRUSHER,
            FLUX_SCRUBBER, NODE_CRADLE);

    private ArcanaModules() {
    }

    public static void register() {
        FieldframeRegistry.registerModule(ModuleDefinition.builder(ESSENTIA_DYNAMO)
                .category(ModuleCategory.GENERATION)
                .phase(TickPhase.GENERATE)
                .stage(ModuleDefinition.Stage.AUTOMATION)
                .binding(BindingSpec.signalIn("enable"))
                .binding(BindingSpec.signalOut("active"))
                .tunable(TunableSpec.of("ticks_per_point", 1, 1_200, 40, "ticks"))
                .tunable(TunableSpec.of("rf_multiplier", 0, 10, 1, "x"))
                .tunable(TunableSpec.of("flux_multiplier", 0, 10, 1, "x"))
                .behavior(def -> new EssentiaDynamoModule())
                .presentation(face("essentia_dynamo"))
                .build(), NS);
        FieldframeRegistry.registerModule(ModuleDefinition.builder(VIS_CHARGER)
                .category(ModuleCategory.PROCESSING)
                .phase(TickPhase.PROCESS)
                .stage(ModuleDefinition.Stage.AUTOMATION)
                .binding(BindingSpec.item("input", 0))
                .binding(BindingSpec.item("output", 1))
                .binding(BindingSpec.signalIn("enable"))
                .binding(BindingSpec.signalOut("active"))
                .tunable(TunableSpec.of("rf_per_vis", 0, 100_000, 200, "RF"))
                .tunable(TunableSpec.of("centivis_per_tick", 1, 10_000, 25, "cv/t"))
                .behavior(def -> new VisChargerModule())
                .presentation(face("vis_charger"))
                .build(), NS);
        FieldframeRegistry.registerModule(ModuleDefinition.builder(AURA_TAP)
                .category(ModuleCategory.GENERATION)
                .phase(TickPhase.GENERATE)
                .stage(ModuleDefinition.Stage.FIELD)
                .binding(BindingSpec.signalIn("enable"))
                .binding(BindingSpec.signalOut("active"))
                .tunable(TunableSpec.of("vis_per_second", 0.1, 100, 2, "vis/s"))
                .tunable(TunableSpec.of("rf_per_vis", 0, 1_000_000, 1_500, "RF"))
                .tunable(TunableSpec.of("floor_fraction", 0, 1, 0.5, "x"))
                .behavior(def -> new AuraTapModule())
                .presentation(face("aura_tap"))
                .build(), NS);
        FieldframeRegistry.registerModule(ModuleDefinition.builder(PRIMAL_CONDENSER)
                .category(ModuleCategory.PROCESSING)
                .phase(TickPhase.PROCESS)
                .stage(ModuleDefinition.Stage.FIELD)
                .binding(BindingSpec.item("output", 1))
                .binding(BindingSpec.signalIn("enable"))
                .binding(BindingSpec.signalOut("active"))
                .config(ConfigField.choice("aspect", "any", "any", "aer", "terra", "ignis", "aqua", "ordo", "perditio"))
                .behavior(def -> new PrimalCondenserModule())
                .presentation(face("primal_condenser"))
                .build(), NS);
        FieldframeRegistry.registerModule(Archetypes.processor(ETHERIC_CRUSHER, ETHEREALIZE, ModuleDefinition.Stage.AUTOMATION, false, false)
                .presentation(face("etheric_crusher"))
                .build(), NS);
        FieldframeRegistry.registerModule(ModuleDefinition.builder(FLUX_SCRUBBER)
                .category(ModuleCategory.PROCESSING)
                .phase(TickPhase.PROCESS)
                .stage(ModuleDefinition.Stage.FIELD)
                .binding(BindingSpec.item("output", 1))
                .binding(BindingSpec.signalIn("enable"))
                .binding(BindingSpec.signalOut("active"))
                .tunable(TunableSpec.of("flux_per_second", 0.1, 100, 1, "flux/s"))
                .tunable(TunableSpec.of("rf_per_flux", 0, 1_000_000, 2_000, "RF"))
                .tunable(TunableSpec.of("flux_per_crystal", 0.5, 1_000, 10, "flux"))
                .behavior(def -> new FluxScrubberModule())
                .presentation(face("flux_scrubber"))
                .build(), NS);
        FieldframeRegistry.registerModule(ModuleDefinition.builder(NODE_CRADLE)
                .category(ModuleCategory.GENERATION)
                .phase(TickPhase.GENERATE)
                .stage(ModuleDefinition.Stage.FIELD)
                .binding(BindingSpec.item("vessel", 2))
                .tunable(TunableSpec.of("centivis_per_tick", 0, 1_000, 5, "cv/t"))
                .behavior(def -> new NodeCradleModule())
                .presentation(face("node_cradle"))
                .build(), NS);
    }

    public static void serverStopped() {
        NodeCradleModule.clear();
    }

    private static FfData face(String name) {
        return new FfData().putString("face_texture", NS + ":block/module/" + name);
    }

    /** Where a module sits in the world: its chassis' level and the block in front of its face. */
    record Site(ServerLevel level, BlockPos front, Direction toChassis) {
    }

    static Site site(dev.fieldframe.api.module.ModuleContext ctx) {
        var pos = ctx.world().position();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (pos == null || server == null) {
            return null;
        }
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(pos.dimension())));
        if (level == null) {
            return null;
        }
        Direction out = Direction.values()[ctx.worldDirection().ordinal()];
        BlockPos front = new BlockPos(pos.x(), pos.y(), pos.z()).relative(out);
        return new Site(level, front, out.getOpposite());
    }

    /** One Thaumaturge vis crystal of {@code aspectId} (the item with its aspect component), or null without a world. */
    static @Nullable ItemVariant visCrystal(dev.fieldframe.api.module.ModuleContext ctx, String aspectId) {
        Site site = site(ctx);
        if (site == null) {
            return null;
        }
        return site.level().registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY)
                .get(ResourceKey.create(IAspect.REGISTRY_KEY, Identifier.parse(aspectId)))
                .map(aspect -> FieldframeForgeApi.variantOf(EssentiaCrystalAccess.create(aspect, 1)))
                .orElse(null);
    }
}
