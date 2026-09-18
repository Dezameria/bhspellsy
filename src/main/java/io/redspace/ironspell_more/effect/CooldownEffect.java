package io.redspace.ironspell_more.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Data-only marker effect. Its amplifier records the resolved outcome of
 * Venomous Blossomfall for other systems to inspect.
 */
public final class CooldownEffect extends MobEffect {
    public CooldownEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
