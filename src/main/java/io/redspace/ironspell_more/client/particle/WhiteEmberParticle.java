package io.redspace.ironspell_more.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public class WhiteEmberParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final boolean mirrored;

    public WhiteEmberParticle(ClientLevel level, double x, double y, double z,
                              SpriteSet sprites, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.scale(1.0F + this.random.nextFloat() * 1.75F);
        this.lifetime = 4 + (int) (Math.random() * 11.0);
        this.sprites = sprites;
        this.setSpriteFromAge(sprites);
        this.gravity = -0.1F;
        this.friction = 0.85F;
        this.mirrored = this.random.nextBoolean();
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
    }

    @Override
    public void tick() {
        super.tick();
        this.xd += (this.random.nextBoolean() ? 1 : -1) * (this.random.nextFloat() / 100.0F);
        this.yd += (this.random.nextFloat() / 100.0F);
        this.zd += (this.random.nextBoolean() ? 1 : -1) * (this.random.nextFloat() / 100.0F);
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
        return 15728880; // Full brightness / glow
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
            return new WhiteEmberParticle(level, x, y, z, this.sprites, xSpeed, ySpeed, zSpeed);
        }
    }
}
