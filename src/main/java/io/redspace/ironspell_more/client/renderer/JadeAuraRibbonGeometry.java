package io.redspace.ironspell_more.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironspell_more.client.particle.JadeAuraVfx;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Entity-local, full-bright electric lightning ribbon geometry for the sustained Jade Aura effect.
 */
public final class JadeAuraRibbonGeometry {
    private static final float TWO_PI = (float) (Math.PI * 2.0D);

    private JadeAuraRibbonGeometry() {
    }

    public static void render(PoseStack poseStack, VertexConsumer consumer,
                              float entityWidth, float entityHeight, float ageInTicks,
                              int entityId, boolean reducedDetail) {
        float height = Math.max(0.9F, entityHeight);
        float radius = Math.max(0.55F, entityWidth * 0.72F) + 0.08F;
        float phase = ageInTicks * 0.055F + entityId * 0.7548777F;
        float pulse = 1.0F + 0.04F * (float) Math.sin(ageInTicks * 0.14F + entityId);

        int strandCount = reducedDetail ? 2 : 3;
        int strandSegments = reducedDetail ? 24 : 36;
        int crackleTick = (int) (ageInTicks * 1.6F);

        for (int strand = 0; strand < strandCount; strand++) {
            float strandPhase = phase + strand * (TWO_PI / 3.0F);
            float direction = strand == 1 ? -1.0F : 1.0F;
            float turns = strand == 2 ? 2.05F : 1.75F;
            float strandRadius = radius * pulse * (1.0F + strand * 0.06F);

            int[] coreColor = switch (strand) {
                case 0 -> JadeAuraVfx.COLOR_ELECTRIC_CORE_RGB;
                case 1 -> JadeAuraVfx.COLOR_LUMINOUS_VERDIGRIS_RGB;
                default -> JadeAuraVfx.COLOR_RADIANT_JADE_RGB;
            };
            int[] glowColor = JadeAuraVfx.COLOR_ELECTRIC_GLOW_RGB;

            // Wide radiant jade halo layer
            renderLightningStrand(poseStack, consumer, height, strandRadius, strandPhase,
                    direction, turns, strandSegments, 0.075F, glowColor, 140,
                    strand, crackleTick, entityId);
            // Intense luminous electric core
            renderLightningStrand(poseStack, consumer, height, strandRadius, strandPhase,
                    direction, turns, strandSegments, 0.024F, coreColor, 255,
                    strand, crackleTick, entityId);
        }

        int ringSegments = reducedDetail ? 20 : 28;
        renderOrbitRing(poseStack, consumer, radius * 1.06F, height * 0.20F,
                phase * -1.7F, ageInTicks, ringSegments,
                JadeAuraVfx.COLOR_ELECTRIC_GLOW_RGB,
                JadeAuraVfx.COLOR_LUMINOUS_VERDIGRIS_RGB);

        if (!reducedDetail) {
            renderOrbitRing(poseStack, consumer, radius * 1.13F, height * 0.68F,
                    phase * 1.35F + 1.9F, ageInTicks + 17.0F, ringSegments,
                    JadeAuraVfx.COLOR_ELECTRIC_GLOW_RGB,
                    JadeAuraVfx.COLOR_RADIANT_JADE_RGB);
        }
    }

    private static void renderLightningStrand(PoseStack poseStack, VertexConsumer consumer,
                                              float height, float radius, float phase,
                                              float direction, float turns, int segments,
                                              float halfWidth, int[] color, int baseAlpha,
                                              int strand, int crackleTick, int entityId) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        float yMin = Math.max(0.06F, height * 0.03F);
        float ySpan = height * 0.94F;

        // Precompute jagged lightning vertices along the strand
        float[] px = new float[segments + 1];
        float[] py = new float[segments + 1];
        float[] pz = new float[segments + 1];

        for (int i = 0; i <= segments; i++) {
            float t = (float) i / segments;
            float baseAngle = phase + direction * t * turns * TWO_PI;
            float baseRadius = radius * (1.0F + 0.04F * (float) Math.sin(t * TWO_PI * 2.0F + phase * 1.8F));
            float baseY = yMin + t * ySpan;

            // Envelope limits jitter at ends so ribbons cleanly terminate
            float envelope = (float) Math.sin(Math.PI * t);
            float crackleA = hashNoise(i, strand, crackleTick, entityId);
            float crackleB = hashNoise(i + 31, strand + 5, crackleTick, entityId);
            float crackleC = hashNoise(i + 67, strand + 11, crackleTick, entityId);

            // Sharp lightning zig-zag displacement (angular, radial, vertical)
            float jaggedAngle = crackleA * 0.18F * envelope;
            float jaggedRadius = crackleB * 0.13F * baseRadius * envelope;
            float jaggedY = crackleC * 0.055F * ySpan * envelope;

            float finalAngle = baseAngle + jaggedAngle;
            float finalRadius = baseRadius + jaggedRadius;
            px[i] = (float) Math.cos(finalAngle) * finalRadius;
            py[i] = baseY + jaggedY;
            pz[i] = (float) Math.sin(finalAngle) * finalRadius;
        }

        // Connect consecutive jagged vertices with double-sided ribbon quads
        for (int i = 0; i < segments; i++) {
            float t0 = (float) i / segments;
            float t1 = (float) (i + 1) / segments;

            float x0 = px[i], y0 = py[i], z0 = pz[i];
            float x1 = px[i + 1], y1 = py[i + 1], z1 = pz[i + 1];

            // Tangent along segment
            float tx = x1 - x0;
            float ty = y1 - y0;
            float tz = z1 - z0;

            // Outward radial normal
            float ox = (x0 + x1) * 0.5F;
            float oz = (z0 + z1) * 0.5F;

            // Cross product: tangent x outward gives width vector facing the viewer
            float wx = ty * oz;
            float wy = tz * ox - tx * oz;
            float wz = -ty * ox;

            float wScale = inverseLength(wx, wy, wz) * halfWidth;
            wx *= wScale;
            wy *= wScale;
            wz *= wScale;

            int alpha0 = taperedAlpha(t0, baseAlpha);
            int alpha1 = taperedAlpha(t1, baseAlpha);

            drawDoubleSidedQuad(matrix, normal, consumer,
                    x0 - wx, y0 - wy, z0 - wz,
                    x1 - wx, y1 - wy, z1 - wz,
                    x1 + wx, y1 + wy, z1 + wz,
                    x0 + wx, y0 + wy, z0 + wz,
                    t0, t1, color, alpha0, alpha1);
        }
    }

    private static float hashNoise(int i, int strand, int time, int seed) {
        int h = i * 374761393 + strand * 668265263 + time * 314159265 + seed * (int) 2246822519L;
        h = (h ^ (h >> 13)) * 1274126177;
        return ((h ^ (h >> 16)) & 0xFFFF) / 32767.5F - 1.0F;
    }

    private static void renderOrbitRing(PoseStack poseStack, VertexConsumer consumer,
                                        float radius, float y, float phase, float ageInTicks,
                                        int segments, int[] glowColor, int[] coreColor) {
        renderRingLayer(poseStack, consumer, radius, y, phase, ageInTicks,
                segments, 0.055F, glowColor, 120);
        renderRingLayer(poseStack, consumer, radius, y + 0.003F, phase, ageInTicks,
                segments, 0.018F, coreColor, 230);
    }

    private static void renderRingLayer(PoseStack poseStack, VertexConsumer consumer,
                                        float radius, float y, float phase, float ageInTicks,
                                        int segments, float halfWidth, int[] color, int alpha) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        for (int i = 0; i < segments; i++) {
            float t0 = (float) i / segments;
            float t1 = (float) (i + 1) / segments;
            float angle0 = phase + t0 * TWO_PI;
            float angle1 = phase + t1 * TWO_PI;

            // Energetic electric ripple on the orbit rings
            float wave0 = (float) Math.sin(angle0 * 6.0F + ageInTicks * 0.25F) * 0.022F;
            float wave1 = (float) Math.sin(angle1 * 6.0F + ageInTicks * 0.25F) * 0.022F;

            float inner0 = radius - halfWidth;
            float outer0 = radius + halfWidth;
            float inner1 = radius - halfWidth;
            float outer1 = radius + halfWidth;

            drawDoubleSidedQuad(matrix, normal, consumer,
                    (float) Math.cos(angle0) * inner0, y + wave0, (float) Math.sin(angle0) * inner0,
                    (float) Math.cos(angle1) * inner1, y + wave1, (float) Math.sin(angle1) * inner1,
                    (float) Math.cos(angle1) * outer1, y + wave1, (float) Math.sin(angle1) * outer1,
                    (float) Math.cos(angle0) * outer0, y + wave0, (float) Math.sin(angle0) * outer0,
                    t0, t1, color, alpha, alpha);
        }
    }

    private static int taperedAlpha(float progress, int baseAlpha) {
        // Broadened envelope so lightning arcs remain fully brilliant over 70%+ of length
        float taper = Mth.clamp((float) Math.sin(Math.PI * progress) * 2.2F, 0.0F, 1.0F);
        return Math.max(0, Math.min(255, Math.round(baseAlpha * taper)));
    }

    private static float inverseLength(float x, float y, float z) {
        float lengthSquared = x * x + y * y + z * z;
        return lengthSquared > 1.0E-6F ? (float) (1.0D / Math.sqrt(lengthSquared)) : 0.0F;
    }

    private static void drawDoubleSidedQuad(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer,
                                           float x1, float y1, float z1,
                                           float x2, float y2, float z2,
                                           float x3, float y3, float z3,
                                           float x4, float y4, float z4,
                                           float v0, float v1, int[] color, int alpha0, int alpha1) {
        // Front face
        vertex(matrix, normal, consumer, x1, y1, z1, 0.0F, v0, color, alpha0);
        vertex(matrix, normal, consumer, x2, y2, z2, 0.0F, v1, color, alpha1);
        vertex(matrix, normal, consumer, x3, y3, z3, 1.0F, v1, color, alpha1);
        vertex(matrix, normal, consumer, x4, y4, z4, 1.0F, v0, color, alpha0);

        // Back face
        vertex(matrix, normal, consumer, x4, y4, z4, 1.0F, v0, color, alpha0);
        vertex(matrix, normal, consumer, x3, y3, z3, 1.0F, v1, color, alpha1);
        vertex(matrix, normal, consumer, x2, y2, z2, 0.0F, v1, color, alpha1);
        vertex(matrix, normal, consumer, x1, y1, z1, 0.0F, v0, color, alpha0);
    }

    private static void vertex(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer,
                               float x, float y, float z, float u, float v,
                               int[] color, int alpha) {
        consumer.vertex(matrix, x, y, z)
                .color(color[0], color[1], color[2], alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
