package dev.fieldframe.arcana.module;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.api.aura.VisRelayHelper;
import dev.fieldframe.api.module.ModuleBehavior;
import dev.fieldframe.api.module.ModuleContext;
import dev.fieldframe.api.status.Status;
import dev.fieldframe.api.status.Statuses;
import dev.fieldframe.api.world.WorldPos;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;

/**
 * Vis Cradle (registry id {@code node_cradle}, formerly the Node Cradle). It draws {@code centivis_per_tick} a tick into
 * a pool shared by the chassis. The vis comes from a Thaumaturge vis relay network reaching the module's front first,
 * one primal after another, and otherwise from the chunk's aura. A Vis Charger on the same chassis spends the pool
 * before it spends RF. The pool holds at most two seconds of flow; what nobody uses is lost.
 *
 * <p>The TC4 original fed the pool from a jarred node. Under Thaumaturge a node drain for vis becomes a relay or aura
 * draw, so the jarred node no longer does anything. The {@code vessel} storage stays so jars put in it in older worlds
 * can still be taken back out.
 */
final class NodeCradleModule implements ModuleBehavior {
    static final Status THIN_AURA = Status.of("ARCANA_THIN_AURA", Status.Severity.IDLE);

    /** Centivis available per chassis position. Server thread only. */
    private static final Map<WorldPos, Double> POOLS = new ConcurrentHashMap<>();

    @Override
    public void tick(ModuleContext ctx) {
        WorldPos pos = ctx.world().position();
        ArcanaModules.Site site = ArcanaModules.site(ctx);
        if (site == null || pos == null) {
            ctx.setStatus(Statuses.NO_WORLD_ACCESS);
            return;
        }
        double rate = ctx.tunable("centivis_per_tick") * ctx.chassisSpeed();
        double have = POOLS.getOrDefault(pos, 0.0);
        int want = (int) Math.min(Math.ceil(rate), Math.max(0, rate * 40 - have));
        if (want <= 0) {
            ctx.setStatus(Statuses.BUFFER_FULL);
            return;
        }
        List<ResourceKey<IAspect>> primals = TTAspects.PRIMALS;
        ResourceKey<IAspect> primal = primals.get((int) (ctx.gameTime() % primals.size()));
        int got = VisRelayHelper.drainCentivis(site.level(), site.front(), primal, want, false);
        if (got < want) {
            got += Math.round(AuraHelper.drainVis(site.level(), site.front(), (want - got) / 100f, false) * 100);
        }
        if (got <= 0) {
            ctx.setStatus(THIN_AURA);
            ctx.sleep(40);
            return;
        }
        double cap = rate * 40;
        POOLS.merge(pos, (double) got, (a, b) -> Math.min(a + b, cap));
        ctx.setStatus(Statuses.working(got + " cv/t"));
    }

    /** Takes up to {@code centivis} from the pool of the chassis at {@code pos}; returns what was taken. */
    static int take(WorldPos pos, int centivis) {
        if (pos == null || centivis <= 0) {
            return 0;
        }
        int[] got = new int[1];
        POOLS.computeIfPresent(pos, (k, have) -> {
            got[0] = (int) Math.min(centivis, Math.floor(have));
            double left = have - got[0];
            return left <= 0 ? null : left;
        });
        return got[0];
    }

    static void clear() {
        POOLS.clear();
    }
}
