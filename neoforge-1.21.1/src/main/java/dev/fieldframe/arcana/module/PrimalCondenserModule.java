package dev.fieldframe.arcana.module;

import dev.fieldframe.api.data.FfData;
import dev.fieldframe.api.id.FfId;
import dev.fieldframe.api.module.ModuleBehavior;
import dev.fieldframe.api.module.ModuleContext;
import dev.fieldframe.api.resource.ItemStore;
import dev.fieldframe.api.resource.ItemVariant;
import dev.fieldframe.api.status.Status;
import dev.fieldframe.api.status.Statuses;
import dev.fieldframe.api.world.WorldPos;
import dev.fieldframe.arcana.data.Condensers;

/**
 * Spends RF while a world condition holds and makes a primal vis crystal into its {@code output} storage. Which one,
 * and under which condition, is {@link Condensers} data; the {@code aspect} setting narrows it to one primal.
 * Progress pauses (it is not lost) while the condition stops holding.
 */
final class PrimalCondenserModule implements ModuleBehavior {
    static final Status NO_CONDITION = Status.of("ARCANA_NO_CONDITION", Status.Severity.IDLE);

    private String entryId = "";
    private long rfPaid;
    private Condensers.Entry entry;

    @Override
    public void tick(ModuleContext ctx) {
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
        if (entry == null || !holds(ctx, entry)) {
            entry = pick(ctx);
            if (entry == null) {
                ctx.emit("active", 0);
                ctx.setStatus(NO_CONDITION);
                ctx.sleep(40);
                return;
            }
            if (!entry.id().toString().equals(entryId)) {
                entryId = entry.id().toString();
                rfPaid = 0;
            }
        }
        ItemVariant result = entry.result() == null ? ArcanaModules.visCrystal(ctx, entry.aspect())
                : ctx.world().itemVariant(FfId.parse(entry.result()));
        if (result == null || out.insert(result, 1, true) < 1) {
            ctx.emit("active", 0);
            ctx.setStatus(Statuses.outputFull(ctx.bindingLabel("output")));
            return;
        }
        long perTick = Math.max(1, Math.round(entry.rf() * ctx.chassisSpeed() / entry.ticks()));
        long step = Math.min(perTick, entry.rf() - rfPaid);
        if (step > 0 && !ctx.energy().consume(step)) {
            ctx.emit("active", 0);
            ctx.setStatus(Statuses.noEnergy(step));
            return;
        }
        rfPaid += step;
        if (rfPaid >= entry.rf()) {
            out.insert(result, 1, false);
            rfPaid = 0;
        }
        ctx.emit("active", 15);
        ctx.setStatus(Statuses.working(entry.aspect()));
    }

    private Condensers.Entry pick(ModuleContext ctx) {
        String want = ctx.config().choice("aspect");
        for (Condensers.Entry e : Condensers.all()) {
            if (("any".equals(want) || e.aspect().endsWith(":" + want)) && holds(ctx, e)) {
                return e;
            }
        }
        return null;
    }

    private static boolean holds(ModuleContext ctx, Condensers.Entry e) {
        var w = ctx.world();
        WorldPos pos = w.position();
        String when = e.when();
        if (when.startsWith("below_y:")) {
            return pos != null && pos.y() < Integer.parseInt(when.substring(8));
        }
        if (when.startsWith("above_y:")) {
            return pos != null && pos.y() > Integer.parseInt(when.substring(8));
        }
        if (when.startsWith("dimension:")) {
            return pos != null && pos.dimension().equals(when.substring(10));
        }
        boolean sky = w.canSeeSky(ctx.face());
        return switch (when) {
            case "always" -> true;
            case "rain" -> sky && w.isRaining();
            case "thunder" -> sky && w.isThundering();
            case "clear" -> sky && !w.isRaining();
            case "day_sky" -> sky && w.isDaytime() && !w.isRaining();
            case "night_sky" -> sky && !w.isDaytime() && !w.isRaining();
            default -> false;
        };
    }

    @Override
    public double progress() {
        return entry == null || entry.rf() <= 0 ? -1 : (double) rfPaid / entry.rf();
    }

    @Override
    public FfData save() {
        return new FfData().putString("entry", entryId).putLong("rf_paid", rfPaid);
    }

    @Override
    public void load(FfData data) {
        entryId = data.getString("entry", "");
        rfPaid = data.getLong("rf_paid", 0);
    }
}
