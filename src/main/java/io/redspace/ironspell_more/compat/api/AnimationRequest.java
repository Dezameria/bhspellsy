package io.redspace.ironspell_more.compat.api;

import net.minecraft.world.entity.LivingEntity;

/**
 * Parameter object for requesting animation playback on an entity.
 */
public record AnimationRequest(
    LivingEntity entity,
    AnimationCue cue,
    float transitionDuration,
    boolean synchronize
) {
    public static AnimationRequest of(LivingEntity entity, AnimationCue cue) {
        return new AnimationRequest(entity, cue, 0.0F, true);
    }

    public static AnimationRequest of(LivingEntity entity, AnimationCue cue, float transitionDuration) {
        return new AnimationRequest(entity, cue, transitionDuration, true);
    }
}
