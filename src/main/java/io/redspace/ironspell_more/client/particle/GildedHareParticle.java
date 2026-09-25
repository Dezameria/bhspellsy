package io.redspace.ironspell_more.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * Custom particle rendering the cute golden hare icon when attacks/punches land during Gilded Hare stance.
 * Scatters and leaps outward like a playful golden hare, gently bobbing and fading.
 */
public class GildedHareParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final boolean mirrored;
    private final float hopPhase;

    public GildedHareParticle(ClientLevel level, double x, double y, double z,
                              double xSpeed, double ySpeed, double zSpeed,
                              SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.lifetime = 16 + this.random.nextInt(12);
        this.gravity = 0.02F;
        this.friction = 0.94F;
        this.hasPhysics = false;
        this.mirrored = xSpeed < 0;
        this.hopPhase = this.random.nextFloat() * Mth.TWO_PI;
        this.roll = (this.random.nextFloat() - 0.5F) * 0.35F;
        this.oRoll = this.roll;
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.scale(0.35F + this.random.nextFloat() * 0.18F);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        super.tick();
        if (this.removed) {
            return;
        }

        // Playful subtle hop bobbing
        float bob = Mth.sin(this.hopPhase + this.age * 0.35F) * 0.003F;
        this.yd += bob;

        // Smooth fade-in (first 2 ticks) and fade-out (last 40% of lifetime)
        float life = (float) this.age / (float) this.lifetime;
        float fadeIn = Mth.clamp((float) this.age / 2.0F, 0.0F, 1.0F);
        float fadeOut = life > 0.6F ? (1.0F - life) / 0.4F : 1.0F;
        this.alpha = fadeIn * Mth.clamp(fadeOut, 0.0F, 1.0F);

        this.setSpriteFromAge(this.sprites);
    }

    @Override
    protected float getU0() {
        return this.mirrored ? super.getU1() : super.getU0();
    }

    @Override
    protected float getU1() {
        return this.mirrored ? super.getU0() : super.getU1();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new GildedHareParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
