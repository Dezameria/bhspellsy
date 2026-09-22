package io.redspace.ironspell_more.effect;

import io.redspace.ironspell_more.spells.ground.TigershadeTerrabreakSpell;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

public class TigershadeMarkEffect extends MobEffect {
    public static final String MARK_CASTER_UUID_TAG = "TigershadeMarkCasterUUID";
    public static final String MARK_CASTER_DIM_TAG = "TigershadeMarkCasterDimension";

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
        TigershadeTerrabreakSpell.onMarkRemoved(entity);
    }
}
