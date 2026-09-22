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
 * A lightweight, full-bright plum petal that curls through the air while its
 * sprite spins. Initial velocity still controls whether it gently blooms
 * outward or bursts away at high speed.
 */
public class RedPlumParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float spinSpeed;
    private final float curlSpeed;
    private final float flutterPhase;
    private final boolean mirrored;

    protected RedPlumParticle(ClientLevel level, double x, double y, double z,
                              double xSpeed, double ySpeed, double zSpeed,
                              SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.lifetime = 30 + this.random.nextInt(19);
        this.gravity = 0.018F;
        this.friction = 0.96F;
        this.hasPhysics = false;
        this.mirrored = this.random.nextBoolean();
        this.spinSpeed = (this.random.nextBoolean() ? 1.0F : -1.0F)
                * (0.10F + this.random.nextFloat() * 0.12F);
        double horizontalSpeed = Math.sqrt(xSpeed * xSpeed + zSpeed * zSpeed);
        // Ambient petals curve strongly in one shared orbit direction. Burst
        // petals keep only a slight curve so their radial explosion stays clear.
        this.curlSpeed = horizontalSpeed > 0.15D
                ? 0.010F + this.random.nextFloat() * 0.010F
                : 0.040F + this.random.nextFloat() * 0.020F;
        this.flutterPhase = this.random.nextFloat() * Mth.TWO_PI;
        this.roll = this.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        this.scale(0.90F + this.random.nextFloat() * 0.55F);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        super.tick();
        if (this.removed) {
            return;
        }

        // Rotate horizontal velocity a little every tick to create a smooth
        // curling path instead of a straight leaf-like projectile.
        float cos = Mth.cos(this.curlSpeed);
        float sin = Mth.sin(this.curlSpeed);
        double curvedX = this.xd * cos - this.zd * sin;
        double curvedZ = this.xd * sin + this.zd * cos;
        float flutter = Mth.sin(this.flutterPhase + this.age * 0.42F);
        this.xd = curvedX + Mth.cos(this.flutterPhase + this.age * 0.31F) * 0.0018F;
        this.yd += flutter * 0.0015F;
        this.zd = curvedZ + Mth.sin(this.flutterPhase + this.age * 0.31F) * 0.0018F;
        this.roll += this.spinSpeed * (0.75F + 0.25F * Math.abs(flutter));

        float life = (float) this.age / (float) this.lifetime;
        float fadeIn = Mth.clamp(life / 0.10F, 0.0F, 1.0F);
        float fadeOut = Mth.clamp((1.0F - life) / 0.30F, 0.0F, 1.0F);
        this.alpha = Math.min(fadeIn, fadeOut);
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
            return new RedPlumParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
