package io.redspace.ironspell_more.compat.epicfight;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Backward compatibility facade for fracture spawning.
 * Delegates to {@link EpicFightCompat}.
 */
public final class EpicFightFractureHelper {
    private EpicFightFractureHelper() {
    }

    public static boolean trySpawnFracture(LivingEntity source, Level level, Vec3 samplePosition,
            int searchUp, int searchDown, double radius) {
        return EpicFightCompat.spawnFracture(source, level, samplePosition, searchUp, searchDown, radius);
    }

    public static void spawnScatterParticles(net.minecraft.server.level.ServerLevel level, LivingEntity entity) {
        EpicFightCompat.spawnScatterParticles(level, entity);
    }

    public static void spawnScatterParticles(net.minecraft.server.level.ServerLevel level, double x, double y, double z) {
        EpicFightCompat.spawnScatterParticles(level, x, y, z);
    }
}
