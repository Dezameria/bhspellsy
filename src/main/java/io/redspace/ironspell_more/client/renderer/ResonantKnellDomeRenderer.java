package io.redspace.ironspell_more.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.redspace.ironspell_more.entity.spells.resonant_knell.ResonantKnellDomeAoe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.redspace.ironspell_more.client.renderer.ResonantKnellDomeVisuals.*;

public class ResonantKnellDomeRenderer extends EntityRenderer<ResonantKnellDomeAoe> {
    private static final ResourceLocation WHITE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("forge", "textures/white.png");
    private static final ResourceLocation TALISMAN_TEXTURE =
            new ResourceLocation("ironspell_more", "textures/entity/resonant_knell/talisman.png");

    private static final RenderStateShard.TransparencyStateShard ADDITIVE_ALPHA_TRANSPARENCY =
            new RenderStateShard.TransparencyStateShard("rk_dome_additive_alpha", () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            }, () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            });

    private static final RenderType RENDER_TYPE = buildDomeRenderType();
    private static final RenderType TALISMAN_RENDER_TYPE = RenderType.entityTranslucentEmissive(TALISMAN_TEXTURE);

    private static RenderType buildDomeRenderType() {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityTranslucentEmissiveShader))
                .setTextureState(new RenderStateShard.TextureStateShard(WHITE_TEXTURE, false, false))
                .setTransparencyState(ADDITIVE_ALPHA_TRANSPARENCY)
                .setCullState(new RenderStateShard.CullStateShard(false))
                .setLightmapState(new RenderStateShard.LightmapStateShard(false))
                .setOverlayState(new RenderStateShard.OverlayStateShard(true))
                .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, false))
                .createCompositeState(false);
        return RenderType.create("ironspell_more_resonant_knell_dome", DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS, 256, false, true, state);
    }

    private record MeshKey(double radius, int ringDensityPerQuarter, int segments, double lowerExtent) {}

    private record MeshData(float[] posX, float[] posY, float[] posZ,
                             float[] normX, float[] normY, float[] normZ,
                             int[] quadVertexIndices) {}

    private static final Map<MeshKey, MeshData> MESH_CACHE = new ConcurrentHashMap<>();

    private static MeshData meshFor(double radius, int ringDensityPerQuarter, int segments, double lowerExtent) {
        return MESH_CACHE.computeIfAbsent(new MeshKey(radius, ringDensityPerQuarter, segments, lowerExtent),
                key -> buildMesh(key.radius(), key.ringDensityPerQuarter(), key.segments(), key.lowerExtent()));
    }

    private static MeshData buildMesh(double radius, int ringDensityPerQuarter, int segments, double lowerExtent) {
        double phiMax = Math.acos(-lowerExtent);
        int latRings = Math.max(1, (int) Math.round(ringDensityPerQuarter * (phiMax / (Math.PI / 2.0))));

        int vertexCount = (latRings + 1) * segments;
        float[] posX = new float[vertexCount];
        float[] posY = new float[vertexCount];
        float[] posZ = new float[vertexCount];
        float[] normX = new float[vertexCount];
        float[] normY = new float[vertexCount];
        float[] normZ = new float[vertexCount];

        for (int row = 0; row <= latRings; row++) {
            double phi = (double) row / latRings * phiMax;
            double sinPhi = Math.sin(phi);
            double cosPhi = Math.cos(phi);
            for (int col = 0; col < segments; col++) {
                double theta = (double) col / segments * (2.0 * Math.PI);
                double nx = sinPhi * Math.cos(theta);
                double ny = cosPhi;
                double nz = sinPhi * Math.sin(theta);
                int i = row * segments + col;
                posX[i] = (float) (nx * radius);
                posY[i] = (float) (ny * radius);
                posZ[i] = (float) (nz * radius);
                normX[i] = (float) nx;
                normY[i] = (float) ny;
                normZ[i] = (float) nz;
            }
        }

        int quadCount = latRings * segments;
        int[] quadVertexIndices = new int[quadCount * 4];
        int q = 0;
        for (int row = 0; row < latRings; row++) {
            for (int col = 0; col < segments; col++) {
                int nextCol = (col + 1) % segments;
                int a = row * segments + col;
                int b = row * segments + nextCol;
                int c = (row + 1) * segments + nextCol;
                int d = (row + 1) * segments + col;
                quadVertexIndices[q++] = a;
                quadVertexIndices[q++] = b;
                quadVertexIndices[q++] = c;
                quadVertexIndices[q++] = d;
            }
        }
        return new MeshData(posX, posY, posZ, normX, normY, normZ, quadVertexIndices);
    }

    private static final float[] RING_COS = new float[SIGIL_SEGMENTS + 1];
    private static final float[] RING_SIN = new float[SIGIL_SEGMENTS + 1];
    static {
        for (int i = 0; i <= SIGIL_SEGMENTS; i++) {
            double angle = 2.0 * Math.PI * i / SIGIL_SEGMENTS;
            RING_COS[i] = (float) Math.cos(angle);
            RING_SIN[i] = (float) Math.sin(angle);
        }
    }

    public ResonantKnellDomeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ResonantKnellDomeAoe entity) {
        return WHITE_TEXTURE;
    }

    @Override
    public boolean shouldRender(ResonantKnellDomeAoe entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(ResonantKnellDomeAoe entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        Vec3 center = entity.position();
        Vec3 camPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        if (camPos.distanceToSqr(center) > MAX_RENDER_DISTANCE_SQ) {
            return;
        }

        float time = entity.tickCount + partialTicks;
        PoseStack.Pose pose = poseStack.last();
        Matrix4f positionMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        float camLocalX = (float) (camPos.x - center.x);
        float camLocalY = (float) (camPos.y - center.y);
        float camLocalZ = (float) (camPos.z - center.z);

        int state = entity.getDomeState();
        int seed = entity.getId();

        if (state == ResonantKnellDomeAoe.STATE_OPEN) {
            float openAge = Math.max(0.0f, time - entity.getDomeOpenStartTick());
            float openProgress = Mth.clamp(openAge / DOME_OPEN_TICKS, 0.0f, 1.0f);
            float openScale = Mth.lerp(easeOutBack(openProgress), DOME_OPEN_START_SCALE, 1.0f);
            float openYOffset = Mth.lerp(easeOut(openProgress), DOME_OPEN_START_Y, 0.0f);
            float fadeAlpha = fadeInAlpha(openAge);
            if (fadeAlpha > 0.0f) {
                // 1. Raise and unfold the shell, wisps, and talismans from beneath the caster.
                VertexConsumer domeConsumer = buffer.getBuffer(RENDER_TYPE);
                poseStack.pushPose();
                poseStack.translate(0.0, openYOffset, 0.0);
                PoseStack.Pose openingPose = poseStack.last();
                Matrix4f openingPosition = openingPose.pose();
                Matrix3f openingNormal = openingPose.normal();
                float openingCamY = camLocalY - openYOffset;

                renderShell(seed, time, openScale, fadeAlpha, camLocalX, openingCamY, camLocalZ,
                        openingPosition, openingNormal, domeConsumer);
                renderSpiritWisps(seed, time, openScale, -1.0f, fadeAlpha, camLocalX, openingCamY, camLocalZ,
                        openingPosition, openingNormal, domeConsumer);
                poseStack.popPose();

                // 2. Keep ground effects anchored beneath the caster while the dome rises.
                renderRing(BASE_RADIUS * openScale, SIGIL_LINE_WIDTH * openScale, SIGIL_Y,
                        CORE_R, CORE_G, CORE_B, SIGIL_ALPHA * fadeAlpha,
                        positionMatrix, normalMatrix, domeConsumer);
                renderConvergingGroundRings(time, fadeAlpha, positionMatrix, normalMatrix, domeConsumer);

                // 3. Raise the floating Taoist talismans with the shell.
                VertexConsumer talismanConsumer = buffer.getBuffer(TALISMAN_RENDER_TYPE);
                renderTalismans(seed, time, openScale, -1.0f, fadeAlpha,
                        openingPosition, openingNormal, talismanConsumer);
            }
        } else if (state == ResonantKnellDomeAoe.STATE_EXPLODING) {
            int shockwaveStart = entity.getShockwaveStartTick();
            float shockAge = Math.max(0.0f, time - shockwaveStart);
            if (shockAge < SHOCKWAVE_TICKS) {
                float p = shockAge / SHOCKWAVE_TICKS;
                float ease = easeOut(p);
                float targetRadius = entity.getShockwaveRadius();
                float expandScale = Mth.lerp(ease, 1.0f, targetRadius / BASE_RADIUS);
                float blastAlpha = (1.0f - p) * (1.0f - p);

                // 1. Expanding dome shell that blows outward
                VertexConsumer domeConsumer = buffer.getBuffer(RENDER_TYPE);
                renderShell(seed, time, expandScale, blastAlpha, camLocalX, camLocalY, camLocalZ,
                        positionMatrix, normalMatrix, domeConsumer);
                renderRing(BASE_RADIUS * expandScale, SIGIL_LINE_WIDTH * expandScale, SIGIL_Y,
                        CORE_R, CORE_G, CORE_B, SIGIL_ALPHA * blastAlpha,
                        positionMatrix, normalMatrix, domeConsumer);
                renderNuclearShockwave(shockAge, targetRadius, positionMatrix, normalMatrix, domeConsumer);
                renderSpiritWisps(seed, time, expandScale, p, blastAlpha, camLocalX, camLocalY, camLocalZ,
                        positionMatrix, normalMatrix, domeConsumer);

                // 2. Talismans blasting outward radially in the shockwave
                VertexConsumer talismanConsumer = buffer.getBuffer(TALISMAN_RENDER_TYPE);
                renderTalismans(seed, time, expandScale, p, blastAlpha, positionMatrix, normalMatrix, talismanConsumer);
            }
        }
    }

    private static void renderShell(int seed, float time, float scale, float fadeAlpha,
                                    float camLocalX, float camLocalY, float camLocalZ,
                                    Matrix4f positionMatrix, Matrix3f normalMatrix, VertexConsumer consumer) {
        MeshData mesh = meshFor(BASE_RADIUS, LAT_RINGS, SEGMENTS, LOWER_EXTENT);
        float insideFactor = insideFactor(camLocalX, camLocalY, camLocalZ);

        int[] indices = mesh.quadVertexIndices();
        float[] posX = mesh.posX();
        float[] posY = mesh.posY();
        float[] posZ = mesh.posZ();
        float[] normX = mesh.normX();
        float[] normY = mesh.normY();
        float[] normZ = mesh.normZ();

        for (int vi : indices) {
            float lx = posX[vi] * scale;
            float ly = posY[vi] * scale;
            float lz = posZ[vi] * scale;
            float nx = normX[vi];
            float ny = normY[vi];
            float nz = normZ[vi];

            float edgeTerm = fresnelEdgeTerm(lx, ly, lz, nx, ny, nz, camLocalX, camLocalY, camLocalZ);
            float rim = rimFactor(posY[vi]);
            float alpha = edgeTerm + INSIDE_ALPHA * insideFactor + RIM_ALPHA * rim;
            alpha = Mth.clamp(alpha, 0.0f, 1.0f) * fadeAlpha;

            // Gradient: Radiant gold transitioning to blaze red at edge
            float r = Mth.lerp(edgeTerm, COLOR_R, EDGE_R);
            float g = Mth.lerp(edgeTerm, COLOR_G, EDGE_G);
            float b = Mth.lerp(edgeTerm, COLOR_B, EDGE_B);

            consumer.vertex(positionMatrix, lx, ly, lz)
                    .color(r, g, b, alpha)
                    .uv(0.5f, 0.5f)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.FULL_BRIGHT)
                    .normal(normalMatrix, nx, ny, nz)
                    .endVertex();
        }

        // Plasma flame streaks
        renderPlasmaStreaks(seed, time, scale, fadeAlpha, positionMatrix, normalMatrix, consumer);
    }

    private static void renderTalismans(int seed, float time, float scale, float explodeProgress, float fadeAlpha,
                                        Matrix4f positionMatrix, Matrix3f normalMatrix, VertexConsumer consumer) {
        float alpha = TALISMAN_ALPHA * fadeAlpha;
        if (alpha <= 0.005f) return;

        boolean exploding = explodeProgress >= 0.0f;
        float radius = (BASE_RADIUS + TALISMAN_RADIUS_OFFSET) * scale;
        float halfW = TALISMAN_WIDTH * 0.5f;
        float halfH = TALISMAN_HEIGHT * 0.5f;

        for (int i = 0; i < TALISMAN_COUNT; i++) {
            // Distribute talismans across latitudes and longitudes
            float basePhi = Mth.lerp((float) i / TALISMAN_COUNT, 0.25f, 1.25f);
            float baseTheta = (i * 137.50776f + (hash(seed, i, 11, 0) - 0.5f) * 20.0f) * Mth.DEG_TO_RAD;
            float orbitSpeed = TALISMAN_ORBIT_SPEED_DEG * (hash(seed, i, 12, 0) < 0.5f ? 1.0f : -1.0f) * Mth.DEG_TO_RAD;
            float theta = baseTheta + time * orbitSpeed;
            float wobble = Mth.sin(time * 0.08f + i * 1.5f) * 0.08f;
            float phi = Mth.clamp(basePhi + wobble, 0.15f, (float) Math.PI * 0.48f);

            // Unit outward normal
            float sinPhi = Mth.sin(phi);
            float cosPhi = Mth.cos(phi);
            float nx = sinPhi * Mth.cos(theta);
            float ny = cosPhi;
            float nz = sinPhi * Mth.sin(theta);

            // Center position
            float cx = nx * radius;
            float cy = Math.max(0.1f, ny * radius);
            float cz = nz * radius;

            // Extra explosion scatter
            float roll = (hash(seed, i, 13, 0) - 0.5f) * 0.4f + Mth.sin(time * 0.05f + i) * 0.12f;
            if (exploding) {
                float scatter = hash(seed, i, 14, 0) * 4.0f * explodeProgress;
                cx += nx * scatter;
                cz += nz * scatter;
                cy += (hash(seed, i, 15, 0) * 2.5f) * explodeProgress;
                roll += explodeProgress * (hash(seed, i, 16, 0) - 0.5f) * 12.0f;
            }

            // Tangent & Bitangent basis for the quad
            // Tangent along orbit: (-sin theta, 0, cos theta)
            float tx = -Mth.sin(theta);
            float ty = 0.0f;
            float tz = Mth.cos(theta);

            // Bitangent: n x t
            float bx = ny * tz - nz * ty;
            float by = nz * tx - nx * tz;
            float bz = nx * ty - ny * tx;

            // Apply slight roll rotation in tangent plane
            float cosR = Mth.cos(roll);
            float sinR = Mth.sin(roll);
            float ux = tx * cosR - bx * sinR;
            float uy = ty * cosR - by * sinR;
            float uz = tz * cosR - bz * sinR;

            float vx = tx * sinR + bx * cosR;
            float vy = ty * sinR + by * cosR;
            float vz = tz * sinR + bz * cosR;

            // 4 Quad Corners:
            // Corner 0: -u * halfW, +v * halfH (Top-Left)
            // Corner 1: -u * halfW, -v * halfH (Bottom-Left)
            // Corner 2: +u * halfW, -v * halfH (Bottom-Right)
            // Corner 3: +u * halfW, +v * halfH (Top-Right)
            float x0 = cx - ux * halfW + vx * halfH;
            float y0 = cy - uy * halfW + vy * halfH;
            float z0 = cz - uz * halfW + vz * halfH;

            float x1 = cx - ux * halfW - vx * halfH;
            float y1 = cy - uy * halfW - vy * halfH;
            float z1 = cz - uz * halfW - vz * halfH;

            float x2 = cx + ux * halfW - vx * halfH;
            float y2 = cy + uy * halfW - vy * halfH;
            float z2 = cz + uz * halfW - vz * halfH;

            float x3 = cx + ux * halfW + vx * halfH;
            float y3 = cy + uy * halfW + vy * halfH;
            float z3 = cz + uz * halfW + vz * halfH;

            consumer.vertex(positionMatrix, x0, y0, z0).color(1.0f, 1.0f, 1.0f, alpha)
                    .uv(0.0f, 0.0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                    .normal(normalMatrix, nx, ny, nz).endVertex();

            consumer.vertex(positionMatrix, x1, y1, z1).color(1.0f, 1.0f, 1.0f, alpha)
                    .uv(0.0f, 1.0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                    .normal(normalMatrix, nx, ny, nz).endVertex();

            consumer.vertex(positionMatrix, x2, y2, z2).color(1.0f, 1.0f, 1.0f, alpha)
                    .uv(1.0f, 1.0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                    .normal(normalMatrix, nx, ny, nz).endVertex();

            consumer.vertex(positionMatrix, x3, y3, z3).color(1.0f, 1.0f, 1.0f, alpha)
                    .uv(1.0f, 0.0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                    .normal(normalMatrix, nx, ny, nz).endVertex();
        }
    }

    private static void renderSpiritWisps(int seed, float time, float scale, float explodeProgress, float fadeAlpha,
                                          float camX, float camY, float camZ,
                                          Matrix4f positionMatrix, Matrix3f normalMatrix, VertexConsumer consumer) {
        float alpha = SPIRIT_ALPHA * fadeAlpha;
        if (alpha <= 0.005f) return;

        boolean exploding = explodeProgress >= 0.0f;
        float radius = (BASE_RADIUS + 0.35f) * scale;
        float halfSize = SPIRIT_WISP_SIZE * 0.5f;

        for (int i = 0; i < SPIRIT_WISP_COUNT; i++) {
            float basePhi = Mth.lerp((float) i / SPIRIT_WISP_COUNT, 0.3f, 1.15f);
            float baseTheta = (i * 157.0f + hash(seed, i, 21, 0) * 30.0f) * Mth.DEG_TO_RAD;
            float orbitSpeed = 0.45f * (i % 2 == 0 ? 1.0f : -1.0f) * Mth.DEG_TO_RAD;
            float theta = baseTheta + time * orbitSpeed;
            float bob = Mth.sin(time * 0.12f + i * 2.0f) * 0.25f;
            float phi = Mth.clamp(basePhi + bob * 0.05f, 0.15f, (float) Math.PI * 0.45f);

            float nx = Mth.sin(phi) * Mth.cos(theta);
            float ny = Mth.cos(phi);
            float nz = Mth.sin(phi) * Mth.sin(theta);

            float cx = nx * radius;
            float cy = Math.max(0.2f, ny * radius + bob);
            float cz = nz * radius;

            if (exploding) {
                float scatter = (3.0f + hash(seed, i, 22, 0) * 5.0f) * explodeProgress;
                cx += nx * scatter;
                cy += ny * scatter + explodeProgress * 1.5f;
                cz += nz * scatter;
            }

            // Camera facing billboard
            float dx = camX - cx;
            float dy = camY - cy;
            float dz = camZ - cz;
            float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1.0e-4f) continue;
            dx /= len; dy /= len; dz /= len;

            // Perpendicular basis
            float rx = -dz;
            float ry = 0.0f;
            float rz = dx;
            float rLen = Mth.sqrt(rx * rx + rz * rz);
            if (rLen < 1.0e-4f) { rx = 1.0f; rz = 0.0f; } else { rx /= rLen; rz /= rLen; }

            float ux = dy * rz;
            float uy = dz * rx - dx * rz;
            float uz = -dy * rx;

            float pulse = 1.0f + 0.2f * Mth.sin(time * 0.2f + i);
            float hs = halfSize * pulse;

            consumer.vertex(positionMatrix, cx - rx * hs + ux * hs, cy + uy * hs, cz - rz * hs + uz * hs)
                    .color(SPIRIT_R, SPIRIT_G, SPIRIT_B, alpha).uv(0.5f, 0.5f).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.FULL_BRIGHT).normal(normalMatrix, dx, dy, dz).endVertex();
            consumer.vertex(positionMatrix, cx - rx * hs - ux * hs, cy - uy * hs, cz - rz * hs - uz * hs)
                    .color(SPIRIT_R, SPIRIT_G, SPIRIT_B, alpha).uv(0.5f, 0.5f).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.FULL_BRIGHT).normal(normalMatrix, dx, dy, dz).endVertex();
            consumer.vertex(positionMatrix, cx + rx * hs - ux * hs, cy - uy * hs, cz + rz * hs - uz * hs)
                    .color(SPIRIT_R, SPIRIT_G, SPIRIT_B, alpha).uv(0.5f, 0.5f).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.FULL_BRIGHT).normal(normalMatrix, dx, dy, dz).endVertex();
            consumer.vertex(positionMatrix, cx + rx * hs + ux * hs, cy + uy * hs, cz + rz * hs + uz * hs)
                    .color(SPIRIT_R, SPIRIT_G, SPIRIT_B, alpha).uv(0.5f, 0.5f).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.FULL_BRIGHT).normal(normalMatrix, dx, dy, dz).endVertex();
        }
    }

    private static void renderPlasmaStreaks(int seed, float time, float scale, float alpha,
                                           Matrix4f position, Matrix3f normal, VertexConsumer consumer) {
        if (alpha <= 0.0f) return;
        float radius = (BASE_RADIUS + STREAK_SURFACE_OFFSET) * scale;
        for (int i = 0; i < STREAK_COUNT; i++) {
            float phaseOffset = hash(seed, i, 47, 0);
            float local = time / STREAK_CYCLE_TICKS + phaseOffset;
            float lifePhase = local - Mth.floor(local);
            float life = 0.55f + 0.45f * Mth.sin(lifePhase * Mth.PI);
            float a = alpha * life * STREAK_ALPHA;
            if (a <= 0.005f) continue;

            float startPhi = Mth.lerp(hash(seed, i, 48, 0),
                    STREAK_MIN_START_PHI_DEG, STREAK_MAX_START_PHI_DEG) * Mth.DEG_TO_RAD;
            float endPhi = Mth.lerp(hash(seed, i, 49, 0),
                    STREAK_MIN_END_PHI_DEG, STREAK_MAX_END_PHI_DEG) * Mth.DEG_TO_RAD;
            float direction = hash(seed, i, 50, 0) < 0.5f ? -1.0f : 1.0f;
            float twist = Mth.lerp(hash(seed, i, 51, 0),
                    STREAK_MIN_TWIST_DEG, STREAK_MAX_TWIST_DEG) * Mth.DEG_TO_RAD * direction;
            float startTheta = (float) i / STREAK_COUNT * Mth.TWO_PI
                    + (hash(seed, i, 52, 0) - 0.5f) * 0.7f
                    + time * STREAK_ORBIT_SPEED_DEG * Mth.DEG_TO_RAD * direction;
            float wavePhase = hash(seed, i, 53, 0) * Mth.TWO_PI;
            float flowPhase = time / STREAK_FLOW_CYCLE_TICKS + phaseOffset;

            surfaceFlowStreak(radius, startPhi, endPhi, startTheta, twist, STREAK_WIDTH / radius,
                    wavePhase, flowPhase, STREAK_SEGMENTS,
                    STREAK_R, STREAK_G, STREAK_B, a, position, normal, consumer);
        }
    }

    private static void renderConvergingGroundRings(float time, float fadeAlpha,
                                                     Matrix4f position, Matrix3f normal, VertexConsumer consumer) {
        for (int i = 0; i < CONVERGING_RING_COUNT; i++) {
            float local = time / CONVERGING_RING_CYCLE_TICKS + (float) i / CONVERGING_RING_COUNT;
            float phase = local - Mth.floor(local);
            float smooth = phase * phase * (3.0f - 2.0f * phase);
            float radius = Mth.lerp(smooth, CONVERGING_RING_MAX_RADIUS, CONVERGING_RING_MIN_RADIUS);
            float life = Mth.sin(phase * Mth.PI);
            float alpha = CONVERGING_RING_ALPHA * fadeAlpha * life;
            if (alpha <= 0.005f) continue;

            float width = CONVERGING_RING_WIDTH * (0.8f + life * 0.4f);
            float y = SIGIL_Y + 0.012f + i * 0.001f;
            renderRing(radius, width, y, CORE_R, CORE_G, CORE_B, alpha, position, normal, consumer);
        }
    }

    private static void renderExpandingGroundRings(float progress, float targetRadius,
                                                    Matrix4f position, Matrix3f normal, VertexConsumer consumer) {
        for (int i = 0; i < EXPANDING_RING_COUNT; i++) {
            float delay = i * EXPANDING_RING_STAGGER;
            float localProgress = (progress - delay) / (1.0f - delay);
            if (localProgress <= 0.0f || localProgress >= 1.0f) continue;

            float eased = easeOut(localProgress);
            float radius = Mth.lerp(eased, EXPANDING_RING_START_RADIUS, targetRadius);
            float fade = (1.0f - localProgress) * (1.0f - localProgress);
            float alpha = EXPANDING_RING_ALPHA * fade;
            float width = EXPANDING_RING_WIDTH * (1.0f + eased * 0.65f);
            float y = SIGIL_Y + 0.035f + i * 0.002f;
            renderRing(radius, width, y, SHOCKWAVE_CORE_R, SHOCKWAVE_CORE_G, SHOCKWAVE_CORE_B,
                    alpha, position, normal, consumer);
        }
    }

    private static void surfaceFlowStreak(float radius, float startPhi, float endPhi,
                                          float startTheta, float twist, float halfWidthRad,
                                          float wavePhase, float flowPhase, int segments,
                                          float r, float g, float b, float alpha,
                                          Matrix4f position, Matrix3f normal, VertexConsumer consumer) {
        float phiMax = (float) Math.acos(-LOWER_EXTENT);
        for (int s = 0; s < segments; s++) {
            for (int side = 0; side < 2; side++) {
                for (int corner = 0; corner < 4; corner++) {
                    float f = (s + (corner >= 2 ? 1.0f : 0.0f)) / segments;
                    int col = side + (corner == 1 || corner == 2 ? 1 : 0);

                    float bodyGlow = 0.22f + 0.78f * Mth.sin(f * Mth.PI);
                    float crownProgress = Mth.clamp((f - 0.72f) / 0.28f, 0.0f, 1.0f);
                    crownProgress = crownProgress * crownProgress * (3.0f - 2.0f * crownProgress);
                    float convergenceGlow = Math.min(1.0f, bodyGlow + crownProgress * STREAK_CROWN_GLOW);

                    float pulseWave = 0.5f + 0.5f * Mth.sin(Mth.TWO_PI
                            * (f * STREAK_FLOW_PULSES - flowPhase));
                    float pulse = pulseWave * pulseWave * pulseWave;
                    float streamAlpha = alpha * convergenceGlow * (0.34f + 0.66f * pulse);

                    float widthTaper = 0.28f + 0.72f * Mth.sin(f * Mth.PI);
                    float theta = startTheta + twist * f
                            + 0.08f * Mth.sin(f * Mth.TWO_PI * STREAK_WAVE_FREQUENCY + wavePhase);
                    float phi = Mth.lerp(f, startPhi, endPhi)
                            + STREAK_WAVE_AMPLITUDE * Mth.sin(f * Mth.TWO_PI * STREAK_WAVE_FREQUENCY
                            + wavePhase + flowPhase * 0.35f) * Mth.sin(f * Mth.PI)
                            + (col - 1) * halfWidthRad * widthTaper;
                    phi = Mth.clamp(phi, 0.015f, phiMax);
                    float sinPhi = Mth.sin(phi);
                    vertex(consumer, position, normal, radius * sinPhi * Mth.cos(theta), radius * Mth.cos(phi),
                            radius * sinPhi * Mth.sin(theta), r, g, b, col == 1 ? streamAlpha : 0.0f);
                }
            }
        }
    }

    private static void renderNuclearShockwave(float age, float targetRadius, Matrix4f position, Matrix3f normal, VertexConsumer consumer) {
        float p = age / SHOCKWAVE_TICKS;
        float ease = easeOut(p);
        float radius = Mth.lerp(ease, EXPANDING_RING_START_RADIUS, targetRadius);
        float alpha = (1.0f - p) * (1.0f - p);

        // 1. Expanding ground shockwave rings
        renderExpandingGroundRings(p, targetRadius, position, normal, consumer);
        renderRing(radius, SHOCKWAVE_RING_WIDTH * (1.0f + ease * 0.5f), SIGIL_Y + 0.02f,
                SHOCKWAVE_R, SHOCKWAVE_G, SHOCKWAVE_B, alpha * 0.9f, position, normal, consumer);
        renderRing(radius * 0.95f, SHOCKWAVE_RING_WIDTH * 0.5f, SIGIL_Y + 0.03f,
                SHOCKWAVE_CORE_R, SHOCKWAVE_CORE_G, SHOCKWAVE_CORE_B, alpha, position, normal, consumer);

        // 2. Vertical shockwave wall
        float wallHeight = SHOCKWAVE_WALL_HEIGHT * (1.0f - ease * 0.4f);
        for (int i = 0; i < SIGIL_SEGMENTS; i++) {
            float c0 = RING_COS[i], s0 = RING_SIN[i], c1 = RING_COS[i + 1], s1 = RING_SIN[i + 1];
            vertex(consumer, position, normal, c0 * radius, 0.0f, s0 * radius,
                    SHOCKWAVE_CORE_R, SHOCKWAVE_CORE_G, SHOCKWAVE_CORE_B, alpha * 0.75f);
            vertex(consumer, position, normal, c1 * radius, 0.0f, s1 * radius,
                    SHOCKWAVE_CORE_R, SHOCKWAVE_CORE_G, SHOCKWAVE_CORE_B, alpha * 0.75f);
            vertex(consumer, position, normal, c1 * radius, wallHeight, s1 * radius,
                    SHOCKWAVE_R, SHOCKWAVE_G, SHOCKWAVE_B, 0.0f);
            vertex(consumer, position, normal, c0 * radius, wallHeight, s0 * radius,
                    SHOCKWAVE_R, SHOCKWAVE_G, SHOCKWAVE_B, 0.0f);
        }

        // 3. Piercing blast rays shooting outwards
        for (int ray = 0; ray < 8; ray++) {
            float angle = ray * (float) Math.PI * 0.25f + age * 0.04f;
            float cosA = Mth.cos(angle), sinA = Mth.sin(angle);
            float rayLen = radius * 1.05f;
            vertex(consumer, position, normal, 0.0f, 0.5f, 0.0f, SHOCKWAVE_CORE_R, SHOCKWAVE_CORE_G, SHOCKWAVE_CORE_B, alpha * 0.8f);
            vertex(consumer, position, normal, cosA * rayLen, 0.5f, sinA * rayLen, SHOCKWAVE_CORE_R, SHOCKWAVE_CORE_G, SHOCKWAVE_CORE_B, 0.0f);
            vertex(consumer, position, normal, cosA * rayLen, 1.8f, sinA * rayLen, SHOCKWAVE_R, SHOCKWAVE_G, SHOCKWAVE_B, 0.0f);
            vertex(consumer, position, normal, 0.0f, 1.8f, 0.0f, SHOCKWAVE_R, SHOCKWAVE_G, SHOCKWAVE_B, alpha * 0.8f);
        }
    }

    private static float easeOut(float p) {
        float inv = 1.0f - p;
        return 1.0f - inv * inv * inv;
    }

    private static float easeOutBack(float p) {
        float x = p - 1.0f;
        return 1.0f + (DOME_OPEN_OVERSHOOT + 1.0f) * x * x * x + DOME_OPEN_OVERSHOOT * x * x;
    }

    private static float fresnelEdgeTerm(float lx, float ly, float lz, float nx, float ny, float nz,
                                         float camLocalX, float camLocalY, float camLocalZ) {
        float dx = camLocalX - lx;
        float dy = camLocalY - ly;
        float dz = camLocalZ - lz;
        float lenSq = dx * dx + dy * dy + dz * dz;
        if (lenSq < 1.0e-6f) return BASE_ALPHA;
        float invLen = 1.0f / (float) Math.sqrt(lenSq);
        float dot = nx * (dx * invLen) + ny * (dy * invLen) + nz * (dz * invLen);
        float f = Mth.clamp(1.0f - Math.abs(dot), 0.0f, 1.0f);
        return BASE_ALPHA + EDGE_ALPHA * (float) Math.pow(f, EDGE_POWER);
    }

    private static float insideFactor(float camLocalX, float camLocalY, float camLocalZ) {
        double r = Math.sqrt((camLocalX * camLocalX + camLocalZ * camLocalZ) / (BASE_RADIUS * BASE_RADIUS)
                + (Math.max(camLocalY, 0.0) * Math.max(camLocalY, 0.0)) / (BASE_RADIUS * BASE_RADIUS));
        float t = Mth.clamp((float) (1.0 - r), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private static float rimFactor(float vertexHeight) {
        return 1.0f - Mth.clamp(Math.abs(vertexHeight) / RIM_HEIGHT, 0.0f, 1.0f);
    }

    private static float fadeInAlpha(float openAge) {
        float p = Mth.clamp(openAge / FADE_IN_TICKS, 0.0f, 1.0f);
        float inv = 1.0f - p;
        return 1.0f - inv * inv * inv;
    }

    private static void renderRing(float radius, float width, float y, float r, float g, float b, float alpha,
                                   Matrix4f position, Matrix3f normal, VertexConsumer consumer) {
        for (int i = 0; i < SIGIL_SEGMENTS; i++) {
            for (int corner = 0; corner < 4; corner++) {
                int j = i + (corner >= 2 ? 1 : 0);
                float rr = radius + (corner == 0 || corner == 3 ? -0.5f : 0.5f) * width;
                vertex(consumer, position, normal, RING_COS[j] * rr, y, RING_SIN[j] * rr, r, g, b, alpha);
            }
        }
    }

    private static void vertex(VertexConsumer consumer, Matrix4f position, Matrix3f normal,
                               float x, float y, float z, float r, float g, float b, float alpha) {
        consumer.vertex(position, x, y, z).color(r, g, b, alpha).uv(0.5f, 0.5f)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0f, 1.0f, 0.0f).endVertex();
    }

    private static float hash(int a, int b, int c, int d) {
        int h = a * 0x9E3779B1 + b * 0x85EBCA77 + c * 0xC2B2AE3D + d * 0x27D4EB2F;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        h *= 0x297A2D39;
        h ^= h >>> 15;
        return (h >>> 8) * 0x1.0p-24f;
    }
}
