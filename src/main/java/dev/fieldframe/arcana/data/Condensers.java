package dev.fieldframe.arcana.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.fieldframe.arcana.FieldframeArcana;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.Nullable;

/**
 * What the Primal Condenser can make, and when. {@code data/<ns>/fieldframe_arcana/condensers/*.json}:
 * {@code {"aspect": "thaumaturge:aqua", "when": "rain", "rf": 6000, "ticks": 200}} makes one Thaumaturge vis crystal of
 * that aspect; an optional {@code "result": "<item id>"} makes that item instead.
 * {@code when}: {@code always}, {@code rain}, {@code thunder}, {@code clear}, {@code day_sky}, {@code night_sky},
 * {@code below_y:<n>}, {@code above_y:<n>}, {@code dimension:<id>}. Files are tried in id order; the first that
 * matches the module's chosen aspect and holds now is made. {@code "remove": true} drops a file of the same id.
 */
public final class Condensers {
    public record Entry(Identifier id, String aspect, @Nullable String result, String when, long rf, int ticks) {
    }

    private static volatile List<Entry> entries = List.of();

    private Condensers() {
    }

    public static List<Entry> all() {
        return entries;
    }

    /** Client side: the server's list, from {@link ArcanaDataSync}. */
    static void set(List<Entry> next) {
        entries = List.copyOf(next);
    }

    public static final class Listener extends SimpleJsonResourceReloadListener<JsonElement> {
        public Listener() {
            super(ExtraCodecs.JSON, FileToIdConverter.json("fieldframe_arcana/condensers"));
        }

        @Override
        protected void apply(Map<Identifier, JsonElement> files, ResourceManager rm, ProfilerFiller profiler) {
            List<Entry> next = new ArrayList<>();
            new TreeMap<>(files).forEach((file, json) -> {
                try {
                    JsonObject o = json.getAsJsonObject();
                    if (o.has("remove") && o.get("remove").getAsBoolean()) {
                        return;
                    }
                    String when = o.has("when") ? o.get("when").getAsString() : "always";
                    long rf = o.get("rf").getAsLong();
                    int ticks = o.get("ticks").getAsInt();
                    if (rf < 0 || ticks < 1 || ticks > 72_000) {
                        throw new IllegalArgumentException("rf >= 0, ticks 1..72000");
                    }
                    next.add(new Entry(file, Identifier.parse(o.get("aspect").getAsString()).toString(),
                            o.has("result") ? Identifier.parse(o.get("result").getAsString()).toString() : null, when, rf, ticks));
                } catch (RuntimeException ex) {
                    FieldframeArcana.LOG.error("Condenser {}: {} (skipped)", file, ex.toString());
                }
            });
            entries = List.copyOf(next);
            FieldframeArcana.LOG.info("Primal condensers: {} entries", next.size());
        }
    }
}
