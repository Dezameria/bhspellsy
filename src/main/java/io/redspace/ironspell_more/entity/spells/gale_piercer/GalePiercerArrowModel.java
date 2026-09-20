package io.redspace.ironspell_more.entity.spells.gale_piercer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironspell_more.IronSpellMore;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class GalePiercerArrowModel {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(IronSpellMore.id("gale_piercer_arrow"), "main");

    private final ModelPart arrow;

    public GalePiercerArrowModel(ModelPart root) {
        this.arrow = root.getChild("arrow");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition arrowPart = root.addOrReplaceChild("arrow",
                CubeListBuilder.create()
                        // Shaft
                        .texOffs(0, 0)
                        .addBox(-0.5F, -0.5F, -8.0F, 1.0F, 1.0F, 16.0F, new CubeDeformation(0.0F))
                        // Arrowhead
                        .texOffs(0, 0)
                        .addBox(-1.0F, -1.0F, 8.0F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
                        // Fletching
                        .texOffs(0, 0)
                        .addBox(-1.5F, -1.5F, -8.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 32, 32);
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                       float red, float green, float blue, float alpha) {
        arrow.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, alpha);
    }
}
