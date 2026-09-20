package io.redspace.ironspell_more.compat.epicfight.common.particle;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Common spiral and radial shockwaves that expand outward on the ground.
 */
public final class GroundShockwaveVfx {
    private GroundShockwaveVfx() {
    }

    public static void spawnSpiralFireShockwave(ServerLevel level, LivingEntity entity) {
        if (entity == null || level == null) {
            return;
        }
        spawnSpiralFireShockwave(level, entity.getX(), entity.getY() + 0.2D, entity.getZ());
    }

    public static void spawnSpiralFireShockwave(ServerLevel level, double originX, double originY, double originZ) {
        if (level == null) {
            return;
        }

        double minRadius = 10.0D;
        double maxRadius = 18.0D;
        int rings = 4;
        int particlesPerRing = 100;
        double speed = 0.5D;
        RandomSource random = level.getRandom();

        for (int r = 0; r < rings; r++) {
            double progress = (double) r / (double) (rings - 1);
            double currentRadius = Mth.lerp(progress, minRadius, maxRadius);
            double heightOffset = 1.8D * Math.sin(progress * Math.PI);

            for (int p = 0; p < particlesPerRing; p++) {
                double angle = (2.0D * Math.PI * p) / (double) particlesPerRing;
                double jitter = 0.5D * (random.nextDouble() - 0.5D);

                double x = currentRadius * Math.cos(angle + progress * 1.5D * Math.PI) + jitter;
                double y = heightOffset * Math.sin(angle * 3.0D + r * 0.7D);
                double z = currentRadius * Math.sin(angle + progress * 1.5D * Math.PI) + jitter;

                Vec3 pos = new Vec3(originX + x, originY + y, originZ + z);
                Vec3 dir = pos.subtract(originX, originY, originZ).normalize();
                Vec3 motion = dir.scale(speed * (0.4D + 0.6D * (1.0D - progress))).add(0.0D, 0.2D, 0.0D);

                level.sendParticles(
                    ParticleTypes.FLAME,
                    pos.x, pos.y, pos.z,
                    2,
                    motion.x * 0.4D, motion.y * 0.9D, motion.z * 0.4D,
                    1.0D
                );

                if (random.nextDouble() < 0.5D) {
                    level.sendParticles(
                        ParticleTypes.SMOKE,
                        pos.x, pos.y + 0.3D, pos.z,
                        1,
                        motion.x * 1.5D, motion.y * 2.0D, motion.z * 1.5D,
                        0.5D
                    );
                }
            }
        }

        level.sendParticles(ParticleTypes.LARGE_SMOKE, originX, originY + 0.5D, originZ, 30, 1.8D, 0.8D, 1.8D, 0.9D);
        level.sendParticles(ParticleTypes.FLAME, originX, originY + 0.5D, originZ, 80, 2.0D, 1.0D, 2.0D, 0.8D);
    }
}
