package io.redspace.ironspell_more.entity.spells.rapturous_bloom;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
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

import static io.redspace.ironspell_more.entity.spells.rapturous_bloom.RapturousBloomVisuals.*;

/**
 * Procedural Geometric Renderer for Rapturous Bloom.
 * Renders a glowing ethereal lotus/plum blossom with 3 layers of curved petals,
 * radiant golden stamen, animated ground water ripples, and radial shatter dispersion.
 *
 * Sequence:
 * 1. หุบเป็นตุ่ม (Closed Bud): Emerges and rests as a closed bud in the center of water ripples (0-2s)
 * 2. ค่อยผลิบาน (Unfolding): Outer -> Middle -> Inner petals peel back smoothly (2-5.1s)
 * 3. บานเต็มที่ (Full Bloom): Fully open blossom with gentle breathing and rotation (5.1-6s)
 * 4. แตกออก (Shatter): Petals disperse from 6-6.5s; the final ripple fades by 6.8s
 */
public class RapturousBloomRenderer extends EntityRenderer<RapturousBloomEntity> {
    private static final ResourceLocation WHITE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("forge", "textures/white.png");

    private static final RenderStateShard.TransparencyStateShard ADDITIVE_ALPHA_TRANSPARENCY =
            new RenderStateShard.TransparencyStateShard("rb_bloom_additive_alpha", () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            }, () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            });

    private static final RenderType EFFECT_RENDER_TYPE = buildBloomRenderType();
    private static final RenderType OPAQUE_PETAL_RENDER_TYPE = RenderType.entityCutoutNoCull(WHITE_TEXTURE);

    private static RenderType buildBloomRenderType() {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityTranslucentEmissiveShader))
                .setTextureState(new RenderStateShard.TextureStateShard(WHITE_TEXTURE, false, false))
                .setTransparencyState(ADDITIVE_ALPHA_TRANSPARENCY)
                .setCullState(new RenderStateShard.CullStateShard(false))
                .setLightmapState(new RenderStateShard.LightmapStateShard(false))
                .setOverlayState(new RenderStateShard.OverlayStateShard(true))
                .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, false))
                .createCompositeState(false);
        return RenderType.create("ironspell_more_rapturous_bloom", DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS, 256, false, true, state);
    }

    private static final float[] RING_COS = new float[GROUND_SEGMENTS + 1];
    private static final float[] RING_SIN = new float[GROUND_SEGMENTS + 1];

    /*
     * Petal topology. Width remains non-zero at u=1, while the cap extends the
     * center columns farther than the sides to produce a broad half-ellipse.
     */
    private static final float[] PETAL_ROW_U = {
            0.00F, 0.12F, 0.26F, 0.42F, 0.58F, 0.72F, 0.84F, 0.93F, 1.00F
    };
    private static final float[] PETAL_COLUMN = {
            -1.00F, -0.66F, -0.33F, 0.00F, 0.33F, 0.66F, 1.00F
    };
    // Outer, middle, and inner variants share one parametric surface.
    private static final float[] PETAL_BASE_WIDTH_FACTOR = {0.10F, 0.08F, 0.07F};
    private static final float[] PETAL_SHOULDER_POSITION = {0.68F, 0.72F, 0.76F};
    private static final float[] PETAL_TIP_TAPER = {0.14F, 0.20F, 0.26F};
    private static final float[] PETAL_CAP_STRENGTH = {0.20F, 0.16F, 0.12F};
    private static final float PETAL_EDGE_WAVE_AMPLITUDE = 0.035F;
    static {
        for (int i = 0; i <= GROUND_SEGMENTS; i++) {
            double angle = 2.0 * Math.PI * i / GROUND_SEGMENTS;
            RING_COS[i] = (float) Math.cos(angle);
            RING_SIN[i] = (float) Math.sin(angle);
        }
    }

    public RapturousBloomRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(RapturousBloomEntity entity) {
        return WHITE_TEXTURE;
    }

    @Override
    public boolean shouldRender(RapturousBloomEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(RapturousBloomEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
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

        VertexConsumer effectConsumer = buffer.getBuffer(EFFECT_RENDER_TYPE);
        VertexConsumer opaquePetalConsumer = buffer.getBuffer(OPAQUE_PETAL_RENDER_TYPE);

        int phase = entity.getPhase();
        int seed = entity.getId();

        // 1. Render Ground Visuals (Water ripples, floral resonance circle, shockwave)
        renderGroundVisuals(phase, time, positionMatrix, normalMatrix, effectConsumer);

        // 2. Render Layered Lotus/Plum Blossom Flower Model
        renderFlowerModel(phase, time, seed, positionMatrix, normalMatrix,
                opaquePetalConsumer, effectConsumer);
    }

    // =========================================================================
    // 1. Ground Visuals Rendering
    // =========================================================================

    private void renderGroundVisuals(int phase, float time, Matrix4f position, Matrix3f normal, VertexConsumer consumer) {
        if (phase == RapturousBloomEntity.PHASE_RIPPLE) {
            // Phase 1 (0-2s / 40 ticks): slow, thin aqua ripples.
            float rippleAlpha = smootherstep(0.0F, 12.0F, time);
            float boundaryPulse = 0.82F + 0.18F * Mth.sin(time * 0.08F);

            // Static boundary stays a single restrained line. Layered glow is
            // reserved for the moving ripples only.
            renderRing(BASE_RADIUS, RING_WIDTH * 0.32F, GROUND_Y,
                    RIPPLE_R, RIPPLE_G, RIPPLE_B, 0.32F * rippleAlpha * boundaryPulse,
                    position, normal, consumer);

            // Expanding ripple wave circles
            for (int k = 0; k < RIPPLE_COUNT; k++) {
                float cycle = (time / RIPPLE_CYCLE_TICKS + (float) k / RIPPLE_COUNT);
                float p = cycle - Mth.floor(cycle);
                float currentRadius = p * BASE_RADIUS;
                float waveAlpha = rippleFade(p) * 0.68F * rippleAlpha;

                renderRippleRing(currentRadius, RIPPLE_LINE_WIDTH, GROUND_Y + 0.002F * k,
                        RIPPLE_CORE_R, RIPPLE_CORE_G, RIPPLE_CORE_B, waveAlpha,
                        position, normal, consumer);
            }
        } else if (phase == RapturousBloomEntity.PHASE_BLOOM) {
            // Phase 2: cross-fade the water rings into the floral sigil.
            float bloomAge = time - RapturousBloomEntity.RIPPLE_DURATION_TICKS;
            float transition = smootherstep(0.0F, 20.0F, bloomAge);
            float waterFade = 1.0F - transition;
            float ringSwap = 0.5F + 0.5F * Mth.sin(time * 0.10F);

            // Preserve outgoing ripples briefly so there is no phase pop.
            renderRing(BASE_RADIUS, RING_WIDTH * 0.32F, GROUND_Y,
                    RIPPLE_R, RIPPLE_G, RIPPLE_B, 0.30F * waterFade,
                    position, normal, consumer);
            for (int k = 0; k < RIPPLE_COUNT; k++) {
                float cycle = (time / RIPPLE_CYCLE_TICKS + (float) k / RIPPLE_COUNT);
                float p = cycle - Mth.floor(cycle);
                renderRippleRing(p * BASE_RADIUS, RIPPLE_LINE_WIDTH,
                        GROUND_Y + 0.002F * k, RIPPLE_CORE_R, RIPPLE_CORE_G, RIPPLE_CORE_B,
                        rippleFade(p) * 0.60F * waterFade, position, normal, consumer);
            }

            // The rings trade brightness through phase-shifted sine curves.
            renderRing(BASE_RADIUS, RING_WIDTH * 0.38F, GROUND_Y,
                    GROUND_SIGIL_R, GROUND_SIGIL_G, GROUND_SIGIL_B,
                    (0.30F + 0.14F * ringSwap) * transition, position, normal, consumer);

            // Inner concentric decorative rings
            renderRing(1.85F, RING_WIDTH * 0.30F, GROUND_Y + 0.002F,
                    GROUND_SIGIL_R, GROUND_SIGIL_G, GROUND_SIGIL_B,
                    (0.23F + 0.11F * (1.0F - ringSwap)) * transition, position, normal, consumer);
            renderRing(0.90F, RING_WIDTH * 0.26F, GROUND_Y + 0.004F,
                    PETAL_RIM_R, PETAL_RIM_G, PETAL_RIM_B,
                    (0.30F + 0.13F * ringSwap) * transition, position, normal, consumer);

            // Subtle secondary water ripples lingering beneath the flower
            for (int k = 0; k < 2; k++) {
                float cycle = (time / (RIPPLE_CYCLE_TICKS * 1.3F) + (float) k / 2.0F);
                float p = cycle - Mth.floor(cycle);
                float waveAlpha = rippleFade(p) * 0.18F * transition;
                renderRippleRing(p * BASE_RADIUS, RIPPLE_LINE_WIDTH * 0.80F, GROUND_Y + 0.001F,
                        RIPPLE_R, RIPPLE_G, RIPPLE_B, waveAlpha, position, normal, consumer);
            }
        } else if (phase == RapturousBloomEntity.PHASE_BURST) {
            // Phase 3: a soft luminous shockwave grows out of the fading sigil.
            float burstAge = time - RapturousBloomEntity.BURST_TICK;
            float p = Mth.clamp(burstAge / RapturousBloomEntity.BURST_VISUAL_TICKS, 0.0F, 1.0F);
            float ease = smootherstep01(p);
            float sigilFade = 1.0F - smootherstep(0.0F, 0.32F, p);
            float currentRadius = Mth.lerp(ease, 0.6F, 4.4F);
            float alpha = rippleFade(p);

            renderRing(BASE_RADIUS, RING_WIDTH * 0.38F, GROUND_Y,
                    GROUND_SIGIL_R, GROUND_SIGIL_G, GROUND_SIGIL_B,
                    0.34F * sigilFade, position, normal, consumer);
            renderRing(1.85F, RING_WIDTH * 0.30F, GROUND_Y + 0.002F,
                    PETAL_RIM_R, PETAL_RIM_G, PETAL_RIM_B,
                    0.26F * sigilFade, position, normal, consumer);
            renderRippleRing(currentRadius, BURST_RIPPLE_LINE_WIDTH, GROUND_Y + 0.01F,
                    BURST_R, BURST_G, BURST_B, alpha * 0.88F,
                    position, normal, consumer);
        }
    }

    private static void renderRippleRing(float radius, float lineWidth, float y,
                                         float r, float g, float b, float alpha,
                                         Matrix4f position, Matrix3f normal, VertexConsumer consumer) {
        if (alpha <= 0.003F || radius <= 0.01F) return;

        // A narrow bright core plus two faint bands reads as a thin water line
        // with glow, without turning the whole ground sigil into a thick halo.
        renderRing(radius, lineWidth * 4.0F, y, r, g, b, alpha * 0.07F, position, normal, consumer);
        renderRing(radius, lineWidth * 2.2F, y + 0.0005F, r, g, b, alpha * 0.16F, position, normal, consumer);
        renderRing(radius, lineWidth, y + 0.001F, r, g, b, alpha * 0.82F, position, normal, consumer);
    }

    private static void renderRing(float radius, float width, float y,
                                   float r, float g, float b, float alpha,
                                   Matrix4f position, Matrix3f normal, VertexConsumer consumer) {
        if (alpha <= 0.003F || radius <= 0.01F) return;
        float rInner = Math.max(0.0F, radius - width * 0.5F);
        float rOuter = radius + width * 0.5F;

        for (int i = 0; i < GROUND_SEGMENTS; i++) {
            int next = i + 1;
            vertex(consumer, position, normal, RING_COS[i] * rInner, y, RING_SIN[i] * rInner, r, g, b, alpha);
            vertex(consumer, position, normal, RING_COS[next] * rInner, y, RING_SIN[next] * rInner, r, g, b, alpha);
            vertex(consumer, position, normal, RING_COS[next] * rOuter, y, RING_SIN[next] * rOuter, r, g, b, alpha);
            vertex(consumer, position, normal, RING_COS[i] * rOuter, y, RING_SIN[i] * rOuter, r, g, b, alpha);
        }
    }

    // =========================================================================
    // 2. Layered Flower Model & Animations (According to Reference Diagram)
    // =========================================================================

    private void renderFlowerModel(int phase, float time, int seed,
                                   Matrix4f position, Matrix3f normal,
                                   VertexConsumer opaquePetalConsumer, VertexConsumer effectConsumer) {
        float flowerScale;
        float outerTilt;
        float middleTilt;
        float innerTilt;
        float bloomProgress;
        float stamenHeight;
        float stamenAlpha;
        float idleRot = time * 0.004F; // Gentle continuous rotation
        float burstProgress = -1.0F;

        if (phase == RapturousBloomEntity.PHASE_RIPPLE) {
            // Stage 1: เริ่มต้น (หุบเป็นตุ่มดอกไม้อยู่ตรงกลางวงน้ำ)
            // Grow slowly from a tiny closed bud for the whole ripple phase.
            float budEmerge = smootherstep(0.0F, RapturousBloomEntity.RIPPLE_DURATION_TICKS, time);
            float bob = 0.008F * budEmerge * Mth.sin(time * 0.12F);
            flowerScale = Mth.lerp(budEmerge, BUD_START_SCALE, BUD_END_SCALE) + bob;

            // Tightly closed upright bud angles
            outerTilt = OUTER_BUD_ANGLE;
            middleTilt = MIDDLE_BUD_ANGLE;
            innerTilt = INNER_BUD_ANGLE;
            bloomProgress = 0.0F;

            // Keep a fixed stamen shape and reveal it only through opacity.
            stamenHeight = STAMEN_HEIGHT;
            stamenAlpha = 0.15F * smootherstep(0.45F, 1.0F, budEmerge);
        } else if (phase == RapturousBloomEntity.PHASE_BLOOM) {
            // Stage 2 & 3: ค่อยๆ ผลิบาน -> บานเต็มที่ (Layer-by-layer progressive unfolding)
            float bloomAge = time - RapturousBloomEntity.RIPPLE_DURATION_TICKS;

            // Layered unfolding now occupies most of the four-second phase.
            float pOuter = Mth.clamp(bloomAge / OUTER_BLOOM_TICKS, 0.0F, 1.0F);
            float pMid = Mth.clamp((bloomAge - MIDDLE_BLOOM_DELAY_TICKS) / MIDDLE_BLOOM_TICKS, 0.0F, 1.0F);
            float pInner = Mth.clamp((bloomAge - INNER_BLOOM_DELAY_TICKS) / INNER_BLOOM_TICKS, 0.0F, 1.0F);
            float pScale = Mth.clamp(bloomAge / FLOWER_GROWTH_TICKS, 0.0F, 1.0F);

            float easeOuter = smootherstep01(pOuter);
            float easeMid = smootherstep01(pMid);
            float easeInner = smootherstep01(pInner);

            outerTilt = Mth.lerp(easeOuter, OUTER_BUD_ANGLE, OUTER_BLOOM_ANGLE);
            middleTilt = Mth.lerp(easeMid, MIDDLE_BUD_ANGLE, MIDDLE_BLOOM_ANGLE);
            innerTilt = Mth.lerp(easeInner, INNER_BUD_ANGLE, INNER_BLOOM_ANGLE);

            bloomProgress = (easeOuter + easeMid + easeInner) / 3.0F;

            // Fade breathing in near the end so opening motion remains steady.
            float easedScale = smootherstep01(pScale);
            float breatheWeight = smootherstep(0.82F, 1.0F, pScale);
            float breathe = 1.0F + 0.018F * breatheWeight * Mth.sin(time * 0.075F);
            flowerScale = Mth.lerp(easedScale, BUD_END_SCALE, 1.0F) * breathe;

            // Reveal the fixed-height stamen without stretching its geometry.
            float stamenReveal = smootherstep(0.10F, 0.82F, pInner);
            stamenHeight = STAMEN_HEIGHT;
            stamenAlpha = Mth.lerp(stamenReveal, 0.15F, 0.92F);
        } else {
            // Stage 4 & 5: แตกออก -> กระจายเป็นชิ้นเล็กๆ (Shatter & Disperse)
            float burstAge = time - RapturousBloomEntity.BURST_TICK;
            burstProgress = Mth.clamp(burstAge / RapturousBloomEntity.PETAL_SHATTER_TICKS, 0.0F, 1.0F);
            flowerScale = 1.0F;
            outerTilt = OUTER_BLOOM_ANGLE;
            middleTilt = MIDDLE_BLOOM_ANGLE;
            innerTilt = INNER_BLOOM_ANGLE;
            bloomProgress = 1.0F;
            stamenHeight = STAMEN_HEIGHT;
            // Fade in place; the center no longer stretches or flies outward.
            stamenAlpha = 1.0F - smootherstep01(burstProgress);
        }

        if (flowerScale <= 0.01F) return;

        // Render Center Stamen (เกสร)
        renderCenterStamen(time, stamenHeight, flowerScale, stamenAlpha,
                position, normal, effectConsumer);

        // Stable petals are rendered through an opaque, depth-writing layer so
        // their scarlet colors stay saturated. Shattering petals retain the
        // additive layer so the existing fade-out animation remains intact.
        VertexConsumer petalConsumer = burstProgress < 0.0F ? opaquePetalConsumer : effectConsumer;

        // Render Outer Layer (กลีบชั้นนอก - 10 Petals)
        renderPetalLayer(OUTER_PETAL_COUNT, 0.0F, OUTER_PETAL_LENGTH, OUTER_PETAL_WIDTH,
                OUTER_PETAL_CURVE, outerTilt, OUTER_PIVOT_Y, flowerScale, idleRot,
                bloomProgress, burstProgress, seed, 0, position, normal, petalConsumer);

        // Render Middle Layer (กลีบชั้นกลาง - 8 Petals, offset angle)
        renderPetalLayer(MIDDLE_PETAL_COUNT, (float) Math.PI / 8.0F, MIDDLE_PETAL_LENGTH, MIDDLE_PETAL_WIDTH,
                MIDDLE_PETAL_CURVE, middleTilt, MIDDLE_PIVOT_Y, flowerScale, idleRot,
                bloomProgress, burstProgress, seed, 1, position, normal, petalConsumer);

        // Render Inner Layer (กลีบชั้นใน - 6 Petals, offset angle)
        renderPetalLayer(INNER_PETAL_COUNT, (float) Math.PI / 6.0F, INNER_PETAL_LENGTH, INNER_PETAL_WIDTH,
                INNER_PETAL_CURVE, innerTilt, INNER_PIVOT_Y, flowerScale, idleRot,
                bloomProgress, burstProgress, seed, 2, position, normal, petalConsumer);
    }

    // =========================================================================
    // 3. Petal Layer & Shard Geometry Generator
    // =========================================================================

    private void renderPetalLayer(int count, float angleOffset, float length, float width,
                                  float curve, float tiltAngle, float pivotY, float scale,
                                  float idleRot, float bloomProgress, float burstProgress,
                                  int seed, int layerId,
                                  Matrix4f position, Matrix3f normal, VertexConsumer consumer) {
        boolean shattering = burstProgress >= 0.0F;
        float baseAlpha = shattering ? (1.0F - burstProgress) * (1.0F - burstProgress) : 1.0F;
        if (baseAlpha <= 0.005F) return;

        float easeBurst = shattering ? easeOutCubic(burstProgress) : 0.0F;

        for (int i = 0; i < count; i++) {
            float headingAngle = (float) (2.0 * Math.PI * i / count) + angleOffset + idleRot;

            // Deterministic per-petal variation keeps the layered flower organic
            // without introducing more mesh types or changing animation state.
            float lengthScale = 0.95F + hash(seed, i, layerId, 10) * 0.10F;
            float widthScale = 0.93F + hash(seed, i, layerId, 11) * 0.14F;
            float asymmetry = (hash(seed, i, layerId, 12) - 0.5F) * 0.12F;
            float edgeWavePhase = hash(seed, i, layerId, 13) * Mth.TWO_PI;

            float px = 0.0F;
            float py = pivotY * scale;
            float pz = 0.0F;

            float currentTilt = tiltAngle;
            float currentRoll = 0.0F;

            if (shattering) {
                // Shatter outward radial vector (แตกออก)
                float radialSpeed = 3.6F + hash(seed, i, layerId, 1) * 1.5F;
                float flyDist = easeBurst * radialSpeed;
                px += Mth.cos(headingAngle) * flyDist;
                pz += Mth.sin(headingAngle) * flyDist;
                py += easeBurst * (1.2F + hash(seed, i, layerId, 2) * 0.8F) - easeBurst * easeBurst * 1.5F;

                // Tumble rotation in flight
                currentTilt += burstProgress * (2.5F + hash(seed, i, layerId, 3) * 2.0F);
                currentRoll += burstProgress * (i % 2 == 0 ? 1.0F : -1.0F) * 4.0F;

                // Render small broken shards (กระจายเป็นชิ้นเล็กๆ)
                renderPetalShards(headingAngle, px, py, pz, length * scale, width * scale,
                        burstProgress, seed, i, layerId, position, normal, consumer);
            }

            // Render main curved petal mesh
            renderSinglePetal(px, py, pz, headingAngle, currentTilt, currentRoll,
                    length * scale * lengthScale, width * scale * widthScale,
                    curve * scale, baseAlpha, bloomProgress, layerId, asymmetry, edgeWavePhase,
                    position, normal, consumer);
        }
    }

    private void renderSinglePetal(float px, float py, float pz,
                                   float heading, float tilt, float roll,
                                   float length, float width, float curve, float alpha,
                                   float bloomProgress, int shapeVariant,
                                   float asymmetry, float edgeWavePhase,
                                   Matrix4f position, Matrix3f normal, VertexConsumer consumer) {
        float cosH = Mth.cos(heading);
        float sinH = Mth.sin(heading);
        float cosT = Mth.cos(tilt);
        float sinT = Mth.sin(tilt);
        float cosR = Mth.cos(roll);
        float sinR = Mth.sin(roll);

        // Orthonormal basis vectors:
        // Heading radial unit vector on horizontal plane:
        float rx = cosH, rz = sinH;
        // Lateral width unit vector:
        float wx = -sinH, wz = cosH;
        // Longitudinal direction of the petal (tilted by 'tilt' from vertical +Y):
        float tx = sinT * rx;
        float ty = cosT;
        float tz = sinT * rz;
        // Normal direction perpendicular to the petal surface (pointing outward):
        float nx = cosT * rx;
        float ny = -sinT;
        float nz = cosT * rz;

        // Apply roll twist around longitudinal axis t:
        float wpx = wx * cosR + nx * sinR;
        float wpy = ny * sinR;
        float wpz = wz * cosR + nz * sinR;

        float npx = -wx * sinR + nx * cosR;
        float npy = ny * cosR;
        float npz = -wz * sinR + nz * cosR;

        int profile = Mth.clamp(shapeVariant, 0, PETAL_BASE_WIDTH_FACTOR.length - 1);
        for (int row = 0; row < PETAL_LENGTH_ROWS - 1; row++) {
            for (int column = 0; column < PETAL_WIDTH_COLUMNS - 1; column++) {
                emitPetalGridVertex(row, column, profile, px, py, pz, length, width, curve,
                        bloomProgress, asymmetry, edgeWavePhase,
                        tx, ty, tz, wpx, wpy, wpz, npx, npy, npz,
                        alpha, position, normal, consumer);
                emitPetalGridVertex(row + 1, column, profile, px, py, pz, length, width, curve,
                        bloomProgress, asymmetry, edgeWavePhase,
                        tx, ty, tz, wpx, wpy, wpz, npx, npy, npz,
                        alpha, position, normal, consumer);
                emitPetalGridVertex(row + 1, column + 1, profile, px, py, pz, length, width, curve,
                        bloomProgress, asymmetry, edgeWavePhase,
                        tx, ty, tz, wpx, wpy, wpz, npx, npy, npz,
                        alpha, position, normal, consumer);
                emitPetalGridVertex(row, column + 1, profile, px, py, pz, length, width, curve,
                        bloomProgress, asymmetry, edgeWavePhase,
                        tx, ty, tz, wpx, wpy, wpz, npx, npy, npz,
                        alpha, position, normal, consumer);
            }
        }
    }

    private void emitPetalGridVertex(int row, int column, int profile,
                                     float px, float py, float pz,
                                     float length, float width, float curve, float bloomProgress,
                                     float asymmetry, float edgeWavePhase,
                                     float tx, float ty, float tz,
                                     float wpx, float wpy, float wpz,
                                     float npx, float npy, float npz,
                                     float alpha, Matrix4f position, Matrix3f normal,
                                     VertexConsumer consumer) {
        float lateral = PETAL_COLUMN[column];
        float absLateral = Math.abs(lateral);
        float u = PETAL_ROW_U[row];

        float shoulderPosition = PETAL_SHOULDER_POSITION[profile];
        float grow = smoothstep(0.0F, shoulderPosition, u);
        float taper = smoothstep(shoulderPosition, 1.0F, u);
        float baseWidthFactor = PETAL_BASE_WIDTH_FACTOR[profile];
        float widthEnvelope = (baseWidthFactor + (1.0F - baseWidthFactor) * grow)
                * (1.0F - PETAL_TIP_TAPER[profile] * taper);

        // Keep the wave subtle and confined mainly to the upper half.
        float edgeWave = 1.0F + PETAL_EDGE_WAVE_AMPLITUDE
                * smoothstep(0.40F, 1.0F, u)
                * Mth.sin(u * Mth.PI * 6.0F + edgeWavePhase);
        float sideBias = 1.0F + lateral * asymmetry;
        float lateralOffset = lateral * width * 0.5F * widthEnvelope * edgeWave * sideBias;

        float capRegion = smoothstep(0.68F, 1.0F, u);
        float capShape = (float) Math.pow(Math.max(0.0F, 1.0F - lateral * lateral), 1.4D);
        float capExtension = PETAL_CAP_STRENGTH[profile] * capRegion * capShape;
        float longitudinal = (u + capExtension) * length;

        // Retain a small closed-to-open curvature transition while converging on
        // the requested soft arch and raised side edges at full bloom.
        float archStrength = Mth.lerp(bloomProgress, 0.48F, 0.55F);
        float sideCupStrength = Mth.lerp(bloomProgress, 0.16F, 0.22F);
        float arch = curve * Mth.sin(u * Mth.PI * 0.5F) * archStrength;
        float sideCup = curve * sideCupStrength
                * (float) Math.pow(absLateral, 1.8D) * u;
        float surfaceDepth = arch + sideCup;

        float x = px + longitudinal * tx + lateralOffset * wpx + surfaceDepth * npx;
        float y = py + longitudinal * ty + lateralOffset * wpy + surfaceDepth * npy;
        float z = pz + longitudinal * tz + lateralOffset * wpz + surfaceDepth * npz;

        float rimMix = (float) Math.pow(absLateral, 1.7D);
        float baseBlend = smoothstep(0.0F, 0.20F, u);
        float bodyBlend = smoothstep(0.20F, 0.65F, u);
        float brightBlend = smoothstep(0.65F, 1.0F, u);
        float centerR = Mth.lerp(baseBlend, PETAL_SHADOW_R, PETAL_BASE_R);
        float centerG = Mth.lerp(baseBlend, PETAL_SHADOW_G, PETAL_BASE_G);
        float centerB = Mth.lerp(baseBlend, PETAL_SHADOW_B, PETAL_BASE_B);
        centerR = Mth.lerp(bodyBlend, centerR, PETAL_BODY_R);
        centerG = Mth.lerp(bodyBlend, centerG, PETAL_BODY_G);
        centerB = Mth.lerp(bodyBlend, centerB, PETAL_BODY_B);
        centerR = Mth.lerp(brightBlend, centerR, PETAL_BRIGHT_R);
        centerG = Mth.lerp(brightBlend, centerG, PETAL_BRIGHT_G);
        centerB = Mth.lerp(brightBlend, centerB, PETAL_BRIGHT_B);
        float highlightMix = smoothstep(0.72F, 1.0F, u);
        float edgeR = Mth.lerp(highlightMix, PETAL_RIM_R, PETAL_HIGHLIGHT_R);
        float edgeG = Mth.lerp(highlightMix, PETAL_RIM_G, PETAL_HIGHLIGHT_G);
        float edgeB = Mth.lerp(highlightMix, PETAL_RIM_B, PETAL_HIGHLIGHT_B);
        float r = Mth.lerp(rimMix, centerR, edgeR);
        float g = Mth.lerp(rimMix, centerG, edgeG);
        float b = Mth.lerp(rimMix, centerB, edgeB);
        float vertexAlpha = alpha < 1.0F ? Mth.lerp(rimMix, alpha * 0.95F, alpha) : 1.0F;
        vertex(consumer, position, normal, x, y, z, r, g, b, vertexAlpha);
    }

    // =========================================================================
    // 4. Broken Shards Generator (ชิ้นเล็กๆ กระจายตัวตามเรฟ)
    // =========================================================================

    private void renderPetalShards(float heading, float px, float py, float pz, float length, float width,
                                   float burstProgress, int seed, int petalIdx, int layerId,
                                   Matrix4f position, Matrix3f normal, VertexConsumer consumer) {
        float shardAlpha = (1.0F - burstProgress) * (1.0F - burstProgress) * 0.95F;
        if (shardAlpha <= 0.01F) return;

        int shardCount = 2;
        float shardScale = (1.0F - burstProgress * 0.6F) * 0.28F;

        for (int s = 0; s < shardCount; s++) {
            float offAngle = heading + (hash(seed, petalIdx, layerId, s * 5 + 4) - 0.5F) * 0.8F;
            float extraDist = burstProgress * (1.8F + hash(seed, petalIdx, layerId, s * 5 + 5) * 2.2F);
            float sx = px + Mth.cos(offAngle) * extraDist;
            float sz = pz + Mth.sin(offAngle) * extraDist;
            float sy = py + burstProgress * (0.6F + hash(seed, petalIdx, layerId, s * 5 + 6) * 1.2F);

            float sw = width * shardScale;
            float sl = length * shardScale;
            float spin = burstProgress * 12.0F + petalIdx + s;
            float cosS = Mth.cos(spin) * sw;
            float sinS = Mth.sin(spin) * sl;

            // Small glowing shard quad
            vertex(consumer, position, normal, sx - cosS, sy - sinS, sz, PETAL_RIM_R, PETAL_RIM_G, PETAL_RIM_B, shardAlpha);
            vertex(consumer, position, normal, sx + sinS, sy - cosS, sz, PETAL_RIM_R, PETAL_RIM_G, PETAL_RIM_B, shardAlpha);
            vertex(consumer, position, normal, sx + cosS, sy + sinS, sz, PETAL_BODY_R, PETAL_BODY_G, PETAL_BODY_B, shardAlpha * 0.85F);
            vertex(consumer, position, normal, sx - sinS, sy + cosS, sz, PETAL_BODY_R, PETAL_BODY_G, PETAL_BODY_B, shardAlpha * 0.85F);
        }
    }

    // =========================================================================
    // 5. Center Glowing Golden Stamen (เกสร)
    // =========================================================================

    private void renderCenterStamen(float time, float height, float scale, float alpha,
                                    Matrix4f position, Matrix3f normal, VertexConsumer consumer) {
        if (alpha <= 0.005F) return;

        float coreRadius = 0.09F * scale;
        float h = height * scale;

        // Central glowing core point
        for (int i = 0; i < 4; i++) {
            float angle = (float) (Math.PI * 0.5 * i + time * 0.05F);
            float next = angle + (float) (Math.PI * 0.5);
            vertex(consumer, position, normal, 0.0F, 0.08F * scale, 0.0F,
                    STAMEN_TIP_R, STAMEN_TIP_G, STAMEN_TIP_B, alpha);
            vertex(consumer, position, normal, Mth.cos(angle) * coreRadius, 0.16F * scale, Mth.sin(angle) * coreRadius,
                    STAMEN_R, STAMEN_G, STAMEN_B, alpha * 0.8F);
            vertex(consumer, position, normal, 0.0F, 0.24F * scale, 0.0F,
                    STAMEN_TIP_R, STAMEN_TIP_G, STAMEN_TIP_B, alpha);
            vertex(consumer, position, normal, Mth.cos(next) * coreRadius, 0.16F * scale, Mth.sin(next) * coreRadius,
                    STAMEN_R, STAMEN_G, STAMEN_B, alpha * 0.8F);
        }

        // Stamen Filaments radiating upright with glowing tips
        float filamentRadius = STAMEN_RADIUS * scale;
        for (int i = 0; i < STAMEN_COUNT; i++) {
            float theta = (float) (2.0 * Math.PI * i / STAMEN_COUNT) + time * 0.02F;
            float cos = Mth.cos(theta);
            float sin = Mth.sin(theta);

            float bx = cos * filamentRadius;
            float bz = sin * filamentRadius;
            float by = 0.10F * scale;

            // Filaments flare slightly outward near the tip
            float flare = filamentRadius * 1.5F;
            float tx = cos * flare;
            float tz = sin * flare;
            float ty = by + h;

            // Filament stalk line quad
            float halfW = 0.014F * scale;
            vertex(consumer, position, normal, bx - halfW, by, bz, STAMEN_R, STAMEN_G, STAMEN_B, alpha * 0.7F);
            vertex(consumer, position, normal, bx + halfW, by, bz, STAMEN_R, STAMEN_G, STAMEN_B, alpha * 0.7F);
            vertex(consumer, position, normal, tx + halfW, ty, tz, STAMEN_TIP_R, STAMEN_TIP_G, STAMEN_TIP_B, alpha);
            vertex(consumer, position, normal, tx - halfW, ty, tz, STAMEN_TIP_R, STAMEN_TIP_G, STAMEN_TIP_B, alpha);

            // Stamen Pollen Tip Diamond
            float tipSize = STAMEN_TIP_SIZE * scale;
            vertex(consumer, position, normal, tx - tipSize, ty, tz, STAMEN_TIP_R, STAMEN_TIP_G, STAMEN_TIP_B, alpha);
            vertex(consumer, position, normal, tx, ty + tipSize, tz, STAMEN_TIP_R, STAMEN_TIP_G, STAMEN_TIP_B, alpha);
            vertex(consumer, position, normal, tx + tipSize, ty, tz, STAMEN_TIP_R, STAMEN_TIP_G, STAMEN_TIP_B, alpha);
            vertex(consumer, position, normal, tx, ty - tipSize, tz, STAMEN_TIP_R, STAMEN_TIP_G, STAMEN_TIP_B, alpha);
        }
    }

    // =========================================================================
    // Utilities & Fast Math
    // =========================================================================

    private static float easeOutCubic(float p) {
        float inv = 1.0F - p;
        return 1.0F - inv * inv * inv;
    }

    private static float smootherstep01(float p) {
        float t = Mth.clamp(p, 0.0F, 1.0F);
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    private static float smootherstep(float edge0, float edge1, float x) {
        return smootherstep01((x - edge0) / (edge1 - edge0));
    }

    private static float rippleFade(float progress) {
        float appear = smootherstep(0.0F, 0.07F, progress);
        float fade = 1.0F - smootherstep(0.10F, 1.0F, progress);
        return appear * fade;
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f position, Matrix3f normal,
                               float x, float y, float z, float r, float g, float b, float alpha) {
        consumer.vertex(position, x, y, z)
                .color(r, g, b, alpha)
                .uv(0.5F, 0.5F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
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
