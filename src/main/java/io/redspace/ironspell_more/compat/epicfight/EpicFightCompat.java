package io.redspace.ironspell_more.compat.epicfight;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.compat.CompatMods;
import io.redspace.ironspell_more.compat.api.AnimationCue;
import io.redspace.ironspell_more.compat.api.AnimationRequest;
import io.redspace.ironspell_more.compat.api.CompatResult;
import io.redspace.ironspell_more.compat.api.VfxCue;
import io.redspace.ironspell_more.compat.api.VfxRequest;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Public safe facade for Epic Fight integration.
 * Safe to call anywhere from spell code or common logic.
 */
public final class EpicFightCompat {
    private static boolean linkageFailed;

    private EpicFightCompat() {
    }

    public static boolean isAvailable() {
        return CompatMods.isEpicFightLoaded() && !linkageFailed;
    }

    public static void registerModEvents(IEventBus modEventBus) {
        if (!CompatMods.isEpicFightLoaded() || linkageFailed) {
            return;
        }

        try {
            EpicFightLoadedBridge.registerModEvents(modEventBus);
        } catch (LinkageError error) {
            linkageFailed = true;
            IronSpellMore.LOGGER.error("Epic Fight event registration failed due to linkage error", error);
        }
    }

    public static CompatResult playAnimation(AnimationRequest request) {
        if (!isAvailable() || request == null || request.entity() == null) {
            return CompatResult.UNAVAILABLE;
        }

        try {
            return EpicFightLoadedBridge.playAnimation(request);
        } catch (LinkageError error) {
            linkageFailed = true;
            IronSpellMore.LOGGER.error("Epic Fight animation playback failed due to linkage error", error);
            return CompatResult.FAILED;
        }
    }

    public static CompatResult playAnimation(LivingEntity entity, AnimationCue cue) {
        return playAnimation(AnimationRequest.of(entity, cue));
    }

    public static CompatResult playAnimation(LivingEntity entity, AnimationCue cue, float transitionDuration) {
        return playAnimation(AnimationRequest.of(entity, cue, transitionDuration));
    }

    public static boolean spawnFracture(LivingEntity source, Level level, Vec3 samplePosition,
            int searchUp, int searchDown, double radius) {
        if (!isAvailable() || level.isClientSide || source == null) {
            return false;
        }

        try {
            return EpicFightLoadedBridge.trySpawnFracture(source, level, samplePosition, searchUp, searchDown, radius);
        } catch (LinkageError error) {
            linkageFailed = true;
            IronSpellMore.LOGGER.error("Epic Fight fracture spawn failed due to linkage error", error);
            return false;
        }
    }

    public static CompatResult spawnVfx(VfxRequest request) {
        if (!isAvailable() || request == null) {
            return CompatResult.UNAVAILABLE;
        }

        try {
            return EpicFightLoadedBridge.spawnVfx(request);
        } catch (LinkageError error) {
            linkageFailed = true;
            IronSpellMore.LOGGER.error("Epic Fight VFX spawn failed due to linkage error", error);
            return CompatResult.FAILED;
        }
    }

    public static CompatResult spawnVfx(LivingEntity source, Vec3 position, double radius, VfxCue cue) {
        return spawnVfx(VfxRequest.of(source, position, radius, cue));
    }

    public static void spawnScatterParticles(net.minecraft.server.level.ServerLevel level, LivingEntity entity) {
        if (!isAvailable() || level == null || entity == null) {
            return;
        }

        try {
            EpicFightLoadedBridge.spawnScatterParticles(level, entity);
        } catch (LinkageError error) {
            linkageFailed = true;
            IronSpellMore.LOGGER.error("Epic Fight scatter particles failed due to linkage error", error);
        }
    }

    public static void spawnScatterParticles(net.minecraft.server.level.ServerLevel level, double x, double y, double z) {
        if (!isAvailable() || level == null) {
            return;
        }

        try {
            EpicFightLoadedBridge.spawnScatterParticles(level, x, y, z);
        } catch (LinkageError error) {
            linkageFailed = true;
            IronSpellMore.LOGGER.error("Epic Fight scatter particles failed due to linkage error", error);
        }
    }

    public static boolean isBattleMode(LivingEntity entity) {
        if (!isAvailable() || entity == null) {
            return false;
        }

        try {
            return EpicFightLoadedBridge.isBattleMode(entity);
        } catch (LinkageError error) {
            linkageFailed = true;
            IronSpellMore.LOGGER.error("Epic Fight battle mode check failed due to linkage error", error);
            return false;
        }
    }

    public static Vec3 getLegJointWorldPos(LivingEntity entity, boolean isLeft) {
        if (!isAvailable() || entity == null) {
            return null;
        }

        try {
            return EpicFightLoadedBridge.getLegJointWorldPos(entity, isLeft);
        } catch (LinkageError error) {
            linkageFailed = true;
            IronSpellMore.LOGGER.error("Epic Fight leg joint lookup failed due to linkage error", error);
            return null;
        }
    }
}
