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
import java.util.Map;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler;
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
    private static final ArmorMaterial LENS = armor("field_lens", 15, ArmorType.HELMET, 2);
    private static final ArmorMaterial BOOTS = armor("galvanic_boots", 20, ArmorType.BOOTS, 3);
    private static final ArmorMaterial WINGS = armor("galvanic_wings", 20, ArmorType.CHESTPLATE, 4);
    private static final ArmorMaterial JETPACK = armor("galvanic_jetpack", 25, ArmorType.CHESTPLATE, 5);
    /** Thaumium chest plating over the pack: thaumium chest defence, a little toughness. */
    private static final ArmorMaterial ARMORED_JETPACK = armor("armored_galvanic_jetpack", 33, ArmorType.CHESTPLATE, 7, 1f);

    public static final DeferredItem<FieldLensItem> FIELD_LENS = ITEMS.registerItem("field_lens", props -> new FieldLensItem(props
            .humanoidArmor(LENS, ArmorType.HELMET).component(TTDataComponents.GOGGLES_UPGRADE.get(), Unit.INSTANCE).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<GalvanicArmorItem> GALVANIC_BOOTS = ITEMS.registerItem("galvanic_boots", props -> new GalvanicArmorItem(props
            .humanoidArmor(BOOTS, ArmorType.BOOTS).rarity(Rarity.UNCOMMON), "galvanic_boots"));
    public static final DeferredItem<GalvanicArmorItem> GALVANIC_WINGS = ITEMS.registerItem("galvanic_wings", props -> new GalvanicArmorItem(props
            .humanoidArmor(WINGS, ArmorType.CHESTPLATE).component(DataComponents.GLIDER, Unit.INSTANCE).rarity(Rarity.RARE), "galvanic_wings"));

    public static final DeferredItem<GalvanicJetpackItem> GALVANIC_JETPACK = ITEMS.registerItem("galvanic_jetpack", props -> new GalvanicJetpackItem(props
            .humanoidArmor(JETPACK, ArmorType.CHESTPLATE).rarity(Rarity.RARE)));
    public static final DeferredItem<GalvanicJetpackItem> ARMORED_GALVANIC_JETPACK = ITEMS.registerItem("armored_galvanic_jetpack",
            props -> new GalvanicJetpackItem(props.humanoidArmor(ARMORED_JETPACK, ArmorType.CHESTPLATE).rarity(Rarity.EPIC), "armored_galvanic_jetpack"));

    /** Blocks the Field Drill and Field Saw mine at full speed. Datapacks extend or replace these tags. */
    public static final net.minecraft.tags.TagKey<Block> DRILL_MINEABLE = net.minecraft.tags.TagKey.create(Registries.BLOCK, FieldframeArcana.id("field_drill_mineable"));
    public static final net.minecraft.tags.TagKey<Block> SAW_MINEABLE = net.minecraft.tags.TagKey.create(Registries.BLOCK, FieldframeArcana.id("field_saw_mineable"));

    // ---- held --------------------------------------------------------------------------------------------------
    public static final DeferredItem<FieldToolItem.Drill> FIELD_DRILL = ITEMS.registerItem("thaumium_field_drill", props -> new FieldToolItem.Drill(
            tool(props, DRILL_MINEABLE, 6f, -2.8f)));
    public static final DeferredItem<FieldToolItem.Saw> FIELD_SAW = ITEMS.registerItem("thaumium_field_saw", props -> new FieldToolItem.Saw(
            tool(props, SAW_MINEABLE, 8f, -3.0f)));
    public static final DeferredItem<GalvanicSwordItem> GALVANIC_SWORD = ITEMS.registerItem("galvanic_sword", props -> new GalvanicSwordItem(
            props.stacksTo(1).enchantable(14).rarity(Rarity.RARE).attributes(ItemAttributeModifiers.builder()
                    .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 7f, AttributeModifier.Operation.ADD_VALUE),
                            EquipmentSlotGroup.MAINHAND)
                    .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.4f, AttributeModifier.Operation.ADD_VALUE),
                            EquipmentSlotGroup.MAINHAND)
                    .build())));
    public static final DeferredItem<GalvanicBowItem> GALVANIC_BOW = ITEMS.registerItem("galvanic_bow", props -> new GalvanicBowItem(
            props.stacksTo(1).enchantable(1).rarity(Rarity.RARE)));

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

    private static ArmorMaterial armor(String name, int durability, ArmorType type, int defense) {
        return armor(name, durability, type, defense, 0f);
    }

    private static ArmorMaterial armor(String name, int durability, ArmorType type, int defense, float toughness) {
        Map<ArmorType, Integer> def = new EnumMap<>(ArmorType.class);
        for (ArmorType t : ArmorType.values()) {
            def.put(t, t == type ? defense : 0);
        }
        ResourceKey<EquipmentAsset> asset = ResourceKey.create(EquipmentAssets.ROOT_ID, FieldframeArcana.id(name));
        return new ArmorMaterial(durability, def, 12, SoundEvents.ARMOR_EQUIP_IRON, toughness, 0f, ItemTags.REPAIRS_IRON_ARMOR, asset);
    }

    /** A thaumium-grade RF tool: no durability, mines what {@code tag} covers at diamond level, digs fast. */
    private static Item.Properties tool(Item.Properties props, net.minecraft.tags.TagKey<Block> tag, float damage, float speed) {
        HolderGetter<Block> blocks = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
        List<Tool.Rule> rules = new java.util.ArrayList<>();
        rules.add(Tool.Rule.deniesDrops(blocks.getOrThrow(BlockTags.INCORRECT_FOR_DIAMOND_TOOL)));
        rules.add(Tool.Rule.minesAndDrops(blocks.getOrThrow(tag), 10f));
        return props.stacksTo(1).enchantable(14).rarity(Rarity.UNCOMMON)
                .component(DataComponents.TOOL, new Tool(rules, 1f, 0, true))
                .attributes(ItemAttributeModifiers.builder()
                        .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damage, AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND)
                        .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, speed, AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND)
                        .build());
    }

    static void register(IEventBus bus) {
        ArcanaComponents.register(bus);
        ITEMS.register(bus);
        RODS.register(bus);
        TABS.register(bus);
        bus.addListener(ARegistry::capabilities);
    }

    private static void capabilities(RegisterCapabilitiesEvent e) {
        for (DeferredItem<? extends Item> item : POWERED) {
            String group = ((Charge.Powered) item.get()).tuningGroup();
            e.registerItem(Capabilities.Energy.ITEM, (stack, access) -> {
                int cap = (int) ArcanaTuning.get(group + ".capacity");
                int io = (int) ArcanaTuning.get(group + ".max_io");
                return new ItemAccessEnergyHandler(access, ArcanaComponents.ENERGY.get(), cap, io, io);
            }, item.get());
        }
    }
}
