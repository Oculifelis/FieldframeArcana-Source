package dev.fieldframe.arcana.item;

import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import dev.fieldframe.api.data.FfData;
import dev.fieldframe.arcana.data.ArcanaTuning;
import dev.fieldframe.neoforge.api.FieldframeForgeApi;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Electric goggles of revealing. Carries Thaumaturge's goggles component, so it reveals like the Goggles of Revealing.
 * Worn and charged, it costs {@code field_lens.rf_per_second} and gives night vision; while the wearer sneaks it
 * shows the status of every module of the chassis in view, or the vis and flux of the chunk, on the action bar.
 */
public class FieldLensItem extends ArmorItem implements Charge.Powered {
    public FieldLensItem(Holder<ArmorMaterial> material, Properties props) {
        super(material, Type.HELMET, props);
    }

    @Override
    public String tuningGroup() {
        return "field_lens";
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity owner, int slotId, boolean selected) {
        if (!(world instanceof ServerLevel level) || !Charge.worn(owner, stack, EquipmentSlot.HEAD) || !(owner instanceof Player player)
                || level.getGameTime() % 20 != 0) {
            return;
        }
        if (!Charge.use(stack, (int) ArcanaTuning.get("field_lens.rf_per_second"))) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false));
        if (player.isShiftKeyDown()) {
            dev.fieldframe.arcana.item.Charge.overlay(player, readout(level, player));
        }
    }

    private static Component readout(ServerLevel level, Player player) {
        HitResult hit = player.pick(6.0, 0f, false);
        if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
            FfData chassis = FieldframeForgeApi.inspect(level, bhr.getBlockPos());
            if (chassis != null) {
                return chassisLine(chassis);
            }
        }
        BlockPos pos = player.blockPosition();
        return Component.translatable("fieldframe_arcana.lens.aura", Math.round(AuraHelper.getVis(level, pos)),
                Math.round(AuraHelper.getFlux(level, pos)), AuraHelper.getAuraBase(level, pos)).withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    private static Component chassisLine(FfData chassis) {
        MutableComponent line = Component.translatable("fieldframe_arcana.lens.energy", chassis.getLong("energy", 0),
                chassis.getLong("energyCap", 0)).withStyle(ChatFormatting.GOLD);
        FfData faces = chassis.getDataOrNull("faces");
        if (faces != null) {
            for (String face : faces.keys()) {
                FfData f = faces.getData(face);
                List<Object> args = f.getList("args");
                line.append(Component.literal("  " + face.toUpperCase(Locale.ROOT).charAt(0) + ": ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(Component.translatable(f.getString("name", f.getString("module", "?"))).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(" "))
                        .append(Component.translatable("fieldframe.status." + f.getString("status", "idle").toLowerCase(Locale.ROOT),
                                args == null ? new Object[0] : args.toArray()).withStyle(color(f.getString("severity", "IDLE"))));
            }
        }
        return line;
    }

    private static ChatFormatting color(String severity) {
        return switch (severity) {
            case "WORKING" -> ChatFormatting.GREEN;
            case "BLOCKED", "FAULT" -> ChatFormatting.RED;
            case "DISABLED" -> ChatFormatting.GRAY;
            default -> ChatFormatting.YELLOW;
        };
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Charge.barWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Charge.barColor(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, java.util.List<Component> out, TooltipFlag flag) {
        out.add(Charge.tooltip(stack));
        out.add(Component.translatable("tooltip.fieldframe_arcana.field_lens").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
