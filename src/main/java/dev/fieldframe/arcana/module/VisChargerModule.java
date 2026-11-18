package dev.fieldframe.arcana.module;

import dev.fieldframe.api.module.ModuleBehavior;
import dev.fieldframe.api.module.ModuleContext;
import dev.fieldframe.api.resource.ItemStore;
import dev.fieldframe.api.resource.ItemVariant;
import dev.fieldframe.api.status.Status;
import dev.fieldframe.api.status.Statuses;
import dev.fieldframe.arcana.FieldframeArcana;
import dev.fieldframe.arcana.wand.WandCharge;
import dev.fieldframe.neoforge.api.FieldframeForgeApi;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Fills wands in its {@code input} lane with vis paid for in chassis RF, lowest primal first, and moves each full wand
 * to its {@code output} lane. Works in steps of {@link #STEP} ticks, so a wand is taken out and put back only that often.
 */
final class VisChargerModule implements ModuleBehavior {
    static final TagKey<Item> CHARGEABLE = TagKey.create(Registries.ITEM, FieldframeArcana.id("chargeable_wands"));
    static final Status NO_WAND = Status.of("ARCANA_NO_WAND", Status.Severity.IDLE);
    private static final int STEP = 10;

    private double progress = -1;

    @Override
    public void tick(ModuleContext ctx) {
        ItemStore in = ctx.items("input");
        ItemStore out = ctx.items("output");
        if (in == null || out == null) {
            ctx.setStatus(Statuses.UNBOUND);
            return;
        }
        if (!ctx.enabledBy("enable")) {
            ctx.emit("active", 0);
            ctx.setStatus(Statuses.signalDisabled(ctx.bindingLabel("enable")));
            return;
        }
        if (ctx.gameTime() % STEP != 0) {
            return;
        }
        int slot = findWand(in);
        if (slot < 0) {
            progress = -1;
            ctx.emit("active", 0);
            ctx.setStatus(NO_WAND);
            ctx.sleep(40);
            return;
        }
        ItemVariant v = in.variant(slot);
        ItemStack wand = FieldframeForgeApi.toStack(v, 1);
        if (WandCharge.missing(wand) == 0) {
            moveOut(ctx, in, out, slot, v);
            return;
        }
        double rfPerVis = ctx.tunable("rf_per_vis");
        int speed = (int) Math.min(Integer.MAX_VALUE, Math.round(ctx.tunable("centivis_per_tick") * STEP * ctx.chassisSpeed()));
        // A Vis Cradle on this chassis pays first; RF covers the rest.
        int free = WandCharge.fill(wand, NodeCradleModule.take(ctx.world().position(), Math.min(speed, WandCharge.missing(wand))));
        long budget = speed - free;
        if (rfPerVis > 0) {
            budget = Math.min(budget, (long) Math.floor(ctx.energy().stored() * 100.0 / rfPerVis));
        }
        int added = budget > 0 ? WandCharge.fill(wand, (int) budget) : 0;
        long cost = Math.round(added * rfPerVis / 100.0);
        if (free + added <= 0 || !ctx.energy().consume(cost)) {
            ctx.emit("active", 0);
            ctx.setStatus(Statuses.noEnergy(Math.max(1, cost == 0 ? Math.round(rfPerVis) : cost)));
            return;
        }
        ItemVariant charged = FieldframeForgeApi.variantOf(wand);
        if (in.extractSlot(slot, 1, false) == 1) {
            int back = in.insertSlot(slot, charged, 1, false);
            if (back < 1) {
                ctx.spill(charged, 1);
            }
        }
        progress = WandCharge.fraction(wand);
        ctx.emit("active", 15);
        ctx.setStatus(Statuses.working(Math.round(progress * 100) + "%"));
    }

    private void moveOut(ModuleContext ctx, ItemStore in, ItemStore out, int slot, ItemVariant v) {
        if (out.insert(v, 1, true) < 1) {
            ctx.emit("active", 0);
            ctx.setStatus(Statuses.outputFull(ctx.bindingLabel("output")));
            return;
        }
        if (in.extractSlot(slot, 1, false) == 1) {
            out.insert(v, 1, false);
        }
        progress = -1;
    }

    private static int findWand(ItemStore in) {
        for (int s = 0; s < in.slots(); s++) {
            ItemVariant v = in.variant(s);
            if (v != null && in.count(s) > 0 && FieldframeForgeApi.toStack(v, 1).is(CHARGEABLE)) {
                return s;
            }
        }
        return -1;
    }

    @Override
    public double progress() {
        return progress;
    }
}
