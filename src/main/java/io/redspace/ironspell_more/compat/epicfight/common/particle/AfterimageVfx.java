package io.redspace.ironspell_more.compat.epicfight.common.particle;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import yesman.epicfight.particle.EpicFightParticles;

/**
 * Reusable client-side afterimage particle generator.
 */
public final class AfterimageVfx {
    private AfterimageVfx() {
    }

    public static void spawnWhiteAfterimage(Level level, LivingEntity entity) {
        if (level == null || entity == null) {
            return;
        }
        level.addParticle(
            EpicFightParticles.WHITE_AFTERIMAGE.get(),
            entity.getX(), entity.getY(), entity.getZ(),
            Double.longBitsToDouble(entity.getId()), 0.0D, 0.0D
        );
    }
}
