package dev.fieldframe.arcana.module;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaCapabilities;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaStorage;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaTransport;
import dev.fieldframe.api.data.FfData;
import dev.fieldframe.api.module.ModuleBehavior;
import dev.fieldframe.api.module.ModuleContext;
import dev.fieldframe.api.status.Status;
import dev.fieldframe.api.status.Statuses;
import dev.fieldframe.arcana.data.EssentiaFuels;
import net.minecraft.core.Holder;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Burns essentia from the block in front of its face (a jar, or anything with Thaumaturge's essentia storage or
 * transport capability) one point at a time. A point's RF ({@link EssentiaFuels}) is paid out evenly over
 * {@code ticks_per_point}; the chassis tier speeds it up without changing RF per point. Each point leaks its flux into
 * the chunk's aura.
 */
final class EssentiaDynamoModule implements ModuleBehavior {
    static final Status NO_ESSENTIA = Status.of("ARCANA_NO_ESSENTIA", Status.Severity.IDLE);
    static final Status NO_SOURCE = Status.of("ARCANA_NO_ESSENTIA_SOURCE", Status.Severity.IDLE);

    private String burning = "";
    private long rfLeft;
    private long rfPerTick;

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
            if (!draw(ctx)) {
                return;
            }
        }
        long made = ctx.energy().generate(Math.min(rfLeft, rfPerTick));
        if (made <= 0) {
            ctx.setStatus(Statuses.BUFFER_FULL);
            return;
        }
        rfLeft -= made;
        ctx.emit("active", 15);
        ctx.setStatus(Statuses.working(burning));
    }

    /** Takes one point of the most valuable burnable aspect in front; false (with a status) when there is none. */
    private boolean draw(ModuleContext ctx) {
        ArcanaModules.Site site = ArcanaModules.site(ctx);
        if (site == null || !site.level().isLoaded(site.front())) {
            ctx.setStatus(Statuses.NEIGHBOR_UNLOADED);
            return false;
        }
        Holder<IAspect> taken = null;
        IEssentiaStorage storage = site.level().getCapability(EssentiaCapabilities.STORAGE, site.front(), site.toChassis());
        if (storage != null) {
            Holder<IAspect> best = null;
            long bestRf = 0;
            for (AspectInstance in : storage.contents().entries()) {
                EssentiaFuels.Fuel f = in.amount() > 0 ? EssentiaFuels.of(in.aspect().getRegisteredName()) : null;
                if (f != null && f.rf() > bestRf) {
                    best = in.aspect();
                    bestRf = f.rf();
                }
            }
            if (best != null) {
                try (Transaction tx = Transaction.openRoot()) {
                    if (storage.extract(best, 1, tx) == 1) {
                        tx.commit();
                        taken = best;
                    }
                }
            }
        } else {
            IEssentiaTransport transport = site.level().getCapability(EssentiaCapabilities.TRANSPORT, site.front(), site.toChassis());
            if (transport == null) {
                ctx.setStatus(NO_SOURCE);
                ctx.sleep(40);
                return false;
            }
            Holder<IAspect> type = transport.getEssentiaType(site.toChassis());
            if (type != null && transport.getEssentiaAmount(site.toChassis()) > 0 && EssentiaFuels.of(type.getRegisteredName()) != null
                    && transport.takeEssentia(type, 1, site.toChassis()) == 1) {
                taken = type;
            }
        }
        if (taken == null) {
            ctx.setStatus(NO_ESSENTIA);
            ctx.sleep(20);
            return false;
        }
        EssentiaFuels.Fuel fuel = EssentiaFuels.of(taken.getRegisteredName());
        float flux = (float) (fuel.flux() * ctx.tunable("flux_multiplier"));
        if (flux > 0) {
            AuraHelper.addFlux(site.level(), site.front(), flux);
        }
        burning = taken.getRegisteredName();
        rfLeft = Math.round(fuel.rf() * ctx.tunable("rf_multiplier"));
        double ticks = Math.max(1, ctx.tunable("ticks_per_point") / ctx.chassisSpeed());
        rfPerTick = Math.max(1, (long) Math.ceil(rfLeft / ticks));
        return rfLeft > 0;
    }

    @Override
    public String detail() {
        return rfLeft > 0 ? burning + ", " + rfLeft + " RF left" : "";
    }

    @Override
    public FfData save() {
        return new FfData().putString("burning", burning).putLong("rf_left", rfLeft).putLong("rf_per_tick", rfPerTick);
    }

    @Override
    public void load(FfData data) {
        burning = data.getString("burning", "");
        rfLeft = data.getLong("rf_left", 0);
        rfPerTick = data.getLong("rf_per_tick", 0);
    }
}
