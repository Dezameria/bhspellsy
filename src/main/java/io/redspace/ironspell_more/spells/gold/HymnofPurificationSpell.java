package io.redspace.ironspell_more.spells.gold;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.registry.MobEffectsRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

@AutoSpellConfig
public class HymnofPurificationSpell extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(IronSpellMore.MODID, "hymn_of_purification");
    private static final ResourceLocation GOLD_SCHOOL_RESOURCE = ResourceLocation.fromNamespaceAndPath("bhspells", "gold");

    public static final float RADIUS = 20.0F;
    public static final int DURATION_TICKS = 400; // 20 seconds
    public static final int HEAL_INTERVAL_TICKS = 20; // 1 second per pulse (19 pulses total)
    public static final int NAUSEA_DURATION_TICKS = 40; // 2 seconds

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(GOLD_SCHOOL_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(60.0D)
            .build();

    public HymnofPurificationSpell() {
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 1;
        this.baseManaCost = 80; // Instant cast mana cost
        this.manaCostPerLevel = 0;
        this.castTime = 0; // Instant cast (click once)
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
        SchoolType school = SchoolRegistry.getSchool(GOLD_SCHOOL_RESOURCE);
        return school != null ? school : SchoolRegistry.ENDER.get();
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.empty();
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1)),
                Component.translatable("ui.irons_spellbooks.healing", Utils.stringTruncation(getHealAmount(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(DURATION_TICKS, 1))
        );
    }

    public static float getHealAmount(int spellLevel, LivingEntity caster) {
        return 1.0F * (1.0F + (spellLevel - 1) * 0.5F);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData magicData) {
        if (entity.hasEffect(MobEffectsRegistry.HYMN_OF_PURIFICATION.get())) {
            return false;
        }
        return super.checkPreCastConditions(level, spellLevel, entity, magicData);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        // Enters the 20-second Guqin performance state
        entity.addEffect(new MobEffectInstance(MobEffectsRegistry.HYMN_OF_PURIFICATION.get(), DURATION_TICKS, spellLevel - 1, false, false, true));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    public static boolean isAlly(LivingEntity caster, LivingEntity target) {
        if (target == caster) return true;
        if (!target.isAlive() || target.isSpectator()) return false;
        if (target.isAlliedTo(caster)) return true;
        if (caster instanceof Player && target instanceof Player) {
            return DamageSources.isFriendlyFireBetween(caster, target);
        }
        if (target instanceof TamableAnimal tamable && tamable.isOwnedBy(caster)) return true;
        return false;
    }

    public static void spawnChannelingParticles(Level level, LivingEntity caster, int elapsedTicks) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // Radiating golden musical notes & sparkle dust
        if (elapsedTicks % 4 == 0) {
            double angle = (elapsedTicks * 0.25D) % (Math.PI * 2.0D);
            double ringRadius = 1.5D + (elapsedTicks % 60) * 0.1D;
            double px = caster.getX() + Math.cos(angle) * ringRadius;
            double pz = caster.getZ() + Math.sin(angle) * ringRadius;
            double py = caster.getY() + 0.3D + caster.getRandom().nextDouble() * 1.2D;

            MagicManager.spawnParticles(serverLevel, ParticleTypes.NOTE,
                    px, py, pz, 1, 0.0D, 0.0D, 0.0D, (elapsedTicks % 24) / 24.0D, false);
            MagicManager.spawnParticles(serverLevel, ParticleTypes.WAX_OFF,
                    px, py, pz, 1, 0.01D, 0.02D, 0.01D, 0.02D, false);
        }

        // Expanding subtle golden ring on the ground every 20 ticks
        if (elapsedTicks % 20 == 0) {
            int points = 16;
            for (int i = 0; i < points; i++) {
                double rad = (Math.PI * 2.0D / points) * i;
                double rx = caster.getX() + Math.cos(rad) * 3.0D;
                double rz = caster.getZ() + Math.sin(rad) * 3.0D;
                MagicManager.spawnParticles(serverLevel, ParticleTypes.GLOW,
                        rx, caster.getY() + 0.1D, rz, 1, 0.0D, 0.01D, 0.0D, 0.01D, false);
            }
        }
    }

    public static void performDebuffCleanse(Level level, LivingEntity caster) {
        float radiusSqr = RADIUS * RADIUS;

        List<LivingEntity> allies = level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(RADIUS),
                target -> isAlly(caster, target) && caster.distanceToSqr(target) <= radiusSqr);

        for (LivingEntity ally : allies) {
            var harmfulEffects = ally.getActiveEffects().stream()
                    .filter(effect -> effect.getEffect().getCategory() == MobEffectCategory.HARMFUL)
                    .map(MobEffectInstance::getEffect)
                    .toList();

            for (var effect : harmfulEffects) {
                ally.removeEffect(effect);
            }

            if (level instanceof ServerLevel serverLevel) {
                MagicManager.spawnParticles(serverLevel, ParticleTypes.END_ROD,
                        ally.getX(), ally.getY() + ally.getBbHeight() * 0.5D, ally.getZ(),
                        12, 0.4D, 0.6D, 0.4D, 0.08D, false);
                MagicManager.spawnParticles(serverLevel, ParticleTypes.FLASH,
                        ally.getX(), ally.getY() + ally.getBbHeight() * 0.5D, ally.getZ(),
                        1, 0.0D, 0.0D, 0.0D, 0.0D, false);
            }
        }

        // Sparkling crescendo chime sounds indicating successful debuff cleanse
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.2F, 1.6F);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, 1.3F);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.8F);
    }
}
