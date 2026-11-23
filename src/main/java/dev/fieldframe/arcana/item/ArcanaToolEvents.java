package dev.fieldframe.arcana.item;

import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import dev.fieldframe.arcana.data.ArcanaTuning;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** World behaviour of the Field Drill and Field Saw: empty slowdown, the drill's 3x3 and refining, the saw's felling. */
public final class ArcanaToolEvents {
    /** Players currently breaking extra blocks for a tool, so those breaks do not start another round. */
    private static final Set<Player> BUSY = new HashSet<>();

    private ArcanaToolEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ArcanaToolEvents::breakSpeed);
        NeoForge.EVENT_BUS.addListener(ArcanaToolEvents::onBreak);
        NeoForge.EVENT_BUS.addListener(ArcanaToolEvents::onDrops);
    }

    private static void breakSpeed(PlayerEvent.BreakSpeed e) {
        ItemStack tool = e.getEntity().getMainHandItem();
        if (tool.getItem() instanceof FieldToolItem t && !Charge.has(tool, t.rfPerBlock())) {
            e.setNewSpeed(Math.min(e.getNewSpeed(), (float) ArcanaTuning.get("field_tools.empty_speed")));
        }
    }

    private static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer player) || !(e.getLevel() instanceof ServerLevel level) || BUSY.contains(player)) {
            return;
        }
        ItemStack tool = player.getMainHandItem();
        if (tool.getItem() instanceof FieldToolItem.Drill drill && FieldToolItem.mode(tool) == FieldToolItem.Drill.AREA) {
            area(player, level, e.getPos(), tool, drill);
        } else if (tool.getItem() instanceof FieldToolItem.Saw saw && FieldToolItem.mode(tool) == FieldToolItem.Saw.FELLING
                && e.getState().is(BlockTags.LOGS) && !player.isShiftKeyDown()) {
            fell(player, level, e.getPos(), tool, saw);
        }
    }

    /** Breaks the eight blocks around {@code center} in the plane facing the player, each paid like a normal block. */
    private static void area(ServerPlayer player, ServerLevel level, BlockPos center, ItemStack tool, FieldToolItem drill) {
        HitResult hit = player.pick(player.blockInteractionRange(), 0f, false);
        Direction face = hit instanceof BlockHitResult b ? b.getDirection() : Direction.UP;
        float centerHardness = level.getBlockState(center).getDestroySpeed(level, center);
        List<BlockPos> targets = new ArrayList<>();
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) {
                    continue;
                }
                targets.add(switch (face.getAxis()) {
                    case X -> center.offset(0, a, b);
                    case Y -> center.offset(a, 0, b);
                    case Z -> center.offset(a, b, 0);
                });
            }
        }
        BUSY.add(player);
        try {
            for (BlockPos p : targets) {
                BlockState s = level.getBlockState(p);
                float h = s.getDestroySpeed(level, p);
                if (s.isAir() || h < 0 || h > centerHardness + 1.5f || !tool.isCorrectToolForDrops(s)
                        || !Charge.has(tool, drill.rfPerBlock())) {
                    continue;
                }
                player.gameMode.destroyBlock(p);
            }
        } finally {
            BUSY.remove(player);
        }
    }

    /** Breaks every log connected to {@code start} (up to {@code field_saw.max_logs}), paid per log and once in vis. */
    private static void fell(ServerPlayer player, ServerLevel level, BlockPos start, ItemStack tool, FieldToolItem saw) {
        int max = (int) ArcanaTuning.get("field_saw.max_logs");
        float vis = (float) ArcanaTuning.get("field_saw.vis_per_tree");
        if (vis > 0 && AuraHelper.drainVis(level, start, vis, true) < vis) {
            return;
        }
        List<BlockPos> logs = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && logs.size() < max) {
            BlockPos p = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos n = p.offset(dx, dy, dz);
                        if (seen.add(n) && level.isLoaded(n) && level.getBlockState(n).is(BlockTags.LOGS)) {
                            logs.add(n);
                            queue.add(n);
                        }
                    }
                }
            }
        }
        if (logs.isEmpty()) {
            return;
        }
        if (vis > 0) {
            AuraHelper.drainVis(level, start, vis, false);
        }
        BUSY.add(player);
        try {
            for (BlockPos p : logs) {
                if (!Charge.has(tool, saw.rfPerBlock())) {
                    break;
                }
                player.gameMode.destroyBlock(p);
            }
        } finally {
            BUSY.remove(player);
        }
    }

    /** Refining drill: every smeltable drop comes out smelted, for RF and the chunk's vis per item. */
    private static void onDrops(BlockDropsEvent e) {
        ItemStack tool = e.getTool();
        if (!(tool.getItem() instanceof FieldToolItem.Drill) || FieldToolItem.mode(tool) != FieldToolItem.Drill.REFINING) {
            return;
        }
        ServerLevel level = e.getLevel();
        int rfEach = (int) ArcanaTuning.get("field_drill.refine_rf");
        float visEach = (float) ArcanaTuning.get("field_drill.refine_vis");
        for (ItemEntity drop : e.getDrops()) {
            ItemStack in = drop.getItem();
            SingleRecipeInput input = new SingleRecipeInput(in.copyWithCount(1));
            Optional<RecipeHolder<SmeltingRecipe>> recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level);
            if (recipe.isEmpty()) {
                continue;
            }
            ItemStack out = recipe.get().value().assemble(input, level.registryAccess());
            int n = in.getCount();
            float vis = visEach * n;
            if (out.isEmpty() || !Charge.has(tool, rfEach * n) || (vis > 0 && AuraHelper.drainVis(level, e.getPos(), vis, true) < vis)) {
                continue;
            }
            Charge.use(tool, rfEach * n);
            if (vis > 0) {
                AuraHelper.drainVis(level, e.getPos(), vis, false);
            }
            drop.setItem(out.copyWithCount(Math.min(out.getMaxStackSize(), out.getCount() * n)));
        }
    }
}
