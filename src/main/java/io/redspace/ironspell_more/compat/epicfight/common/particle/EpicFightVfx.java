package io.redspace.ironspell_more.compat.epicfight.common.particle;

import io.redspace.ironspell_more.compat.api.CompatResult;
import io.redspace.ironspell_more.compat.api.VfxCue;
import io.redspace.ironspell_more.compat.api.VfxRequest;
import io.redspace.ironspell_more.compat.epicfight.skills.blazing_chakra.BlazingChakraVfx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * Unified entry point and facade for Epic Fight visual effects.
 * Delegates to modular sub-packages:
 * - {@link io.redspace.ironspell_more.compat.epicfight.particle.common}
 * - {@link io.redspace.ironspell_more.compat.epicfight.particle.chakra}
 */
public final class EpicFightVfx {
    private EpicFightVfx() {
    }

    public static CompatResult spawnVfx(VfxRequest request) {
        if (request == null || request.level().isClientSide) {
            return CompatResult.UNSUPPORTED;
        }

        if (request.cue() == VfxCue.FRACTURE_SLAM) {
            boolean spawned = trySpawnFracture(request.source(), request.level(), request.position(), 2, 4, request.radius());
            return spawned ? CompatResult.APPLIED : CompatResult.FAILED;
        }

        return CompatResult.UNSUPPORTED;
    }

    public static boolean trySpawnFracture(LivingEntity source, Level level, Vec3 samplePosition,
            int searchUp, int searchDown, double radius) {
        return FractureVfx.trySpawnFracture(source, level, samplePosition, searchUp, searchDown, radius);
    }

    public static void spawnClientAfterimage(Level level, LivingEntity entity) {
        AfterimageVfx.spawnWhiteAfterimage(level, entity);
    }

    public static void spawnServerShockwave(ServerLevel level, LivingEntity entity) {
        GroundShockwaveVfx.spawnSpiralFireShockwave(level, entity);
    }

    public static void spawnScatterParticles(ServerLevel level, LivingEntity entity) {
        GroundShockwaveVfx.spawnSpiralFireShockwave(level, entity);
    }

    public static void spawnScatterParticles(ServerLevel level, double originX, double originY, double originZ) {
        GroundShockwaveVfx.spawnSpiralFireShockwave(level, originX, originY, originZ);
    }

    public static void spawnBlazingChakraClientVfx(LivingEntityPatch<?> patch) {
        BlazingChakraVfx.spawnImpactClientVfx(patch);
    }

}
