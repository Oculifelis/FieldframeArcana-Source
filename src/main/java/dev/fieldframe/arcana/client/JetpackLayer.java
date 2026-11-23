package dev.fieldframe.arcana.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.fieldframe.arcana.FieldframeArcana;
import dev.fieldframe.arcana.item.GalvanicJetpackItem;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * The Galvanic Jetpack on the wearer's back: a frame, two tanks, two nozzles and a glowing coil between the tanks
 * ({@code textures/models/galvanic_jetpack.png}, 32x32), posed with the body. The straps are the item's armour
 * layer.
 */
public final class JetpackLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(FieldframeArcana.id("galvanic_jetpack"), "main");
    private static final ResourceLocation TEXTURE = FieldframeArcana.id("textures/models/galvanic_jetpack.png");
    private static final ResourceLocation ARMORED_TEXTURE = FieldframeArcana.id("textures/models/armored_galvanic_jetpack.png");
    private static final int FULL_BRIGHT = 0xF000F0;
    private final ModelPart pack;
    private final ModelPart coil;

    public JetpackLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet models) {
        super(parent);
        ModelPart root = models.bakeLayer(LAYER);
        pack = root.getChild("pack");
        coil = root.getChild("coil");
    }

    /** Body-space boxes (the body is x -4..4, y 0..12, z -2..2; its back faces +z). */
    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("pack", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3f, 1f, 2f, 6, 8, 2)
                .texOffs(0, 10).addBox(-4.5f, 0f, 2.5f, 3, 9, 3)
                .texOffs(12, 10).mirror().addBox(1.5f, 0f, 2.5f, 3, 9, 3).mirror(false)
                .texOffs(0, 22).addBox(-4f, 9f, 3f, 2, 2, 2)
                .texOffs(8, 22).mirror().addBox(2f, 9f, 3f, 2, 2, 2), PartPose.ZERO);
        root.addOrReplaceChild("coil", CubeListBuilder.create().texOffs(16, 0).addBox(-1f, 2f, 4f, 2, 6, 1), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
            float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible() || !(player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof GalvanicJetpackItem item)) {
            return;
        }
        ResourceLocation texture = item.armored() ? ARMORED_TEXTURE : TEXTURE;
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        pack.render(pose, buffers.getBuffer(RenderType.armorCutoutNoCull(texture)), light, OverlayTexture.NO_OVERLAY);
        coil.render(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(texture)), FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
