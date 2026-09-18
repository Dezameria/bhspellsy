package io.redspace.ironspell_more.entity.spells.wind_arrow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class WindArrowRenderer extends EntityRenderer<WindArrowEntity> {
    public static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/projectiles/arrow.png");

    public WindArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(WindArrowEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        Vec3 motion = entity.getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-6D) {
            float yRot = (float) (Mth.atan2(motion.x, motion.z) * (180.0D / Math.PI));
            float xRot = (float) (Mth.atan2(motion.y, motion.horizontalDistance()) * (180.0D / Math.PI));
            poseStack.mulPose(Axis.YP.rotationDegrees(yRot - 90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(xRot));
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
        }

        // Roll rotation during flight
        float roll = (entity.tickCount + partialTick) * (entity.isFullCharge() ? 25.0F : 12.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(roll));

        boolean full = entity.isFullCharge();
        int r = full ? 255 : 190;
        int g = full ? 130 : 255;
        int b = full ? 40 : 220;
        int a = full ? 255 : 220;
        int light = full ? LightTexture.FULL_BRIGHT : packedLight;

        RenderType renderType = full
                ? RenderType.entityTranslucentEmissive(TEXTURE)
                : RenderType.entityTranslucent(TEXTURE);
        VertexConsumer consumer = bufferSource.getBuffer(renderType);

        poseStack.scale(0.05625F, 0.05625F, 0.05625F);
        poseStack.translate(-4.0F, 0.0F, 0.0F);

        PoseStack.Pose lastPose = poseStack.last();
        Matrix4f poseMatrix = lastPose.pose();
        Matrix3f normalMatrix = lastPose.normal();

        // Render cross quads for arrow shaft and fins
        for (int quad = 0; quad < 4; ++quad) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            lastPose = poseStack.last();
            poseMatrix = lastPose.pose();
            normalMatrix = lastPose.normal();

            vertex(poseMatrix, normalMatrix, consumer, -8, -2, 0, 0.0F, 0.0F, 0, 1, 0, light, r, g, b, a);
            vertex(poseMatrix, normalMatrix, consumer, 8, -2, 0, 0.5F, 0.0F, 0, 1, 0, light, r, g, b, a);
            vertex(poseMatrix, normalMatrix, consumer, 8, 2, 0, 0.5F, 0.15625F, 0, 1, 0, light, r, g, b, a);
            vertex(poseMatrix, normalMatrix, consumer, -8, 2, 0, 0.0F, 0.15625F, 0, 1, 0, light, r, g, b, a);
        }

        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void vertex(Matrix4f pose, Matrix3f normal, VertexConsumer consumer,
                               int x, int y, int z, float u, float v,
                               int nx, int ny, int nz, int light,
                               int r, int g, int b, int a) {
        consumer.vertex(pose, (float) x, (float) y, (float) z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, (float) nx, (float) ny, (float) nz)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(WindArrowEntity entity) {
        return TEXTURE;
    }
}
