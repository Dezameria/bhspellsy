package io.redspace.ironspell_more.client.particle;

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
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class ShockwaveParticleCustom extends TextureSheetParticle {
    private final float maxRadius;
    private final boolean isFullbright;
    private final Vector3f direction;
    private final ParticleRenderType PARTICLE_EMISSIVE;

    public ShockwaveParticleCustom(ClientLevel level, double x, double y, double z, ShockwaveParticleOptionCustom options) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.maxRadius = options.getRadius();
        this.isFullbright = options.isFullbright();
        this.direction = new Vector3f(options.getDirection()).normalize();
        this.rCol = options.getColor().x;
        this.gCol = options.getColor().y;
        this.bCol = options.getColor().z;
        this.lifetime = 10;
        this.quadSize = 0.5f;

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
        return isFullbright ? 15728880 : super.getLightColor(partialTick);
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTick) {
        Vec3 cameraPos = camera.getPosition();
        float posX = (float) (Mth.lerp(partialTick, this.xo, this.x) - cameraPos.x());
        float posY = (float) (Mth.lerp(partialTick, this.yo, this.y) - cameraPos.y());
        float posZ = (float) (Mth.lerp(partialTick, this.zo, this.z) - cameraPos.z());

        float progress = ((float) this.age + partialTick) / (float) this.lifetime;
        float currentRadius = Mth.lerp(progress, 0.4f, maxRadius);
        this.alpha = Mth.clamp(1.0f - progress, 0.0f, 1.0f);

        // คำนวณเวกเตอร์ตั้งฉาก 2 แกน (Right, Up) ตามทิศทางทิศสายตา (direction) เพื่อตั้งแผ่นวงกลม Shockwave ตั้งฉากตรงไปหาหน้าผู้ร่าย 100%
        Vector3f dir = new Vector3f(this.direction);
        if (dir.lengthSquared() < 0.0001f) {
            dir = new Vector3f(0.0f, 0.0f, 1.0f);
        }
        Vector3f ref = Math.abs(dir.y()) > 0.99f ? new Vector3f(1.0f, 0.0f, 0.0f) : new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f right = new Vector3f(dir).cross(ref).normalize().mul(currentRadius);
        Vector3f up = new Vector3f(dir).cross(right).normalize().mul(currentRadius);

        Vector3f pos = new Vector3f(posX, posY, posZ);

        int light = getLightColor(partialTick);

        // Layer 1: Inner Core Shockwave Ring
        Vector3f p1 = new Vector3f(pos).add(right).add(up);
        Vector3f p2 = new Vector3f(pos).sub(right).add(up);
        Vector3f p3 = new Vector3f(pos).sub(right).sub(up);
        Vector3f p4 = new Vector3f(pos).add(right).sub(up);

        makeCornerVertex(consumer, p1, getU0(), getV0(), rCol, gCol, bCol, alpha, light);
        makeCornerVertex(consumer, p2, getU1(), getV0(), rCol, gCol, bCol, alpha, light);
        makeCornerVertex(consumer, p3, getU1(), getV1(), rCol, gCol, bCol, alpha, light);
        makeCornerVertex(consumer, p4, getU0(), getV1(), rCol, gCol, bCol, alpha, light);

        // Layer 2: Outer Glow Ripple Ring (ขยายตามหลังเพิ่มความสวยงามเหมือน Shockwave Spell ออริจินัล)
        Vector3f rightGlow = new Vector3f(right).mul(1.22f);
        Vector3f upGlow = new Vector3f(up).mul(1.22f);
        Vector3f gp1 = new Vector3f(pos).add(rightGlow).add(upGlow);
        Vector3f gp2 = new Vector3f(pos).sub(rightGlow).add(upGlow);
        Vector3f gp3 = new Vector3f(pos).sub(rightGlow).sub(upGlow);
        Vector3f gp4 = new Vector3f(pos).add(rightGlow).sub(upGlow);

        float glowAlpha = alpha * 0.45f;
        makeCornerVertex(consumer, gp1, getU0(), getV0(), rCol, gCol, bCol, glowAlpha, light);
        makeCornerVertex(consumer, gp2, getU1(), getV0(), rCol, gCol, bCol, glowAlpha, light);
        makeCornerVertex(consumer, gp3, getU1(), getV1(), rCol, gCol, bCol, glowAlpha, light);
        makeCornerVertex(consumer, gp4, getU0(), getV1(), rCol, gCol, bCol, glowAlpha, light);
    }

    private void makeCornerVertex(VertexConsumer consumer, Vector3f pos, float u, float v, float r, float g, float b, float a, int light) {
        consumer.vertex(pos.x(), pos.y(), pos.z())
                .uv(u, v)
                .color(r, g, b, a)
                .uv2(light)
                .endVertex();
    }

    public static class Provider implements ParticleProvider<ShockwaveParticleOptionCustom> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(ShockwaveParticleOptionCustom options, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            ShockwaveParticleCustom particle = new ShockwaveParticleCustom(level, x, y, z, options);
            particle.pickSprite(this.spriteSet);
            return particle;
        }
    }
}
