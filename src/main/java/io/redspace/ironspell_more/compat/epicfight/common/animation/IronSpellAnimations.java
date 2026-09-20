package io.redspace.ironspell_more.compat.epicfight.common.animation;

import io.redspace.ironspell_more.compat.epicfight.skills.blazing_chakra.BlazingChakraAnimations;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.compat.api.AnimationCue;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.gameasset.Animations;

import java.util.EnumMap;
import java.util.Map;

/**
 * Main registry and lookup for Epic Fight animations mapped to logical AnimationCue entries.
 * Delegates category-specific animations to modular animation builders.
 */
public final class IronSpellAnimations {
    private static final Map<AnimationCue, AssetAccessor<? extends StaticAnimation>> CUE_MAP = new EnumMap<>(AnimationCue.class);
    public static AnimationManager.AnimationAccessor<AttackAnimation> BLAZING_CHAKRA;
    private static boolean registered;

    private IronSpellAnimations() {
    }

    public static void registerAnimations(AnimationManager.AnimationRegistryEvent event) {
        if (registered) {
            return;
        }
        registered = true;

        event.newBuilder(IronSpellMore.MODID, builder -> {
            // Register primary Blazing Chakra path: ironspell_more:biped/spells/blazing_chakra
            BLAZING_CHAKRA = BlazingChakraAnimations.registerBlazingChakra(builder, BlazingChakraAnimations.PATH_BLAZING_CHAKRA);

            // Populate CUE_MAP inside the builder task
            if (BLAZING_CHAKRA != null) {
                CUE_MAP.put(AnimationCue.BLAZING_CHAKRA, BLAZING_CHAKRA);
            }
        });

        // Initialize fallback mappings
        CUE_MAP.put(AnimationCue.BOW_AIM, Animations.BIPED_BOW_AIM);
        CUE_MAP.put(AnimationCue.BOW_SHOOT, Animations.BIPED_BOW_SHOT);
        CUE_MAP.put(AnimationCue.AERIAL_SLAM, Animations.BIPED_DEMOLITION_LEAP);
        CUE_MAP.put(AnimationCue.WARP_PUNCH, Animations.BIPED_MOB_ONEHAND1);
        CUE_MAP.put(AnimationCue.UPPERCUT, Animations.BIPED_MOB_ONEHAND2);
        CUE_MAP.put(AnimationCue.PULL_KICK, Animations.BIPED_STEP_FORWARD);
    }

    public static AssetAccessor<? extends StaticAnimation> getAnimationForCue(AnimationCue cue) {
        AssetAccessor<? extends StaticAnimation> animation = CUE_MAP.get(cue);
        if (animation != null) {
            return animation;
        }

        if (cue == AnimationCue.BLAZING_CHAKRA) {
            if (BLAZING_CHAKRA != null) {
                CUE_MAP.put(cue, BLAZING_CHAKRA);
                return BLAZING_CHAKRA;
            }

            // Dynamic runtime lookup
            AnimationManager.AnimationAccessor<? extends StaticAnimation> byKey =
                AnimationManager.byKey(new ResourceLocation(IronSpellMore.MODID, BlazingChakraAnimations.PATH_BLAZING_CHAKRA));
            if (byKey != null) {
                CUE_MAP.put(cue, byKey);
                return byKey;
            }
        }

        return null;
    }

    public static void setCustomCueMapping(AnimationCue cue, AssetAccessor<? extends StaticAnimation> animation) {
        CUE_MAP.put(cue, animation);
    }
}
