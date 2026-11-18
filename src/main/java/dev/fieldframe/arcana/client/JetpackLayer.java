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
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * The Galvanic Jetpack on the wearer's back: a frame, two tanks, two nozzles and a glowing coil between the tanks
 * ({@code textures/models/galvanic_jetpack.png}, 32x32), posed with the body. The straps are the item's humanoid
 * equipment layer.
 */
public final class JetpackLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(FieldframeArcana.id("galvanic_jetpack"), "main");
    private static final Identifier TEXTURE = FieldframeArcana.id("textures/models/galvanic_jetpack.png");
    private static final Identifier ARMORED_TEXTURE = FieldframeArcana.id("textures/models/armored_galvanic_jetpack.png");
    private static final int FULL_BRIGHT = 0xF000F0;
    private final ModelPart pack;
    private final ModelPart coil;

    public JetpackLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityModelSet models) {
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
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible || !(state.chestEquipment.getItem() instanceof GalvanicJetpackItem item)) {
            return;
        }
        Identifier texture = item.armored() ? ARMORED_TEXTURE : TEXTURE;
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        RenderType type = RenderTypes.armorCutoutNoCull(texture);
        collector.submitModelPart(pack, pose, type, light, OverlayTexture.NO_OVERLAY, null);
        collector.submitModelPart(coil, pose, RenderTypes.entityTranslucentEmissive(texture), FULL_BRIGHT, OverlayTexture.NO_OVERLAY, null);
        pose.popPose();
    }
}
