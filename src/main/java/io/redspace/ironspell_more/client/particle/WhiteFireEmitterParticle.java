package io.redspace.ironspell_more.client.particle;

import io.redspace.ironspell_more.registry.ParticleRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class WhiteFireEmitterParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    public WhiteFireEmitterParticle(ClientLevel level, double xCoord, double yCoord, double zCoord, SpriteSet spriteSet, double xd, double yd, double zd) {
        super(level, xCoord, yCoord, zCoord, xd, yd, zd);

        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.scale(this.random.nextFloat() * 1.75f + 1f);
        this.friction -= this.random.nextFloat() * .1;
        this.lifetime = 10 + (int) (Math.random() * 25);
        this.sprites = spriteSet;
        this.setSpriteFromAge(spriteSet);
        this.gravity = -0.01F;
    }

    @Override
    public void tick() {
        super.tick();
        this.xd += this.random.nextFloat() / 500.0F * (float) (this.random.nextBoolean() ? 1 : -1);
        this.zd += this.random.nextFloat() / 500.0F * (float) (this.random.nextBoolean() ? 1 : -1);

        animateContinuously();

        // 25% chance in each tick to drop a white ember trailing behind
        if (this.random.nextFloat() <= .25f) {
            this.level.addParticle(ParticleRegistry.WHITE_EMBER.get(), this.x, this.y, this.z, this.xd, this.yd, this.zd);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet spriteSet) {
            this.sprites = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType particleType, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new WhiteFireEmitterParticle(level, x, y, z, this.sprites, dx, dy, dz);
        }
    }

    @Override
    public int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    private void animateContinuously() {
        if (this.age % 4 == 0) {
            this.setSprite(this.sprites.get(this.random));
        }
    }
}
