package dev.fieldframe.arcana.module;

import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import dev.fieldframe.api.data.FfData;
import dev.fieldframe.api.module.ModuleBehavior;
import dev.fieldframe.api.module.ModuleContext;
import dev.fieldframe.api.status.Status;
import dev.fieldframe.api.status.Statuses;
import net.minecraft.core.BlockPos;

/**
 * Draws vis from the chassis' chunk once a second (at most {@code vis_per_second}, never below
 * {@code floor_fraction} of the chunk's base aura) and pays it out as RF over the next second,
 * {@code rf_per_vis} for each vis.
 */
final class AuraTapModule implements ModuleBehavior {
    static final Status THIN_AURA = Status.of("ARCANA_THIN_AURA", Status.Severity.IDLE);

    private long rfLeft;
    private long rfPerTick;
    private float lastVis;

    @Override
    public void tick(ModuleContext ctx) {
        if (rfLeft <= 0) {
            ctx.emit("active", 0);
            if (!ctx.enabledBy("enable")) {
                ctx.setStatus(Statuses.signalDisabled(ctx.bindingLabel("enable")));
                return;
            }
            if (ctx.energy().stored() >= ctx.energy().capacity()) {
                ctx.setStatus(Statuses.BUFFER_FULL);
                return;
            }
            if (ctx.gameTime() % 20 != 0) {
                return;
            }
            ArcanaModules.Site site = ArcanaModules.site(ctx);
            if (site == null) {
                ctx.setStatus(Statuses.NO_WORLD_ACCESS);
                return;
            }
            BlockPos pos = site.front();
            float floor = (float) (AuraHelper.getAuraBase(site.level(), pos) * ctx.tunable("floor_fraction"));
            float spare = AuraHelper.getVis(site.level(), pos) - floor;
            float want = (float) Math.min(spare, ctx.tunable("vis_per_second") * ctx.chassisSpeed());
            if (want <= 0.01f) {
                ctx.setStatus(THIN_AURA);
                return;
            }
            lastVis = AuraHelper.drainVis(site.level(), pos, want, false);
            rfLeft = Math.round(lastVis * ctx.tunable("rf_per_vis"));
            rfPerTick = Math.max(1, (rfLeft + 19) / 20);
        }
        long made = ctx.energy().generate(Math.min(rfLeft, rfPerTick));
        if (made <= 0) {
            ctx.setStatus(Statuses.BUFFER_FULL);
            return;
        }
        rfLeft -= made;
        ctx.emit("active", 15);
        ctx.setStatus(Statuses.working(String.format("%.1f vis/s", lastVis)));
    }

    @Override
    public FfData save() {
        return new FfData().putLong("rf_left", rfLeft).putLong("rf_per_tick", rfPerTick).putDouble("last_vis", lastVis);
    }

    @Override
    public void load(FfData data) {
        rfLeft = data.getLong("rf_left", 0);
        rfPerTick = data.getLong("rf_per_tick", 0);
        lastVis = (float) data.getDouble("last_vis", 0);
    }
}
