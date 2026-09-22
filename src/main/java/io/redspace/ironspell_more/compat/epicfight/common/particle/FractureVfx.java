package io.redspace.ironspell_more.compat.epicfight.common.particle;

import io.redspace.ironsspellbooks.api.util.CameraShakeData;
import io.redspace.ironsspellbooks.api.util.CameraShakeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.property.AnimationEvent;
import yesman.epicfight.api.utils.LevelUtil;
import yesman.epicfight.gameasset.EpicFightSounds;

/**
 * Common block fracture and ground collision visual effects.
 */
public final class FractureVfx {
    private FractureVfx() {
    }

    public static boolean trySpawnFracture(LivingEntity source, Level level, Vec3 samplePosition,
            int searchUp, int searchDown, double radius) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        BlockPos center = BlockPos.containing(samplePosition);
        BlockPos surface = findSurfaceBlock(serverLevel, center, searchUp, searchDown);
        if (surface == null) {
            return false;
        }

        BlockState state = serverLevel.getBlockState(surface);
        if (state.isAir() || state.getRenderShape() == RenderShape.INVISIBLE) {
            return false;
        }

        Vec3 fractureCenter = Vec3.atCenterOf(surface);
        boolean spawned = LevelUtil.circleSlamFracture(source, serverLevel, fractureCenter, radius,
                true, false, false);
        if (!spawned) {
            return false;
        }

        serverLevel.playSound(null, fractureCenter.x, surface.getY() + 1.0D, fractureCenter.z,
                EpicFightSounds.SLAM_LIGHT.get(), SoundSource.BLOCKS, 0.2F,
                0.95F + serverLevel.getRandom().nextFloat() * 0.1F);
        return spawned;
    }

    public static void triggerCameraShake(LivingEntity entity, int durationTicks, float radius) {
        if (entity.level().isClientSide) {
            CameraShakeManager.addCameraShake(new CameraShakeData(durationTicks, entity.position(), radius));
        }
    }

    public static AnimationEvent.InTimeEvent createGroundFractureEvent(float time, int searchUp, int searchDown, double radius) {
        return AnimationEvent.InTimeEvent.create(time, (patch, anim, params) -> {
            LivingEntity entity = patch.getOriginal();
            trySpawnFracture(entity, entity.level(), entity.position(), searchUp, searchDown, radius);
        }, AnimationEvent.Side.SERVER);
    }

    private static BlockPos findSurfaceBlock(ServerLevel level, BlockPos center, int searchUp, int searchDown) {
        for (int dy = searchUp; dy >= -searchDown; dy--) {
            BlockPos checkPos = center.above(dy);
            BlockState state = level.getBlockState(checkPos);
            BlockPos abovePos = checkPos.above();
            BlockState aboveState = level.getBlockState(abovePos);

            if (!state.isAir() && state.isSolid() && (aboveState.isAir() || !aboveState.isSolid())) {
                return checkPos;
            }
        }
        return null;
    }
}
