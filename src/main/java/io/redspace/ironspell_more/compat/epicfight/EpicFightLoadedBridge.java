package io.redspace.ironspell_more.compat.epicfight;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.utils.LevelUtil;

/** Loaded only after the facade confirms that Epic Fight is present. */
final class EpicFightLoadedBridge {
    private EpicFightLoadedBridge() {
    }

    static boolean trySpawnFracture(LivingEntity source, Level level, Vec3 samplePosition,
            int searchUp, int searchDown, double radius) {
        int x = (int) Math.floor(samplePosition.x);
        int z = (int) Math.floor(samplePosition.z);
        int baseY = (int) Math.floor(samplePosition.y);

        for (int dy = searchUp; dy >= -searchDown; dy--) {
            BlockPos position = new BlockPos(x, baseY + dy, z);
            BlockState state = level.getBlockState(position);
            if (LevelUtil.canTransferShockWave(level, position, state)) {
                Vec3 fracturePosition = new Vec3(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D);
                boolean spawned = LevelUtil.circleSlamFracture(source, level, fracturePosition, radius, true, false, false);
                if (spawned) {
                    level.playSound(null, fracturePosition.x, fracturePosition.y, fracturePosition.z,
                            yesman.epicfight.gameasset.EpicFightSounds.SLAM_LIGHT.get(),
                            net.minecraft.sounds.SoundSource.BLOCKS,
                            0.2F, 1.0F + (level.random.nextFloat() - 0.5F) * 0.2F);
                }
                return spawned;
            }
        }
        return false;
    }
}
