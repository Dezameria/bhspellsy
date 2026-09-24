package io.redspace.ironspell_more.spells.ground;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.effect.JadeAuraEffect;
import io.redspace.ironspell_more.client.particle.JadeAuraVfx;
import io.redspace.ironspell_more.registry.MobEffectsRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@AutoSpellConfig
public class JadeAuraSpell extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(IronSpellMore.MODID, "jade_aura");
    private static final ResourceLocation GROUND_SCHOOL_RESOURCE = ResourceLocation.fromNamespaceAndPath("bhspells", "ground");

    public static final float TARGET_RANGE = 16.0F;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(GROUND_SCHOOL_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(40)
            .build();

    public JadeAuraSpell() {
        this.baseManaCost = 50;
        this.manaCostPerLevel = 25;
        this.baseSpellPower = 45;
        this.spellPowerPerLevel = 10;
        this.castTime = 0;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public SchoolType getSchoolType() {
        SchoolType school = SchoolRegistry.getSchool(GROUND_SCHOOL_RESOURCE);
        return school != null ? school : SchoolRegistry.EVOCATION.get();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.SELF_CAST_ANIMATION;
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.AMETHYST_BLOCK_CHIME);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks((int) (getSpellPower(spellLevel, caster) * 20), 1)),
                Component.translatable("attribute.modifier.plus.1", Utils.stringTruncation(getPercentAttackDamage(spellLevel), 0), Component.translatable("attribute.name.generic.attack_damage")),
                Component.translatable("attribute.modifier.plus.1", Utils.stringTruncation(getPercentSpeed(spellLevel), 0), Component.translatable("attribute.name.generic.movement_speed")),
                Component.translatable("attribute.modifier.plus.1", Utils.stringTruncation(getPercentSpellPower(spellLevel), 0), Component.translatable("attribute.irons_spellbooks.spell_power")),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(TARGET_RANGE, 1))
        );
    }

    public static float getPercentAttackDamage(int spellLevel) {
        return spellLevel * JadeAuraEffect.ATTACK_DAMAGE_PER_LEVEL * 100.0F;
    }

    public static float getPercentSpeed(int spellLevel) {
        return spellLevel * JadeAuraEffect.SPEED_PER_LEVEL * 100.0F;
    }

    public static float getPercentSpellPower(int spellLevel) {
        return spellLevel * JadeAuraEffect.SPELL_POWER_PER_LEVEL * 100.0F;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            // Calculate duration once before applying any effect to prevent new Spell Power from artificially boosting the 2nd recipient
            int duration = (int) (getSpellPower(spellLevel, entity) * 20);

            // Attempt direct raycast to an ally
            LivingEntity targetAlly = findTargetAlly(level, entity, TARGET_RANGE);

            // Buff caster
            entity.addEffect(new MobEffectInstance(MobEffectsRegistry.JADE_AURA.get(), duration, spellLevel - 1, false, false, true));
            if (level instanceof ServerLevel serverLevel) {
                JadeAuraVfx.spawnCastBurst(serverLevel, entity);
            }

            // Buff ally if valid and distinct from caster
            if (targetAlly != null && targetAlly != entity) {
                targetAlly.addEffect(new MobEffectInstance(MobEffectsRegistry.JADE_AURA.get(), duration, spellLevel - 1, false, false, true));
                if (level instanceof ServerLevel serverLevel) {
                    JadeAuraVfx.spawnCastBurst(serverLevel, targetAlly);
                }
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Nullable
    public static LivingEntity findTargetAlly(Level level, LivingEntity caster, float range) {
        HitResult hitResult = Utils.raycastForEntity(level, caster, range, true, 0.5F);
        if (hitResult instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity livingTarget
                && isTargetAlly(caster, livingTarget)) {
            return livingTarget;
        }
        return null;
    }

    public static boolean isTargetAlly(LivingEntity caster, LivingEntity target) {
        if (target == null || target == caster) return false;
        if (!target.isAlive() || target.isSpectator()) return false;
        if (target.isAlliedTo(caster)) return true;
        if (caster instanceof Player && target instanceof Player) {
            return DamageSources.isFriendlyFireBetween(caster, target);
        }
        if (target instanceof TamableAnimal tamable && tamable.isOwnedBy(caster)) return true;
        return false;
    }
}
