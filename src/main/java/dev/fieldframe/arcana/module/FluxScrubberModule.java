package dev.fieldframe.arcana.module;

import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import dev.fieldframe.api.data.FfData;
import dev.fieldframe.api.module.ModuleBehavior;
import dev.fieldframe.api.module.ModuleContext;
import dev.fieldframe.api.resource.ItemStore;
import dev.fieldframe.api.resource.ItemVariant;
import dev.fieldframe.api.status.Status;
import dev.fieldframe.api.status.Statuses;

/**
 * Once a second pulls up to {@code flux_per_second} flux out of the chassis' chunk for {@code rf_per_flux} RF each,
 * and puts one Vitium vis crystal into its {@code output} storage for every {@code flux_per_crystal} gathered.
 */
final class FluxScrubberModule implements ModuleBehavior {
    static final Status CLEAN = Status.of("ARCANA_AURA_CLEAN", Status.Severity.IDLE);

    private double gathered;

    @Override
    public void tick(ModuleContext ctx) {
        if (ctx.gameTime() % 20 != 0) {
            return;
        }
        ItemStore out = ctx.items("output");
        if (out == null) {
            ctx.setStatus(Statuses.UNBOUND);
            return;
        }
        if (!ctx.enabledBy("enable")) {
            ctx.emit("active", 0);
            ctx.setStatus(Statuses.signalDisabled(ctx.bindingLabel("enable")));
            return;
        }
        ItemVariant crystal = ArcanaModules.visCrystal(ctx, "thaumaturge:vitium");
        double perCrystal = ctx.tunable("flux_per_crystal");
        if (gathered >= perCrystal) {
            if (crystal == null || out.insert(crystal, 1, false) < 1) {
                ctx.emit("active", 0);
                ctx.setStatus(Statuses.outputFull(ctx.bindingLabel("output")));
                return;
            }
            gathered -= perCrystal;
        }
        ArcanaModules.Site site = ArcanaModules.site(ctx);
        if (site == null) {
            ctx.setStatus(Statuses.NO_WORLD_ACCESS);
            return;
        }
        float flux = AuraHelper.getFlux(site.level(), site.front());
        float want = (float) Math.min(flux, ctx.tunable("flux_per_second") * ctx.chassisSpeed());
        double rfPerFlux = ctx.tunable("rf_per_flux");
        if (rfPerFlux > 0) {
            want = (float) Math.min(want, ctx.energy().stored() / rfPerFlux);
        }
        if (flux < 0.01f) {
            ctx.emit("active", 0);
            ctx.setStatus(CLEAN);
            return;
        }
        if (want < 0.01f) {
            ctx.emit("active", 0);
            ctx.setStatus(Statuses.noEnergy(Math.round(rfPerFlux)));
            return;
        }
        float taken = AuraHelper.drainFlux(site.level(), site.front(), want, false);
        ctx.energy().consume(Math.min(ctx.energy().stored(), Math.round(taken * rfPerFlux)));
        gathered += taken;
        ctx.emit("active", 15);
        ctx.setStatus(Statuses.working(String.format("%.1f flux", flux)));
    }

    @Override
    public double progress() {
        return Math.min(1, gathered / 10.0);
    }

    @Override
    public FfData save() {
        return new FfData().putDouble("gathered", gathered);
    }

    @Override
    public void load(FfData data) {
        gathered = data.getDouble("gathered", 0);
    }
}
