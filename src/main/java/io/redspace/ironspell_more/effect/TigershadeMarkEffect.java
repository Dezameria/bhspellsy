package io.redspace.ironspell_more.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

public class TigershadeMarkEffect extends MobEffect {
    public static final String MARK_CASTER_UUID_TAG = "TigershadeMarkCasterUUID";

    public TigershadeMarkEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);
        entity.getPersistentData().remove(MARK_CASTER_UUID_TAG);
    }
}
