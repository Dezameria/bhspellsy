package io.redspace.ironspell_more.entity.spells.gale_piercer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class GalePiercerArrowModel {
    private static final float TEXTURE_U_MAX = 0.5F;
    private static final float TEXTURE_V_MAX = 0.15625F;

    private GalePiercerArrowModel() {
    }

    public static void orientToDirection(PoseStack poseStack, Vec3 direction) {
        Vec3 normalized = direction.lengthSqr() < 1.0E-8D
                ? new Vec3(0.0D, 0.0D, 1.0D)
                : direction.normalize();
        float xRot = -((float) (Mth.atan2(normalized.horizontalDistance(), normalized.y) * Mth.RAD_TO_DEG) - 90.0F);
        float yRot = -((float) (Mth.atan2(normalized.z, normalized.x) * Mth.RAD_TO_DEG) + 90.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
    }

    public static void render(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                              float scale, int red, int green, int blue, int alpha) {
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(-2.0D, 0.0D, 0.0D);

        // The source textures are horizontal arrow sprites, so render them as four crossed quads.
        for (int side = 0; side < 4; side++) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            PoseStack.Pose pose = poseStack.last();
            vertex(pose, consumer, -8.0F, -2.0F, 0.0F, 0.0F, 0.0F,
                    packedLight, red, green, blue, alpha);
            vertex(pose, consumer, 8.0F, -2.0F, 0.0F, TEXTURE_U_MAX, 0.0F,
                    packedLight, red, green, blue, alpha);
            vertex(pose, consumer, 8.0F, 2.0F, 0.0F, TEXTURE_U_MAX, TEXTURE_V_MAX,
                    packedLight, red, green, blue, alpha);
            vertex(pose, consumer, -8.0F, 2.0F, 0.0F, 0.0F, TEXTURE_V_MAX,
                    packedLight, red, green, blue, alpha);
        }
        poseStack.popPose();
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer,
                               float x, float y, float z, float u, float v,
                               int packedLight, int red, int green, int blue, int alpha) {
        Matrix4f poseMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        consumer.vertex(poseMatrix, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normalMatrix, 0.0F, 0.0F, 1.0F)
                .endVertex();
    }
}
