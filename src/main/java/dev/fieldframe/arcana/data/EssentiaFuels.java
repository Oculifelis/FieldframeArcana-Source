package dev.fieldframe.arcana.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.fieldframe.arcana.FieldframeArcana;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * What one point of each aspect is worth in the Essentia Dynamo. One file per aspect (or several aspects per pack):
 * {@code data/<ns>/fieldframe_arcana/essentia_fuels/*.json} = {@code {"aspect": "thaumaturge:ignis", "rf": 900,
 * "flux": 0.05}}, or {@code {"aspect": ..., "remove": true}}. Later files win. Aspects without an entry are not burned.
 */
public final class EssentiaFuels {
    /** {@code rf} for one point; {@code flux} added to the chunk's aura per point burned. */
    public record Fuel(long rf, float flux) {
    }

    private static volatile Map<String, Fuel> fuels = Map.of();

    private EssentiaFuels() {
    }

    public static Fuel of(String aspectId) {
        return fuels.get(aspectId);
    }

    /** Every fuel by aspect id (for JEI). */
    public static Map<String, Fuel> all() {
        return fuels;
    }

    /** Client side: the server's table, from {@link ArcanaDataSync}. */
    static void set(Map<String, Fuel> next) {
        fuels = Map.copyOf(next);
    }

    public static final class Listener extends SimpleJsonResourceReloadListener<JsonElement> {
        public Listener() {
            super(ExtraCodecs.JSON, FileToIdConverter.json("fieldframe_arcana/essentia_fuels"));
        }

        @Override
        protected void apply(Map<Identifier, JsonElement> files, ResourceManager rm, ProfilerFiller profiler) {
            Map<String, Fuel> next = new HashMap<>();
            new java.util.TreeMap<>(files).forEach((file, json) -> {
                try {
                    JsonObject o = json.getAsJsonObject();
                    String aspect = Identifier.parse(o.get("aspect").getAsString()).toString();
                    if (o.has("remove") && o.get("remove").getAsBoolean()) {
                        next.remove(aspect);
                        return;
                    }
                    long rf = o.get("rf").getAsLong();
                    float flux = o.has("flux") ? o.get("flux").getAsFloat() : 0f;
                    if (rf < 0 || rf > 10_000_000 || flux < 0 || flux > 100) {
                        throw new IllegalArgumentException("rf 0..10000000, flux 0..100");
                    }
                    next.put(aspect, new Fuel(rf, flux));
                } catch (RuntimeException ex) {
                    FieldframeArcana.LOG.error("Essentia fuel {}: {} (skipped)", file, ex.toString());
                }
            });
            fuels = Map.copyOf(next);
            FieldframeArcana.LOG.info("Essentia fuels: {} aspects", next.size());
        }
    }
}
