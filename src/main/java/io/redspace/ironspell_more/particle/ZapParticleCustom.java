package io.redspace.ironspell_more.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class ZapParticleCustom extends TextureSheetParticle {
    private final Vec3 destination;
    private final float scaleMultiplier;
    private final ParticleRenderType PARTICLE_EMISSIVE;

    public ZapParticleCustom(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed,
            double zSpeed, ZapParticleOptionCustom options) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.destination = options.getDestination();
        this.scaleMultiplier = options.getScale();
        this.setSize(1.0f, 1.0f);
        this.quadSize = 1.0f;
        this.lifetime = 6;

        this.PARTICLE_EMISSIVE = new ParticleRenderType() {
            @Override
            public void begin(BufferBuilder pBuilder, TextureManager pTextureManager) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.depthMask(false);
                RenderSystem.setShader(GameRenderer::getParticleShader);
                RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
                pBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
            }

            @Override
            public void end(Tesselator pTesselator) {
                pTesselator.end();
            }
        };
    }

    @Override
    public boolean shouldCull() {
        return false;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return PARTICLE_EMISSIVE;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 15728880;
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTick) {
        Vec3 cameraPos = camera.getPosition();
        float startX = (float) (Mth.lerp(partialTick, this.xo, this.x) - cameraPos.x());
        float startY = (float) (Mth.lerp(partialTick, this.yo, this.y) - cameraPos.y());
        float startZ = (float) (Mth.lerp(partialTick, this.zo, this.z) - cameraPos.z());

        float destX = (float) (this.destination.x() - cameraPos.x());
        float destY = (float) (this.destination.y() - cameraPos.y());
        float destZ = (float) (this.destination.z() - cameraPos.z());

        Vector3f startVec = new Vector3f(startX, startY, startZ);
        Vector3f destVec = new Vector3f(destX, destY, destZ);

        // Dynamic time-based Random Seed เพื่อให้สายฟ้าขยับสั่นสลับรูปร่างหยึกหยักๆ
        // อย่างรวดเร็วในทุกๆ เฟรม
        long dynamicSeed = (long) (this.age * 31 + (long) (partialTick * 40.0f) + (System.currentTimeMillis() / 25L));
        RandomSource random = RandomSource.create(dynamicSeed);
        draw3DLightningTube(consumer, startVec, destVec, random);
    }

    private void draw3DLightningTube(VertexConsumer consumer, Vector3f start, Vector3f dest, RandomSource random) {
        int segments = 12; // 12 รอยหักหยึกหยัก
        Vector3f current = new Vector3f(start);
        Vector3f step = new Vector3f(dest).sub(start).mul(1.0f / segments);

        // Render Layer 1: Core 3D Square Tube (Soft Pink / White Core)
        tube(consumer, current, step, segments, 0.05f * scaleMultiplier, random, 1.0f, 0.75f, 0.9f, 1.0f);

        // Render Layer 2: Glow 3D Square Tube (Hot Pink / Magenta Glow)
        tube(consumer, current, step, segments, 0.13f * scaleMultiplier, random, 1.0f, 0.2f, 0.7f, 0.45f);
    }

    private void tube(VertexConsumer consumer, Vector3f start, Vector3f step, int segments, float width,
            RandomSource random, float r, float g, float b, float a) {
        Vector3f current = new Vector3f(start);
        float radius = width * 0.5f;

        for (int i = 0; i < segments; i++) {
            Vector3f next = new Vector3f(current).add(step);
            if (i < segments - 1) {
                // ระยะสะบัดหยึกหยัก (Jitter Offset) - ปรับลดจาก 0.28f เหลือ 0.10f
                // เพื่อให้ลำแสงเกาะกลุ่มเป็นเส้นเดียวพุ่งตรง
                float jitter = 0.10f * scaleMultiplier;
                next.add(
                        (random.nextFloat() - 0.5f) * jitter,
                        (random.nextFloat() - 0.5f) * jitter,
                        (random.nextFloat() - 0.5f) * jitter);
            }

            draw3DTubeSegment(consumer, current, next, radius, r, g, b, a);
            current = next;
        }
    }

    private void draw3DTubeSegment(VertexConsumer consumer, Vector3f p1, Vector3f p2, float radius, float r, float g,
            float b, float a) {
        Vector3f dir = new Vector3f(p2).sub(p1);
        float lenSq = dir.lengthSquared();
        if (lenSq < 0.0001f)
            return;
        dir.normalize();

        Vector3f ref = Math.abs(dir.y()) > 0.9f ? new Vector3f(1.0f, 0.0f, 0.0f) : new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f right = new Vector3f(dir).cross(ref).normalize().mul(radius);
        Vector3f up = new Vector3f(dir).cross(right).normalize().mul(radius);

        // 4 corners around start point p1
        Vector3f c1_p1 = new Vector3f(p1).add(right).add(up);
        Vector3f c2_p1 = new Vector3f(p1).sub(right).add(up);
        Vector3f c3_p1 = new Vector3f(p1).sub(right).sub(up);
        Vector3f c4_p1 = new Vector3f(p1).add(right).sub(up);

        // 4 corners around end point p2
        Vector3f c1_p2 = new Vector3f(p2).add(right).add(up);
        Vector3f c2_p2 = new Vector3f(p2).sub(right).add(up);
        Vector3f c3_p2 = new Vector3f(p2).sub(right).sub(up);
        Vector3f c4_p2 = new Vector3f(p2).add(right).sub(up);

        int light = 15728880;

        // Face 1: Top
        makeQuad(consumer, c1_p1, c2_p1, c2_p2, c1_p2, r, g, b, a, light);
        // Face 2: Left
        makeQuad(consumer, c2_p1, c3_p1, c3_p2, c2_p2, r, g, b, a, light);
        // Face 3: Bottom
        makeQuad(consumer, c3_p1, c4_p1, c4_p2, c3_p2, r, g, b, a, light);
        // Face 4: Right
        makeQuad(consumer, c4_p1, c1_p1, c1_p2, c4_p2, r, g, b, a, light);
    }

    private void makeQuad(VertexConsumer consumer, Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4, float r, float g,
            float b, float a, int light) {
        makeCornerVertex(consumer, v1, getU0(), getV0(), r, g, b, a, light);
        makeCornerVertex(consumer, v2, getU1(), getV0(), r, g, b, a, light);
        makeCornerVertex(consumer, v3, getU1(), getV1(), r, g, b, a, light);
        makeCornerVertex(consumer, v4, getU0(), getV1(), r, g, b, a, light);
    }

    private void makeCornerVertex(VertexConsumer consumer, Vector3f pos, float u, float v, float r, float g, float b,
            float a, int light) {
        consumer.vertex(pos.x(), pos.y(), pos.z())
                .uv(u, v)
                .color(r, g, b, a)
                .uv2(light)
                .endVertex();
    }

    public static class Provider implements ParticleProvider<ZapParticleOptionCustom> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(ZapParticleOptionCustom options, ClientLevel level, double x, double y, double z,
                double xSpeed, double ySpeed, double zSpeed) {
            ZapParticleCustom particle = new ZapParticleCustom(level, x, y, z, xSpeed, ySpeed, zSpeed, options);
            particle.pickSprite(this.spriteSet);
            return particle;
        }
    }
}
