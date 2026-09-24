package io.redspace.ironspell_more.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.redspace.ironspell_more.IronSpellMore;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Shared ribbon geometry for the vanilla and Epic Fight render paths.
 *
 * <p>Caster methods expect a vanilla-oriented model-part transform. Binding
 * methods expect the entity-local root with positive Y pointing upward.</p>
 */
public final class GildedHareRibbonGeometry {
    public static final ResourceLocation TEXTURE_RIBBON =
            IronSpellMore.id("textures/entity/gilded_hare/ribbon.png");

    private GildedHareRibbonGeometry() {
    }

    public static void renderRabbitEars(PoseStack poseStack, VertexConsumer consumer, float ageInTicks) {
        poseStack.pushPose();
        poseStack.scale(0.0625F, 0.0625F, 0.0625F);

        float sway = (float) Math.sin(ageInTicks * 0.12F) * 2.0F;
        int r = 255, g = 230, b = 90, a = 235;

        poseStack.pushPose();
        poseStack.translate(-2.0F, -8.0F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-12.0F + sway));
        poseStack.mulPose(Axis.XP.rotationDegrees(-6.0F));
        renderEarRibbon(poseStack, consumer, r, g, b, a);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(2.0F, -8.0F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(12.0F - sway));
        poseStack.mulPose(Axis.XP.rotationDegrees(-6.0F));
        renderEarRibbon(poseStack, consumer, r, g, b, a);
        poseStack.popPose();

        poseStack.popPose();
    }

    public static void renderLegRibbons(PoseStack poseStack, VertexConsumer consumer,
                                        boolean isLeft, float ageInTicks) {
        poseStack.pushPose();
        poseStack.scale(0.0625F, 0.0625F, 0.0625F);

        int r = 255, g = 220, b = 70, a = 230;

        // Ankle & foot bands at the lower leg (y = 9.2 to 11.75)
        for (int i = 0; i < 3; i++) {
            poseStack.pushPose();
            float yPos = 9.2F + i * 0.9F;
            poseStack.translate(0.0F, yPos, 0.0F);
            renderRibbonBand(poseStack, consumer, 2.15F, 2.15F, 0.75F, r, g, b, a);
            poseStack.popPose();
        }

        // Ribbon bow & fluttering tail positioned at outer ankle (y = 10.2)
        poseStack.pushPose();
        float xOffset = isLeft ? -2.25F : 2.25F;
        poseStack.translate(xOffset, 10.2F, 0.0F);
        if (!isLeft) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        }

        renderRibbonBand(poseStack, consumer, 0.55F, 0.55F, 0.7F, r, g, b, a);

        poseStack.pushPose();
        poseStack.translate(-0.25F, -1.0F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(25.0F));
        renderBowLoop(poseStack, consumer, r, g, b, a);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(-0.25F, 1.0F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-25.0F));
        renderBowLoop(poseStack, consumer, r, g, b, a);
        poseStack.popPose();

        float flutter = (float) Math.sin(ageInTicks * 0.2F) * 4.0F;
        poseStack.pushPose();
        poseStack.translate(-0.2F, 0.8F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-15.0F + flutter));
        drawQuad(poseStack.last().pose(), poseStack.last().normal(), consumer,
                -0.5F, 0, 0, 0.5F, 0, 0, 0.5F, 2.6F, 0, -0.5F, 2.6F, 0,
                0, 0, 1, 1, r, g, b, a);
        drawQuad(poseStack.last().pose(), poseStack.last().normal(), consumer,
                0.5F, 0, 0, -0.5F, 0, 0, -0.5F, 2.6F, 0, 0.5F, 2.6F, 0,
                0, 0, 1, 1, r, g, b, a);
        poseStack.popPose();

        poseStack.popPose();
        poseStack.popPose();
    }

    public static void renderBinding(PoseStack poseStack, VertexConsumer consumer,
                                     float width, float height, float ageInTicks, int amplifier) {
        if (amplifier >= 4) {
            renderFullCocoon(poseStack, consumer, width, height, ageInTicks);
        } else {
            int stripCount = Math.max(1, Math.min(4, amplifier + 1));
            renderPartialRibbons(poseStack, consumer, width, height, ageInTicks, stripCount);
        }
    }

    public static void renderBinding(PoseStack poseStack, VertexConsumer consumer,
                                     float width, float height, float ageInTicks, boolean fullCocoon) {
        renderBinding(poseStack, consumer, width, height, ageInTicks, fullCocoon ? 4 : 0);
    }

    private static void renderEarRibbon(PoseStack poseStack, VertexConsumer consumer,
                                        int r, int g, int b, int a) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f mat = pose.pose();
        Matrix3f norm = pose.normal();
        float w = 1.3F;
        float h = 12.0F;

        drawQuad(mat, norm, consumer, -w, -h, 0, w, -h, 0, w, 0, 0, -w, 0, 0,
                0, 0, 1, 1, r, g, b, a);
        drawQuad(mat, norm, consumer, w, -h, 0, -w, -h, 0, -w, 0, 0, w, 0, 0,
                0, 0, 1, 1, r, g, b, a);
    }

    private static void renderRibbonBand(PoseStack poseStack, VertexConsumer consumer,
                                         float halfW, float halfD, float height,
                                         int r, int g, int b, int a) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f mat = pose.pose();
        Matrix3f norm = pose.normal();

        drawQuad(mat, norm, consumer, -halfW, 0, halfD, halfW, 0, halfD,
                halfW, height, halfD, -halfW, height, halfD, 0, 0, 1, 1, r, g, b, a);
        drawQuad(mat, norm, consumer, halfW, 0, -halfD, -halfW, 0, -halfD,
                -halfW, height, -halfD, halfW, height, -halfD, 0, 0, 1, 1, r, g, b, a);
        drawQuad(mat, norm, consumer, -halfW, 0, -halfD, -halfW, 0, halfD,
                -halfW, height, halfD, -halfW, height, -halfD, 0, 0, 1, 1, r, g, b, a);
        drawQuad(mat, norm, consumer, halfW, 0, halfD, halfW, 0, -halfD,
                halfW, height, -halfD, halfW, height, halfD, 0, 0, 1, 1, r, g, b, a);
    }

    private static void renderBowLoop(PoseStack poseStack, VertexConsumer consumer,
                                      int r, int g, int b, int a) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f mat = pose.pose();
        Matrix3f norm = pose.normal();
        drawQuad(mat, norm, consumer, -1.5F, -0.7F, 0, 0.5F, -0.7F, 0,
                0.5F, 0.7F, 0, -1.5F, 0.7F, 0, 0, 0, 1, 1, r, g, b, a);
        drawQuad(mat, norm, consumer, 0.5F, -0.7F, 0, -1.5F, -0.7F, 0,
                -1.5F, 0.7F, 0, 0.5F, 0.7F, 0, 0, 0, 1, 1, r, g, b, a);
    }

    private static void renderPartialRibbons(PoseStack poseStack, VertexConsumer consumer,
                                             float width, float height, float ageInTicks, int stripCount) {
        float radius = (width * 0.55F) + 0.10F;
        int segments = 16;
        float bandHeight = Math.max(0.12F, height * 0.08F);
        int r = 255, g = 225, b = 90, a = 210;

        // Render progressive ribbon strips wrapping around target (1 to 4 strips)
        int maxSlots = 4;
        for (int i = 0; i < stripCount; i++) {
            poseStack.pushPose();
            float progress = (i + 0.5F) / (float) maxSlots;
            float y = 0.12F + progress * (height * 0.72F);
            poseStack.translate(0, y, 0);
            float rot = ageInTicks * 1.5F + i * 55.0F;
            poseStack.mulPose(Axis.YP.rotationDegrees(rot));
            float tilt = i % 2 == 0 ? 12.0F : -12.0F;
            poseStack.mulPose(Axis.XP.rotationDegrees(tilt));
            renderRibbonCylinder(poseStack, consumer, radius, bandHeight, segments, r, g, b, a);
            poseStack.popPose();
        }
    }

    private static void renderFullCocoon(PoseStack poseStack, VertexConsumer consumer,
                                         float width, float height, float ageInTicks) {
        float radius = (width * 0.60F) + 0.15F;
        int layers = Math.max(6, (int) (height * 3.5F));
        int segments = 20;
        float layerHeight = (height * 0.95F) / (float) layers;
        int r = 255, g = 215, b = 60, a = 245;

        for (int i = 0; i < layers; i++) {
            poseStack.pushPose();
            float progress = (float) i / (float) layers;
            float y = 0.05F + progress * (height * 0.95F);
            poseStack.translate(0, y, 0);
            float layerRadius = radius * (0.85F + 0.25F * (float) Math.sin(progress * Math.PI));
            float rot = (i * 28.0F) + (float) Math.sin(ageInTicks * 0.1F + i) * 3.0F;
            poseStack.mulPose(Axis.YP.rotationDegrees(rot));
            renderRibbonCylinder(poseStack, consumer, layerRadius, layerHeight * 1.25F,
                    segments, r, g, b, a);
            poseStack.popPose();
        }

        poseStack.pushPose();
        poseStack.translate(0, height + 0.05F, 0);
        float earSway = (float) Math.sin(ageInTicks * 0.15F) * 3.0F;
        float earHeight = Math.max(0.6F, height * 0.45F);
        float earWidth = Math.max(0.12F, width * 0.15F);

        poseStack.pushPose();
        poseStack.translate(-radius * 0.35F, 0, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-15.0F + earSway));
        poseStack.mulPose(Axis.XP.rotationDegrees(-8.0F));
        renderCocoonEar(poseStack, consumer, earWidth, earHeight, r, g, b, a);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(radius * 0.35F, 0, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(15.0F - earSway));
        poseStack.mulPose(Axis.XP.rotationDegrees(-8.0F));
        renderCocoonEar(poseStack, consumer, earWidth, earHeight, r, g, b, a);
        poseStack.popPose();

        poseStack.popPose();
    }

    private static void renderRibbonCylinder(PoseStack poseStack, VertexConsumer consumer,
                                             float radius, float height, int segments,
                                             int r, int g, int b, int a) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f mat = pose.pose();
        Matrix3f norm = pose.normal();
        double step = (2.0D * Math.PI) / segments;

        for (int i = 0; i < segments; i++) {
            double a1 = i * step;
            double a2 = (i + 1) * step;
            float x1 = (float) (Math.cos(a1) * radius);
            float z1 = (float) (Math.sin(a1) * radius);
            float x2 = (float) (Math.cos(a2) * radius);
            float z2 = (float) (Math.sin(a2) * radius);
            float u1 = (float) i / segments;
            float u2 = (float) (i + 1) / segments;

            drawQuad(mat, norm, consumer, x1, 0, z1, x2, 0, z2,
                    x2, height, z2, x1, height, z1, u1, 0, u2, 1, r, g, b, a);
            drawQuad(mat, norm, consumer, x2, 0, z2, x1, 0, z1,
                    x1, height, z1, x2, height, z2, u2, 0, u1, 1, r, g, b, a);
        }
    }

    private static void renderCocoonEar(PoseStack poseStack, VertexConsumer consumer, float w, float h,
                                        int r, int g, int b, int a) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f mat = pose.pose();
        Matrix3f norm = pose.normal();
        drawQuad(mat, norm, consumer, -w, 0, 0, w, 0, 0, w, h, 0, -w, h, 0,
                0, 0, 1, 1, r, g, b, a);
        drawQuad(mat, norm, consumer, w, 0, 0, -w, 0, 0, -w, h, 0, w, h, 0,
                0, 0, 1, 1, r, g, b, a);
    }

    private static void drawQuad(Matrix4f mat, Matrix3f norm, VertexConsumer consumer,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float x4, float y4, float z4,
                                 float u1, float v1, float u2, float v2,
                                 int r, int g, int b, int a) {
        consumer.vertex(mat, x1, y1, z1).color(r, g, b, a).uv(u1, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(norm, 0, 1, 0).endVertex();
        consumer.vertex(mat, x2, y2, z2).color(r, g, b, a).uv(u2, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(norm, 0, 1, 0).endVertex();
        consumer.vertex(mat, x3, y3, z3).color(r, g, b, a).uv(u2, v2)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(norm, 0, 1, 0).endVertex();
        consumer.vertex(mat, x4, y4, z4).color(r, g, b, a).uv(u1, v2)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(norm, 0, 1, 0).endVertex();
    }
}
