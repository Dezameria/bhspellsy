package io.redspace.ironspell_more.effect;

import io.redspace.ironspell_more.client.particle.GildedHareVfx;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class GildedHareEffect extends MobEffect {
    public static final String CASTER_ACTIVE_TARGET_TAG = "GildedHareActiveTarget";
    public static final float ATTACK_SPEED_MODIFIER = 0.10F; // Attack speed +10%

    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("6a9c1e48-3c4a-4d7e-971a-82dc3a5b6c7d");

    public GildedHareEffect(MobEffectCategory category, int color) {
        super(category, color);
        this.addAttributeModifier(Attributes.ATTACK_SPEED, ATTACK_SPEED_UUID.toString(),
                ATTACK_SPEED_MODIFIER, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Emit subtle golden dust particles every 3 ticks
        return duration % 3 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            GildedHareVfx.spawnDustAura(entity);
        }
    }
}
