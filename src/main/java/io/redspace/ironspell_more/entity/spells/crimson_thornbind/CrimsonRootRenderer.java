package io.redspace.ironspell_more.entity.spells.crimson_thornbind;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironsspellbooks.render.GeoLivingEntityRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class CrimsonRootRenderer extends GeoLivingEntityRenderer<CrimsonRootEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(IronSpellMore.MODID, "textures/entity/crimson_root.png");
    private static final RenderType SHEET_RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE);

    public CrimsonRootRenderer(EntityRendererProvider.Context context) {
        super(context, new CrimsonRootModel());
    }

    @Override
    public void render(CrimsonRootEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (entity.getWarmup() > 0) {
            return;
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        if (entity.isOrigin()) {
            renderOriginSheet(entity, partialTick, poseStack, bufferSource, packedLight);
        }
    }

    private void renderOriginSheet(CrimsonRootEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        float yaw = entity.getPathYaw();
        float pitch = entity.getPathPitch();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));

        float age = entity.tickCount + partialTick;
        float unfold = Mth.clamp(age / 4.0F, 0.0F, 1.0F);
        unfold = 1.0F - (1.0F - unfold) * (1.0F - unfold) * (1.0F - unfold);
        float pulse = 1.0F + 0.03F * Mth.sin(age * 0.16F);
        float baseRadius = 1.5F * unfold * pulse * (entity.getBaseScale() / CrimsonRootEntity.DEFAULT_PATH_SCALE);

        VertexConsumer consumer = bufferSource.getBuffer(SHEET_RENDER_TYPE);
        PoseStack.Pose lastPose = poseStack.last();
        Matrix4f poseMatrix = lastPose.pose();
        Matrix3f normalMatrix = lastPose.normal();

        // Texture UV bounds for root artwork in crimson_root.png (32x32)
        // Opaque pixel bounds: X=2..14, Y=12..31
        float u0 = 2.0F / 32.0F;
        float u1 = 15.0F / 32.0F;
        float vTip = 12.0F / 32.0F;
        float vBase = 31.5F / 32.0F;

        int alpha = (int) (255 * unfold);
        int light = LightTexture.FULL_BRIGHT;

        // Layer 1: 8 Primary radial root petals fanning out in 360 degrees
        int petals = 8;
        float angleStep = (float) (2.0 * Math.PI / petals);
        float rIn = 0.02F * baseRadius;
        float rOut1 = 1.0F * baseRadius;
        float wIn = 0.12F * baseRadius;
        float wOut1 = 0.32F * baseRadius;

        for (int i = 0; i < petals; i++) {
            float phi = i * angleStep;
            renderPetalQuad(consumer, poseMatrix, normalMatrix, phi, rIn, rOut1, wIn, wOut1, 0.0F,
                    u0, u1, vTip, vBase, 255, 255, 255, alpha, light);
        }

        // Layer 2: 8 Secondary interleaved root petals rotated by angleStep / 2
        float rOut2 = 0.82F * baseRadius;
        float wOut2 = 0.26F * baseRadius;
        float offsetPhi = angleStep * 0.5F;
        for (int i = 0; i < petals; i++) {
            float phi = i * angleStep + offsetPhi;
            renderPetalQuad(consumer, poseMatrix, normalMatrix, phi, rIn, rOut2, wIn * 0.85F, wOut2, 0.002F,
                    u0, u1, vTip, vBase, 240, 220, 225, alpha, light);
        }

        poseStack.popPose();
    }

    private void renderPetalQuad(VertexConsumer consumer, Matrix4f pose, Matrix3f normal,
                                float phi, float rIn, float rOut, float wIn, float wOut, float zOffset,
                                float u0, float u1, float vTip, float vBase,
                                int r, int g, int b, int a, int light) {
        float cos = Mth.cos(phi);
        float sin = Mth.sin(phi);
        float rx = cos;
        float ry = sin;
        float tx = -sin;
        float ty = cos;

        // Corner 1: Base Left
        float x1 = rx * rIn - tx * wIn;
        float y1 = ry * rIn - ty * wIn;
        // Corner 2: Base Right
        float x2 = rx * rIn + tx * wIn;
        float y2 = ry * rIn + ty * wIn;
        // Corner 3: Tip Right
        float x3 = rx * rOut + tx * wOut;
        float y3 = ry * rOut + ty * wOut;
        // Corner 4: Tip Left
        float x4 = rx * rOut - tx * wOut;
        float y4 = ry * rOut - ty * wOut;

        // Front Face (Normal +Z)
        consumer.vertex(pose, x1, y1, zOffset).color(r, g, b, a).uv(u0, vBase)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(pose, x2, y2, zOffset).color(r, g, b, a).uv(u1, vBase)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(pose, x3, y3, zOffset).color(r, g, b, a).uv(u1, vTip)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(pose, x4, y4, zOffset).color(r, g, b, a).uv(u0, vTip)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0.0F, 0.0F, 1.0F).endVertex();

        // Back Face (Normal -Z, reverse winding for double-sided illumination)
        consumer.vertex(pose, x4, y4, zOffset).color(r, g, b, a).uv(u0, vTip)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0.0F, 0.0F, -1.0F).endVertex();
        consumer.vertex(pose, x3, y3, zOffset).color(r, g, b, a).uv(u1, vTip)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0.0F, 0.0F, -1.0F).endVertex();
        consumer.vertex(pose, x2, y2, zOffset).color(r, g, b, a).uv(u1, vBase)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0.0F, 0.0F, -1.0F).endVertex();
        consumer.vertex(pose, x1, y1, zOffset).color(r, g, b, a).uv(u0, vBase)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0.0F, 0.0F, -1.0F).endVertex();
    }

    @Override
    protected void applyRotations(CrimsonRootEntity animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
        if (animatable.isPathMode()) {
            float yaw = animatable.getPathYaw();
            float pitch = animatable.getPathPitch();
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(-pitch - 90.0F));

            float scale = animatable.getBaseScale();
            poseStack.scale(scale * 0.65F, scale * 0.95F, scale * 0.65F);
            return;
        }

        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick);
    }

    @Override
    public void preRender(PoseStack poseStack, CrimsonRootEntity animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float r, float g, float b, float a) {
        float scale = animatable.getBaseScale();
        if (!animatable.isPathMode()) {
            var rooted = animatable.getFirstPassenger();
            if (rooted != null) {
                float passengerScale = rooted.getBbWidth() / 0.6F;
                if (passengerScale > 1.0F) {
                    scale *= passengerScale;
                }
            }
            poseStack.scale(scale, scale, scale);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, r, g, b, a);
    }
}
