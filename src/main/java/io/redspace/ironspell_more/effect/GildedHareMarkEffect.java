package io.redspace.ironspell_more.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

public class GildedHareMarkEffect extends MobEffect {
    public static final String OWNER_UUID_TAG = "GildedHareOwnerUUID";
    public static final String COMBO_COUNT_TAG = "GildedHareComboCount";
    public static final String LAST_HIT_TICK_TAG = "GildedHareLastHitTick";

    public GildedHareMarkEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Run every tick if amplifier >= 4 (Stunned in cocoon)
        return amplifier >= 4;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (amplifier >= 4) {
            // Complete immobilization: zero all velocity axes during 5-hit stun / cocoon
            entity.setDeltaMovement(0, 0, 0);
            entity.hurtMarked = true;
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);
        if (amplifier >= 4 && entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel && entity.isAlive()) {
            io.redspace.ironspell_more.client.particle.GildedHareVfx.spawnCocoonShatterVfx(serverLevel, entity);
        }
        clearComboData(entity);
    }

    public static void clearComboData(LivingEntity entity) {
        var tag = entity.getPersistentData();
        tag.remove(OWNER_UUID_TAG);
        tag.remove(COMBO_COUNT_TAG);
        tag.remove(LAST_HIT_TICK_TAG);
    }
}
