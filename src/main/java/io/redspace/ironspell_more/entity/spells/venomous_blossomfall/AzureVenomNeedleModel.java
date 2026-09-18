package io.redspace.ironspell_more.entity.spells.venomous_blossomfall;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironspell_more.IronSpellMore;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.texture.OverlayTexture;

public final class AzureVenomNeedleModel {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(IronSpellMore.id("azure_venom_needle"), "main");

    private final ModelPart needle;

    public AzureVenomNeedleModel(ModelPart root) {
        needle = root.getChild("needle");
    }

    /** Temporary chopstick-like geometry, ready to be replaced by a future geo model. */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("needle",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.75F, -0.75F, -9.0F, 1.5F, 1.5F, 16.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(0, 0)
                        .addBox(-0.45F, -0.45F, -12.0F, 0.9F, 0.9F, 3.0F,
                                new CubeDeformation(-0.08F)),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight,
            float red, float green, float blue, float alpha) {
        needle.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                red, green, blue, alpha);
    }
}
