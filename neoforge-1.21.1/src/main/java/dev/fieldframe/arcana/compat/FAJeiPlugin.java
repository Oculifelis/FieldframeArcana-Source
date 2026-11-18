package dev.fieldframe.arcana.compat;

import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaCrystalAccess;
import dev.fieldframe.api.id.FfId;
import dev.fieldframe.arcana.FieldframeArcana;
import dev.fieldframe.arcana.data.Condensers;
import dev.fieldframe.arcana.data.EssentiaFuels;
import dev.fieldframe.arcana.module.ArcanaModules;
import dev.fieldframe.neoforge.api.FieldframeForgeApi;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;

/**
 * JEI: an Essentia Dynamo category (RF and flux per point of each aspect) and a Primal Condenser category (what it
 * condenses, when, and for how much RF), both from the tables {@code ArcanaDataSync} sends, plus information pages for
 * the modules that have no recipe page of their own. The Etheric Crusher's process family is Fieldframe's own JEI
 * category; arcane recipes are Thaumaturge's. Loaded only with JEI.
 */
@JeiPlugin
public final class FAJeiPlugin implements IModPlugin {
    record Fuel(ItemStack crystal, EssentiaFuels.Fuel fuel) {
    }

    record Condense(ItemStack result, Condensers.Entry entry) {
    }

    static final RecipeType<Fuel> DYNAMO = new RecipeType<>(FieldframeArcana.id("essentia_dynamo"), Fuel.class);
    static final RecipeType<Condense> CONDENSER = new RecipeType<>(FieldframeArcana.id("primal_condenser"), Condense.class);

    private static final List<FfId> INFO = List.of(ArcanaModules.AURA_TAP, ArcanaModules.VIS_CHARGER, ArcanaModules.FLUX_SCRUBBER,
            ArcanaModules.NODE_CRADLE);

    @Override
    public ResourceLocation getPluginUid() {
        return FieldframeArcana.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new DynamoCategory(gui), new CondenserCategory(gui));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(FieldframeForgeApi.moduleItem(ArcanaModules.ESSENTIA_DYNAMO)), DYNAMO);
        registration.addRecipeCatalyst(new ItemStack(FieldframeForgeApi.moduleItem(ArcanaModules.PRIMAL_CONDENSER)), CONDENSER);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<Fuel> fuels = new ArrayList<>();
        List<Condense> condensers = new ArrayList<>();
        var level = Minecraft.getInstance().level;
        if (level != null) {
            var aspects = level.registryAccess().lookupOrThrow(IAspect.REGISTRY_KEY);
            new TreeMap<>(EssentiaFuels.all()).forEach((aspect, fuel) -> aspects.get(ResourceKey.create(IAspect.REGISTRY_KEY,
                    ResourceLocation.parse(aspect))).ifPresent(h -> fuels.add(new Fuel(EssentiaCrystalAccess.create(h, 1), fuel))));
            for (Condensers.Entry entry : Condensers.all()) {
                ItemStack result = entry.result() != null
                        ? BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(entry.result())).map(ItemStack::new).orElse(ItemStack.EMPTY)
                        : aspects.get(ResourceKey.create(IAspect.REGISTRY_KEY, ResourceLocation.parse(entry.aspect())))
                                .map(h -> EssentiaCrystalAccess.create(h, 1)).orElse(ItemStack.EMPTY);
                if (!result.isEmpty()) {
                    condensers.add(new Condense(result, entry));
                }
            }
        }
        registration.addRecipes(DYNAMO, fuels);
        registration.addRecipes(CONDENSER, condensers);
        for (FfId module : INFO) {
            registration.addIngredientInfo(FieldframeForgeApi.moduleItem(module),
                    Component.translatable("module." + module.namespace() + "." + module.path() + ".desc"));
        }
    }

    /** "rain", "below_y:40", "dimension:minecraft:the_nether" as a line of text. */
    private static Component when(String when) {
        int colon = when.indexOf(':');
        String kind = colon < 0 ? when : when.substring(0, colon);
        String key = "jei.fieldframe_arcana.when." + kind;
        return colon < 0 ? Component.translatableWithFallback(key, when)
                : Component.translatableWithFallback(key, when, when.substring(colon + 1));
    }

    private static final class DynamoCategory extends AbstractRecipeCategory<Fuel> {
        DynamoCategory(IGuiHelper gui) {
            super(DYNAMO, Component.translatable("jei.fieldframe_arcana.essentia_dynamo"),
                    gui.createDrawableItemLike(FieldframeForgeApi.moduleItem(ArcanaModules.ESSENTIA_DYNAMO)), 140, 44);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, Fuel recipe, IFocusGroup focuses) {
            builder.addInputSlot(4, 5).setStandardSlotBackground().addItemStack(recipe.crystal());
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, Fuel recipe, IFocusGroup focuses) {
            builder.addText(Component.translatable("jei.fieldframe_arcana.essentia_dynamo.rf",
                    String.format("%,d", recipe.fuel().rf())), 110, 10).setPosition(28, 5).setColor(0xFF404040);
            builder.addText(Component.translatable("jei.fieldframe_arcana.essentia_dynamo.flux",
                    String.format("%.2f", recipe.fuel().flux())), 110, 10).setPosition(28, 16).setColor(0xFF707070);
        }
    }

    private static final class CondenserCategory extends AbstractRecipeCategory<Condense> {
        CondenserCategory(IGuiHelper gui) {
            super(CONDENSER, Component.translatable("jei.fieldframe_arcana.primal_condenser"),
                    gui.createDrawableItemLike(FieldframeForgeApi.moduleItem(ArcanaModules.PRIMAL_CONDENSER)), 140, 44);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, Condense recipe, IFocusGroup focuses) {
            builder.addOutputSlot(118, 5).setOutputSlotBackground().addItemStack(recipe.result());
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, Condense recipe, IFocusGroup focuses) {
            builder.addAnimatedRecipeArrowWidget(recipe.entry().ticks()).setPosition(88, 5);
            builder.addText(when(recipe.entry().when()), 86, 10).setPosition(0, 5).setColor(0xFF404040);
            builder.addText(Component.translatable("jei.fieldframe_arcana.primal_condenser.cost",
                    String.format("%,d", recipe.entry().rf()), recipe.entry().ticks() / 20f), 140, 10)
                    .setPosition(0, 28).setColor(0xFF707070);
        }
    }
}
