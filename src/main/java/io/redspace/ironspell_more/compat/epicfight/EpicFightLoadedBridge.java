package io.redspace.ironspell_more.compat.epicfight;

import io.redspace.ironspell_more.compat.api.AnimationRequest;
import io.redspace.ironspell_more.compat.api.CompatResult;
import io.redspace.ironspell_more.compat.api.VfxRequest;
import io.redspace.ironspell_more.compat.epicfight.common.animation.EpicFightAnimationPlayer;
import io.redspace.ironspell_more.compat.epicfight.common.animation.IronSpellAnimations;
import io.redspace.ironspell_more.compat.epicfight.common.particle.EpicFightVfx;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import yesman.epicfight.api.animation.AnimationManager;

/**
 * Internal loaded bridge for direct Epic Fight interactions.
 * Loaded ONLY after confirming epicfight is present in ModList.
 */
final class EpicFightLoadedBridge {
    private EpicFightLoadedBridge() {
    }

    static void registerModEvents(IEventBus modEventBus) {
        modEventBus.addListener(IronSpellAnimations::registerAnimations);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> {
                    io.redspace.ironspell_more.compat.epicfight.client.GildedHareEpicFightRenderCompat
                            .registerModEvents(modEventBus);
                    io.redspace.ironspell_more.compat.epicfight.client.JadeAuraEpicFightRenderCompat
                            .registerModEvents(modEventBus);
                });
    }

    static CompatResult playAnimation(AnimationRequest request) {
        return EpicFightAnimationPlayer.play(request);
    }

    static boolean trySpawnFracture(LivingEntity source, Level level, Vec3 samplePosition,
            int searchUp, int searchDown, double radius) {
        return EpicFightVfx.trySpawnFracture(source, level, samplePosition, searchUp, searchDown, radius);
    }

    static CompatResult spawnVfx(VfxRequest request) {
        return EpicFightVfx.spawnVfx(request);
    }

    static void spawnScatterParticles(net.minecraft.server.level.ServerLevel level, LivingEntity entity) {
        EpicFightVfx.spawnScatterParticles(level, entity);
    }

    static void spawnScatterParticles(net.minecraft.server.level.ServerLevel level, double x, double y, double z) {
        EpicFightVfx.spawnScatterParticles(level, x, y, z);
    }

    static boolean isBattleMode(LivingEntity entity) {
        return io.redspace.ironspell_more.compat.epicfight.client.EpicFightLegPoseHelper.isBattleMode(entity);
    }

    static Vec3 getLegJointWorldPos(LivingEntity entity, boolean isLeft) {
        return io.redspace.ironspell_more.compat.epicfight.client.EpicFightLegPoseHelper.getLegJointWorldPos(entity, isLeft);
    }
}
