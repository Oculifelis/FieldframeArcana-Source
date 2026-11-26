package dev.fieldframe.arcana;

import com.leclowndu93150.thaumaturge.api.wands.WandRod;
import com.leclowndu93150.thaumaturge.content.wands.ItemWandRod;
import com.leclowndu93150.thaumaturge.registry.TTDataComponents;
import dev.fieldframe.api.id.FfId;
import dev.fieldframe.arcana.item.ArcanaComponents;
import dev.fieldframe.arcana.item.Charge;
import dev.fieldframe.arcana.item.FieldLensItem;
import dev.fieldframe.arcana.item.FieldToolItem;
import dev.fieldframe.arcana.item.GalvanicArmorItem;
import dev.fieldframe.arcana.item.GalvanicBowItem;
import dev.fieldframe.arcana.item.GalvanicJetpackItem;
import dev.fieldframe.arcana.item.GalvanicSwordItem;
import dev.fieldframe.arcana.module.ArcanaModules;
import dev.fieldframe.arcana.wand.GalvanicRod;
import dev.fieldframe.arcana.wand.VoltaicCore;
import dev.fieldframe.neoforge.api.FieldframeForgeApi;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.energy.ComponentEnergyStorage;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import dev.fieldframe.arcana.data.ArcanaTuning;

public final class ARegistry {
    private static final String MODID = FieldframeArcana.MODID;

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<WandRod> RODS = DeferredRegister.create(WandRod.REGISTRY_KEY, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // ---- wand parts --------------------------------------------------------------------------------------------
    /** Capacity 75 like the primal rods, but no aura recharge: it refills from carried energy items instead. */
    public static final DeferredHolder<WandRod, WandRod> ROD_GALVANIC = RODS.register("galvanic", () -> new WandRod(75, 6,
            FieldframeArcana.id("textures/models/wand_rod_galvanic.png"), new GalvanicRod(false), true, false, false,
            FieldframeArcana.id("galvanic_rod")));
    public static final DeferredHolder<WandRod, WandRod> STAFF_GALVANIC = RODS.register("galvanic_staff", () -> new WandRod(175, 14,
            FieldframeArcana.id("textures/models/wand_rod_galvanic.png"), new GalvanicRod(true), true, true, false,
            FieldframeArcana.id("galvanic_staff")));
    /** Silverwood tier: half of every vis draw is paid from an RF buffer it keeps topped up from carried energy items. */
    private static final VoltaicCore VOLTAIC_WAND = new VoltaicCore(false), VOLTAIC_STAFF = new VoltaicCore(true);
    public static final DeferredHolder<WandRod, WandRod> ROD_VOLTAIC = RODS.register("voltaic", () -> new WandRod(100, 9,
            FieldframeArcana.id("textures/models/wand_rod_voltaic.png"), VOLTAIC_WAND, true, false, false,
            FieldframeArcana.id("voltaic_core"), VOLTAIC_WAND));
    public static final DeferredHolder<WandRod, WandRod> STAFF_VOLTAIC = RODS.register("voltaic_staff", () -> new WandRod(250, 24,
            FieldframeArcana.id("textures/models/wand_rod_voltaic.png"), VOLTAIC_STAFF, true, true, false,
            FieldframeArcana.id("voltaic_staff"), VOLTAIC_STAFF));
    public static final DeferredItem<ItemWandRod> WAND_ROD_VOLTAIC = ITEMS.registerItem("wand_rod_voltaic",
            props -> new ItemWandRod(props, ROD_VOLTAIC));
    public static final DeferredItem<ItemWandRod> STAFF_ROD_VOLTAIC = ITEMS.registerItem("staff_rod_voltaic",
            props -> new ItemWandRod(props, STAFF_VOLTAIC));
    public static final DeferredItem<ItemWandRod> WAND_ROD_GALVANIC = ITEMS.registerItem("wand_rod_galvanic",
            props -> new ItemWandRod(props, ROD_GALVANIC));
    public static final DeferredItem<ItemWandRod> STAFF_ROD_GALVANIC = ITEMS.registerItem("staff_rod_galvanic",
            props -> new ItemWandRod(props, STAFF_GALVANIC));

    // ---- worn --------------------------------------------------------------------------------------------------
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, MODID);
    private static final Holder<ArmorMaterial> LENS = armor("field_lens", ArmorItem.Type.HELMET, 2, 0f, true);
    private static final Holder<ArmorMaterial> BOOTS = armor("galvanic_boots", ArmorItem.Type.BOOTS, 3, 0f, true);
    private static final Holder<ArmorMaterial> WINGS = armor("galvanic_wings", ArmorItem.Type.CHESTPLATE, 4, 0f, false);
    private static final Holder<ArmorMaterial> JETPACK = armor("galvanic_jetpack", ArmorItem.Type.CHESTPLATE, 5, 0f, true);
    /** Thaumium chest plating over the pack: thaumium chest defence, a little toughness. */
    private static final Holder<ArmorMaterial> ARMORED_JETPACK = armor("armored_galvanic_jetpack", ArmorItem.Type.CHESTPLATE, 7, 1f, true);

    public static final DeferredItem<FieldLensItem> FIELD_LENS = ITEMS.registerItem("field_lens", props -> new FieldLensItem(LENS, props
            .durability(ArmorItem.Type.HELMET.getDurability(15)).component(TTDataComponents.GOGGLES_UPGRADE.get(), Unit.INSTANCE).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<GalvanicArmorItem> GALVANIC_BOOTS = ITEMS.registerItem("galvanic_boots", props -> new GalvanicArmorItem(BOOTS,
            ArmorItem.Type.BOOTS, props.durability(ArmorItem.Type.BOOTS.getDurability(20)).rarity(Rarity.UNCOMMON), "galvanic_boots"));
    public static final DeferredItem<GalvanicArmorItem> GALVANIC_WINGS = ITEMS.registerItem("galvanic_wings", props -> new GalvanicArmorItem(WINGS,
            ArmorItem.Type.CHESTPLATE, props.durability(ArmorItem.Type.CHESTPLATE.getDurability(20)).rarity(Rarity.RARE), "galvanic_wings"));

    public static final DeferredItem<GalvanicJetpackItem> GALVANIC_JETPACK = ITEMS.registerItem("galvanic_jetpack", props -> new GalvanicJetpackItem(JETPACK,
            props.durability(ArmorItem.Type.CHESTPLATE.getDurability(25)).rarity(Rarity.RARE)));
    public static final DeferredItem<GalvanicJetpackItem> ARMORED_GALVANIC_JETPACK = ITEMS.registerItem("armored_galvanic_jetpack",
            props -> new GalvanicJetpackItem(ARMORED_JETPACK, props.durability(ArmorItem.Type.CHESTPLATE.getDurability(33)).rarity(Rarity.EPIC),
                    "armored_galvanic_jetpack"));

    /** Blocks the Field Drill and Field Saw mine at full speed. Datapacks extend or replace these tags. */
    public static final net.minecraft.tags.TagKey<Block> DRILL_MINEABLE = net.minecraft.tags.TagKey.create(Registries.BLOCK, FieldframeArcana.id("field_drill_mineable"));
    public static final net.minecraft.tags.TagKey<Block> SAW_MINEABLE = net.minecraft.tags.TagKey.create(Registries.BLOCK, FieldframeArcana.id("field_saw_mineable"));

    // ---- held --------------------------------------------------------------------------------------------------
    public static final DeferredItem<FieldToolItem.Drill> FIELD_DRILL = ITEMS.registerItem("thaumium_field_drill", props -> new FieldToolItem.Drill(
            tool(props, DRILL_MINEABLE, 6f, -2.8f)));
    public static final DeferredItem<FieldToolItem.Saw> FIELD_SAW = ITEMS.registerItem("thaumium_field_saw", props -> new FieldToolItem.Saw(
            tool(props, SAW_MINEABLE, 8f, -3.0f)));
    public static final DeferredItem<GalvanicSwordItem> GALVANIC_SWORD = ITEMS.registerItem("galvanic_sword", props -> new GalvanicSwordItem(
            props.stacksTo(1).rarity(Rarity.RARE).attributes(weapon(7f, -2.4f))));
    public static final DeferredItem<GalvanicBowItem> GALVANIC_BOW = ITEMS.registerItem("galvanic_bow", props -> new GalvanicBowItem(
            props.stacksTo(1).rarity(Rarity.RARE)));

    public static final List<DeferredItem<? extends Item>> POWERED = List.of(FIELD_LENS, GALVANIC_BOOTS, GALVANIC_WINGS, GALVANIC_JETPACK, ARMORED_GALVANIC_JETPACK, FIELD_DRILL, FIELD_SAW,
            GALVANIC_SWORD, GALVANIC_BOW);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.fieldframe_arcana"))
            .icon(() -> new ItemStack(FieldframeForgeApi.moduleItem(ArcanaModules.ESSENTIA_DYNAMO)))
            .displayItems((params, out) -> {
                for (FfId module : ArcanaModules.ALL) {
                    out.accept(FieldframeForgeApi.moduleItem(module));
                }
                out.accept(WAND_ROD_GALVANIC.get());
                out.accept(STAFF_ROD_GALVANIC.get());
                out.accept(WAND_ROD_VOLTAIC.get());
                out.accept(STAFF_ROD_VOLTAIC.get());
                for (DeferredItem<? extends Item> item : POWERED) {
                    out.accept(item.get());
                    ItemStack full = new ItemStack(item.get());
                    Charge.fill(full);
                    out.accept(full);
                }
            })
            .build());

    private ARegistry() {
    }

    private static Holder<ArmorMaterial> armor(String name, ArmorItem.Type type, int defense, float toughness, boolean layer) {
        EnumMap<ArmorItem.Type, Integer> def = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type t : ArmorItem.Type.values()) {
            def.put(t, t == type ? defense : 0);
        }
        List<ArmorMaterial.Layer> layers = layer ? List.of(new ArmorMaterial.Layer(FieldframeArcana.id(name))) : List.of();
        return ARMOR_MATERIALS.register(name, () -> new ArmorMaterial(def, 12, SoundEvents.ARMOR_EQUIP_IRON,
                () -> Ingredient.of(Items.IRON_INGOT), layers, toughness, 0f));
    }

    private static final ResourceLocation BASE_DAMAGE = ResourceLocation.withDefaultNamespace("base_attack_damage");
    private static final ResourceLocation BASE_SPEED = ResourceLocation.withDefaultNamespace("base_attack_speed");

    static ItemAttributeModifiers weapon(float damage, float speed) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_DAMAGE, damage, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_SPEED, speed, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    /** A thaumium-grade RF tool: no durability, mines what {@code tag} covers at diamond level, digs fast. */
    private static Item.Properties tool(Item.Properties props, net.minecraft.tags.TagKey<Block> tag, float damage, float speed) {
        List<Tool.Rule> rules = new java.util.ArrayList<>();
        rules.add(Tool.Rule.deniesDrops(BlockTags.INCORRECT_FOR_DIAMOND_TOOL));
        rules.add(Tool.Rule.minesAndDrops(tag, 10f));
        return props.stacksTo(1).rarity(Rarity.UNCOMMON)
                .component(DataComponents.TOOL, new Tool(rules, 1f, 0))
                .attributes(weapon(damage, speed));
    }

    static void register(IEventBus bus) {
        ArcanaComponents.register(bus);
        ARMOR_MATERIALS.register(bus);
        ITEMS.register(bus);
        RODS.register(bus);
        TABS.register(bus);
        bus.addListener(ARegistry::capabilities);
    }

    private static void capabilities(RegisterCapabilitiesEvent e) {
        for (DeferredItem<? extends Item> item : POWERED) {
            String group = ((Charge.Powered) item.get()).tuningGroup();
            e.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ctx) -> {
                int cap = (int) ArcanaTuning.get(group + ".capacity");
                int io = (int) ArcanaTuning.get(group + ".max_io");
                return new ComponentEnergyStorage(stack, ArcanaComponents.ENERGY.get(), cap, io, io);
            }, item.get());
        }
    }
}
