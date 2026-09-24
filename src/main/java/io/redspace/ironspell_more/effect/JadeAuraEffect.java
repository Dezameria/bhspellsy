package io.redspace.ironspell_more.effect;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class JadeAuraEffect extends MagicMobEffect {
    public static final float ATTACK_DAMAGE_PER_LEVEL = 0.10F;
    public static final float SPEED_PER_LEVEL = 0.20F;
    public static final float SPELL_POWER_PER_LEVEL = 0.05F;

    private static final UUID ATTACK_DAMAGE_UUID = UUID.fromString("7f4c3a21-9e8b-4a5d-b612-8c1d5e3f2a10");
    private static final UUID MOVEMENT_SPEED_UUID = UUID.fromString("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d");
    private static final UUID SPELL_POWER_UUID = UUID.fromString("3d4e5f6a-7b8c-4d9e-af10-1a2b3c4d5e6f");

    public JadeAuraEffect(MobEffectCategory category, int color) {
        super(category, color);
        this.addAttributeModifier(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE_UUID.toString(),
                ATTACK_DAMAGE_PER_LEVEL, AttributeModifier.Operation.MULTIPLY_TOTAL);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED_UUID.toString(),
                SPEED_PER_LEVEL, AttributeModifier.Operation.MULTIPLY_TOTAL);
        this.addAttributeModifier(AttributeRegistry.SPELL_POWER.get(), SPELL_POWER_UUID.toString(),
                SPELL_POWER_PER_LEVEL, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

}
