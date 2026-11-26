package dev.fieldframe.arcana.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.fieldframe.arcana.FieldframeArcana;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Numbers of everything that is not a Fieldframe module (modules use Fieldframe tunables). Read from
 * {@code data/<ns>/fieldframe_arcana/tuning/*.json}: {@code {"galvanic_rod": {"rf_per_vis": 300}}}. Files merge in load
 * order; an unknown key or a value out of range is skipped with a log line, the rest of the file still applies.
 */
public final class ArcanaTuning {
    public record Spec(double def, double min, double max) {
    }

    public static final Map<String, Spec> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put("galvanic_rod.interval_ticks", new Spec(20, 1, 1200));
        DEFAULTS.put("galvanic_rod.centivis_per_pulse", new Spec(50, 1, 10_000));
        DEFAULTS.put("galvanic_rod.rf_per_vis", new Spec(250, 0, 1_000_000));
        DEFAULTS.put("galvanic_rod.max_fill", new Spec(1.0, 0, 1));
        DEFAULTS.put("galvanic_rod.staff_multiplier", new Spec(2.5, 0, 100));
        powered("field_lens", 100_000, 1_000);
        DEFAULTS.put("field_lens.rf_per_second", new Spec(20, 0, 100_000));
        powered("field_drill", 400_000, 2_000);
        DEFAULTS.put("field_drill.rf_per_block", new Spec(100, 0, 100_000));
        DEFAULTS.put("field_drill.refine_rf", new Spec(200, 0, 100_000));
        DEFAULTS.put("field_drill.refine_vis", new Spec(0.5, 0, 100));
        powered("field_saw", 400_000, 2_000);
        DEFAULTS.put("field_saw.rf_per_block", new Spec(80, 0, 100_000));
        DEFAULTS.put("field_saw.max_logs", new Spec(128, 1, 1_024));
        DEFAULTS.put("field_saw.vis_per_tree", new Spec(1, 0, 100));
        DEFAULTS.put("field_tools.empty_speed", new Spec(0.5, 0, 100));
        powered("galvanic_boots", 200_000, 1_000);
        DEFAULTS.put("galvanic_boots.rf_per_metre", new Spec(10, 0, 100_000));
        DEFAULTS.put("galvanic_boots.step_height", new Spec(0.5, 0, 3));
        DEFAULTS.put("galvanic_boots.speed_bonus", new Spec(0.25, 0, 3));
        powered("galvanic_wings", 400_000, 2_000);
        DEFAULTS.put("galvanic_wings.rf_per_tick", new Spec(40, 0, 100_000));
        DEFAULTS.put("galvanic_wings.thrust", new Spec(0.04, 0, 1));
        DEFAULTS.put("galvanic_wings.max_speed", new Spec(1.2, 0, 10));
        DEFAULTS.put("arc_part.vis_share", new Spec(0.5, 0, 1));
        DEFAULTS.put("voltaic_core.rf_share", new Spec(0.5, 0, 1));
        DEFAULTS.put("voltaic_core.rf_per_vis", new Spec(250, 0, 1_000_000));
        DEFAULTS.put("voltaic_core.buffer", new Spec(50_000, 0, 100_000_000));
        DEFAULTS.put("voltaic_core.staff_multiplier", new Spec(2.5, 0, 100));
        DEFAULTS.put("voltaic_core.interval_ticks", new Spec(20, 1, 1200));
        DEFAULTS.put("voltaic_core.rf_per_pulse", new Spec(2_000, 0, 10_000_000));
        powered("galvanic_sword", 400_000, 2_000);
        DEFAULTS.put("galvanic_sword.rf_per_hit", new Spec(150, 0, 100_000));
        DEFAULTS.put("galvanic_sword.bonus_damage", new Spec(8, 0, 1_000));
        DEFAULTS.put("galvanic_sword.rf_per_tick", new Spec(20, 0, 100_000));
        DEFAULTS.put("galvanic_sword.arc_rf", new Spec(300, 0, 100_000));
        DEFAULTS.put("galvanic_sword.arc_range", new Spec(4, 0, 32));
        DEFAULTS.put("galvanic_sword.arc_damage", new Spec(4, 0, 1_000));
        powered("galvanic_bow", 400_000, 2_000);
        DEFAULTS.put("galvanic_bow.rf_per_shot", new Spec(400, 0, 100_000));
        DEFAULTS.put("galvanic_bow.draw_ticks", new Spec(10, 1, 100));
        DEFAULTS.put("galvanic_bow.bonus_damage", new Spec(2, 0, 1_000));
        DEFAULTS.put("galvanic_bow.arc_range", new Spec(4, 0, 32));
        DEFAULTS.put("galvanic_bow.arc_damage", new Spec(4, 0, 1_000));
        DEFAULTS.put("galvanic_bow.quick_cooldown_ticks", new Spec(10, 0, 200));
        DEFAULTS.put("galvanic_bow.quick_rf_multiplier", new Spec(1.5, 0, 100));
        powered("galvanic_jetpack", 600_000, 3_000);
        DEFAULTS.put("galvanic_jetpack.rf_per_tick", new Spec(50, 0, 100_000));
        DEFAULTS.put("galvanic_jetpack.hover_rf_per_tick", new Spec(15, 0, 100_000));
        DEFAULTS.put("galvanic_jetpack.thrust", new Spec(0.16, 0, 2));
        DEFAULTS.put("galvanic_jetpack.max_rise", new Spec(0.45, 0, 4));
        DEFAULTS.put("galvanic_jetpack.hover_fall", new Spec(-0.12, -4, 0));
        DEFAULTS.put("galvanic_jetpack.air_control", new Spec(0.02, 0, 1));
        powered("armored_galvanic_jetpack", 800_000, 3_000);
        DEFAULTS.put("arc_part.rf_per_vis", new Spec(300, 0, 1_000_000));
    }

    private static void powered(String group, double capacity, double io) {
        DEFAULTS.put(group + ".capacity", new Spec(capacity, 1, 100_000_000));
        DEFAULTS.put(group + ".max_io", new Spec(io, 1, 10_000_000));
    }

    private static volatile Map<String, Double> values = Map.of();

    private ArcanaTuning() {
    }

    public static double get(String key) {
        Double v = values.get(key);
        return v != null ? v : DEFAULTS.get(key).def();
    }

    public static final class Listener extends SimpleJsonResourceReloadListener<JsonElement> {
        public Listener() {
            super(ExtraCodecs.JSON, FileToIdConverter.json("fieldframe_arcana/tuning"));
        }

        @Override
        protected void apply(Map<Identifier, JsonElement> files, ResourceManager rm, ProfilerFiller profiler) {
            load(files);
        }
    }

    /** Current overrides, for restoring after {@link #load} in tests. */
    public static Map<String, Double> snapshot() {
        return values;
    }

    public static void restore(Map<String, Double> saved) {
        values = Map.copyOf(saved);
    }

    /** Replaces all overrides with what {@code files} hold, the same way a datapack reload does. */
    public static void load(Map<Identifier, JsonElement> files) {
        {
            Map<String, Double> next = new LinkedHashMap<>();
            new java.util.TreeMap<>(files).forEach((file, json) -> {
                if (!json.isJsonObject()) {
                    FieldframeArcana.LOG.error("Arcana tuning {}: not an object (skipped)", file);
                    return;
                }
                for (Map.Entry<String, JsonElement> group : json.getAsJsonObject().entrySet()) {
                    if (!group.getValue().isJsonObject()) {
                        FieldframeArcana.LOG.error("Arcana tuning {}: '{}' is not an object (skipped)", file, group.getKey());
                        continue;
                    }
                    JsonObject g = group.getValue().getAsJsonObject();
                    for (Map.Entry<String, JsonElement> e : g.entrySet()) {
                        String key = group.getKey() + "." + e.getKey();
                        Spec spec = DEFAULTS.get(key);
                        if (spec == null || !e.getValue().isJsonPrimitive() || !e.getValue().getAsJsonPrimitive().isNumber()) {
                            FieldframeArcana.LOG.error("Arcana tuning {}: unknown or non-numeric '{}' (skipped)", file, key);
                            continue;
                        }
                        double v = e.getValue().getAsDouble();
                        if (v < spec.min() || v > spec.max()) {
                            FieldframeArcana.LOG.error("Arcana tuning {}: '{}' = {} outside {}..{} (skipped)", file, key, v, spec.min(), spec.max());
                            continue;
                        }
                        next.put(key, v);
                    }
                }
            });
            values = Map.copyOf(next);
            FieldframeArcana.LOG.info("Arcana tuning: {} files, {} values", files.size(), next.size());
        }
    }
}
