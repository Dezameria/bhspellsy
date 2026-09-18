package io.redspace.ironspell_more.spells.nature;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.entity.spells.venomous_blossomfall.AzureVenomNeedleEntity;
import io.redspace.ironspell_more.event.VenomousBlossomfallCastingEvents;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Optional;

@AutoSpellConfig
public class VenomousBlossomfallSpell extends AbstractSpell {
    private static final ResourceLocation SPELL_ID = IronSpellMore.id("venomous_blossomfall");

    public enum ChargeStage {
        SHORT,
        MEDIUM,
        FULL
    }

    public static final float UNSAFE_ENTITY_RANGE = 5.0F;
    public static final float SHORT_PROJECTILE_RANGE = 30.0F;
    public static final float MEDIUM_PROJECTILE_RANGE = 60.0F;
    public static final float FULL_PROJECTILE_RANGE = 90.0F;

    public static final float SHORT_DIRECT_DAMAGE = 15.0F;
    public static final float MEDIUM_DIRECT_DAMAGE = 35.0F;
    public static final float FULL_DIRECT_DAMAGE = 50.0F;

    public static final int MEDIUM_CHARGE_TICKS = 5 * 20;
    public static final int FULL_CHARGE_TICKS = 15 * 20;
    /** Native LONG-cast timer kept far beyond any practical hold duration. */
    public static final int NATIVE_HOLD_CAST_TICKS = 1_000_000_000;
    public static final int FULL_CHARGE_HOLD_REFRESH_THRESHOLD = 200;
    public static final int FULL_CHARGE_HOLD_USE_DURATION = 7200;
    public static final float MEDIUM_CHARGE_THRESHOLD = MEDIUM_CHARGE_TICKS / (float) FULL_CHARGE_TICKS;
    public static final float FULL_CHARGE_THRESHOLD = 0.999F;

    public static final double SHORT_CHARGE_SPEED = 0.8D;
    public static final double MEDIUM_CHARGE_SPEED = 4.0D;
    public static final double FULL_CHARGE_SPEED = 7.0D;

    public static final int MISS_COOLDOWN_SECONDS = 30;
    public static final int DIRECT_HIT_COOLDOWN_SECONDS = 60;
    public static final int MISS_COOLDOWN_EFFECT_AMPLIFIER = 0;
    public static final int SHORT_HIT_COOLDOWN_EFFECT_AMPLIFIER = 1;
    public static final int MEDIUM_HIT_COOLDOWN_EFFECT_AMPLIFIER = 2;
    public static final int FULL_HIT_COOLDOWN_EFFECT_AMPLIFIER = 3;

    public static final int AZURE_VENOM_DURATION_TICKS = 100;
    public static final int AZURE_VENOM_DAMAGE_INTERVAL = 20;
    public static final float AZURE_VENOM_DAMAGE = 3.0F;

    public static final double FRACTURE_SAMPLE_INTERVAL = 2.5D;
    public static final double FRACTURE_RADIUS = 1.75D;

    public static final double FULL_CHARGE_SONIC_RING_INTERVAL = 3.0D;
    public static final float FULL_CHARGE_SONIC_RING_BASE_RADIUS = 2.4F;
    public static final float FULL_CHARGE_SONIC_RING_RADIUS_STEP = 0.55F;
    public static final int FULL_CHARGE_SONIC_RING_RADIUS_STEPS = 3;

    /** Roughly a 140 degree view cone; line of sight is checked separately. */
    public static final double VISIBLE_FOV_DOT_THRESHOLD = Math.cos(Math.toRadians(70.0D));

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.NATURE_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(MISS_COOLDOWN_SECONDS)
            .build();

    public VenomousBlossomfallSpell() {
        baseManaCost = 100;
        manaCostPerLevel = 0;
        baseSpellPower = (int) FULL_DIRECT_DAMAGE;
        spellPowerPerLevel = 0;
        castTime = FULL_CHARGE_TICKS;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return SPELL_ID;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public int getEffectiveCastTime(int spellLevel, LivingEntity entity) {
        // Gameplay charge reaches its cap after FULL_CHARGE_TICKS. The native timer
        // only keeps Iron's Spells in LONG-cast state until an explicit release.
        return NATIVE_HOLD_CAST_TICKS;
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
        return SpellAnimations.BOW_CHARGE_ANIMATION;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData magicData) {
        if (hasVisibleLivingEntityInsideUnsafeRange(level, caster)) {
            if (caster instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable("ui.ironspell_more.venomous_blossomfall_too_close"), true);
            }
            return false;
        }
        return true;
    }

    /**
     * Uses an authoritative view cone plus line of sight. Entities behind the
     * caster
     * or behind blocks do not prevent casting.
     */
    private boolean hasVisibleLivingEntityInsideUnsafeRange(Level level, LivingEntity caster) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle().normalize();
        double range = UNSAFE_ENTITY_RANGE;
        AABB searchBox = caster.getBoundingBox().inflate(range);

        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, searchBox,
                target -> target != caster && target.isAlive() && !target.isSpectator())) {
            Vec3 targetPoint = candidate.getBoundingBox().getCenter();
            Vec3 toTarget = targetPoint.subtract(eye);
            if (toTarget.lengthSqr() > range * range || toTarget.lengthSqr() < 1.0E-6D) {
                continue;
            }
            if (look.dot(toTarget.normalize()) >= VISIBLE_FOV_DOT_THRESHOLD
                    && caster.hasLineOfSight(candidate)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onServerPreCast(Level level, int spellLevel, LivingEntity caster, MagicData magicData) {
        if (caster instanceof ServerPlayer serverPlayer) {
            VenomousBlossomfallCastingEvents.beginMovementLock(serverPlayer);
        }
        super.onServerPreCast(level, spellLevel, caster, magicData);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity caster, MagicData magicData) {
        if (caster instanceof ServerPlayer serverPlayer) {
            VenomousBlossomfallCastingEvents.enforceMovementLock(serverPlayer);
            VenomousBlossomfallCastingEvents.updateChargeStage(serverPlayer,
                    chargeStage(getAuthoritativeCharge(magicData)));
        } else {
            lockCasterMovement(caster);
        }
        super.onServerCastTick(level, spellLevel, caster, magicData);
    }

    public static void lockCasterMovement(LivingEntity caster) {
        caster.xxa = 0.0F;
        caster.zza = 0.0F;
        caster.setSprinting(false);
        Vec3 motion = caster.getDeltaMovement();
        boolean needsCorrection = Math.abs(motion.x) > 1.0E-5D || Math.abs(motion.z) > 1.0E-5D || motion.y > 0.0D;
        if (needsCorrection) {
            caster.setDeltaMovement(0.0D, Math.min(0.0D, motion.y), 0.0D);
            caster.hurtMarked = true;
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource,
            MagicData magicData) {
        float chargeProgress = getAuthoritativeCharge(magicData);
        Vec3 direction = caster.getLookAngle().normalize();

        AzureVenomNeedleEntity needle = new AzureVenomNeedleEntity(level, caster);
        Vec3 spawnPosition = caster.getEyePosition().add(direction.scale(0.85D));
        needle.setPos(spawnPosition.x, spawnPosition.y - needle.getBbHeight() * 0.5D, spawnPosition.z);
        needle.configure(direction, projectileSpeed(chargeProgress), chargeProgress);
        level.addFreshEntity(needle);

        // เสียงตอนปล่อยเวทไล่ระดับตาม stage
        ChargeStage stage = chargeStage(chargeProgress);
        if (stage == ChargeStage.FULL) {
            // Full charge: เสียงระเบิดสนั่นก้อง + เสียงลมกระแทก + wom solar hit
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0F, 1.2F);
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.5F, 1.4F);
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    net.minecraft.sounds.SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 2.0F, 0.6F);
            playOptionalSound(level, caster, new ResourceLocation("wom", "sfx.solar_hit"), 0.2F, 1.0F);
        } else if (stage == ChargeStage.MEDIUM) {
            // Medium charge: เสียงยิงธนูหนักแน่น + เสียงหวืดลม + traveloptics blast stage
            // one
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    net.minecraft.sounds.SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.6F, 0.9F);
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    net.minecraft.sounds.SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.4F, 0.8F);
            playOptionalSound(level, caster, new ResourceLocation("traveloptics", "blast_stage_one"), 0.7F, 1.0F);
        } else {
            // Short charge: เสียงยิงลูกศรเบาๆ + traveloptics blast stage one
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    net.minecraft.sounds.SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.2F);
            playOptionalSound(level, caster, new ResourceLocation("traveloptics", "blast_stage_one"), 0.5F, 2.0F);
        }

        super.onCast(level, spellLevel, caster, castSource, magicData);
    }

    private static void playOptionalSound(Level level, LivingEntity caster, ResourceLocation soundId, float volume,
            float pitch) {
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(soundId);
        if (sound == null) {
            sound = SoundEvent.createVariableRangeEvent(soundId);
        }
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private float getAuthoritativeCharge(MagicData magicData) {
        if (magicData == null || !magicData.isCasting() || !getSpellId().equals(magicData.getCastingSpellId())) {
            return 0.0F;
        }
        return chargeProgress(magicData.getCastDuration(), magicData.getCastDurationRemaining());
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity caster, MagicData magicData,
            boolean cancelled) {
        try {
            // Iron's Spells normally turns an early LONG-cast release into a cancellation.
            // For this spell, a living player releasing early fires at the accumulated
            // charge.
            if (cancelled && caster instanceof ServerPlayer serverPlayer && caster.isAlive() && !caster.isRemoved()
                    && magicData != null && magicData.isCasting()
                    && getSpellId().equals(magicData.getCastingSpellId())) {
                CastSource castSource = magicData.getCastSource();
                castSpell(level, spellLevel, serverPlayer, castSource, true);
                super.onServerCastComplete(level, spellLevel, caster, magicData, false);
                return;
            }
            super.onServerCastComplete(level, spellLevel, caster, magicData, cancelled);
        } finally {
            if (caster instanceof ServerPlayer serverPlayer) {
                VenomousBlossomfallCastingEvents.releaseMovementLock(serverPlayer);
            }
        }
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.ironspell_more.venomous_blossomfall_damages",
                        Utils.stringTruncation(SHORT_DIRECT_DAMAGE, 1),
                        Utils.stringTruncation(MEDIUM_DIRECT_DAMAGE, 1),
                        Utils.stringTruncation(FULL_DIRECT_DAMAGE, 1)),
                Component.translatable("ui.ironspell_more.venomous_blossomfall_ranges",
                        Utils.stringTruncation(SHORT_PROJECTILE_RANGE, 1),
                        Utils.stringTruncation(MEDIUM_PROJECTILE_RANGE, 1),
                        Utils.stringTruncation(FULL_PROJECTILE_RANGE, 1)),
                Component.translatable("ui.ironspell_more.azure_venom_damage",
                        Utils.stringTruncation(AZURE_VENOM_DAMAGE, 1),
                        AZURE_VENOM_DURATION_TICKS / 20));
    }

    public static float normalizeCharge(float chargeProgress) {
        return Mth.clamp(chargeProgress, 0.0F, 1.0F);
    }

    public static float chargeProgress(int nativeCastDuration, int nativeCastDurationRemaining) {
        int elapsedTicks = Math.max(0, nativeCastDuration - nativeCastDurationRemaining);
        return normalizeCharge(elapsedTicks / (float) FULL_CHARGE_TICKS);
    }

    public static boolean isFullCharge(float chargeProgress) {
        return chargeStage(chargeProgress) == ChargeStage.FULL;
    }

    public static ChargeStage chargeStage(float chargeProgress) {
        float normalized = normalizeCharge(chargeProgress);
        if (normalized >= FULL_CHARGE_THRESHOLD) {
            return ChargeStage.FULL;
        }
        if (normalized >= MEDIUM_CHARGE_THRESHOLD) {
            return ChargeStage.MEDIUM;
        }
        return ChargeStage.SHORT;
    }

    public static double projectileSpeed(float chargeProgress) {
        return switch (chargeStage(chargeProgress)) {
            case SHORT -> SHORT_CHARGE_SPEED;
            case MEDIUM -> MEDIUM_CHARGE_SPEED;
            case FULL -> FULL_CHARGE_SPEED;
        };
    }

    public static double projectileRange(float chargeProgress) {
        return switch (chargeStage(chargeProgress)) {
            case SHORT -> SHORT_PROJECTILE_RANGE;
            case MEDIUM -> MEDIUM_PROJECTILE_RANGE;
            case FULL -> FULL_PROJECTILE_RANGE;
        };
    }

    public static float directDamage(float chargeProgress) {
        return switch (chargeStage(chargeProgress)) {
            case SHORT -> SHORT_DIRECT_DAMAGE;
            case MEDIUM -> MEDIUM_DIRECT_DAMAGE;
            case FULL -> FULL_DIRECT_DAMAGE;
        };
    }

    public static int cooldownEffectAmplifier(float chargeProgress, boolean directHit) {
        if (!directHit) {
            return MISS_COOLDOWN_EFFECT_AMPLIFIER;
        }
        return switch (chargeStage(chargeProgress)) {
            case SHORT -> SHORT_HIT_COOLDOWN_EFFECT_AMPLIFIER;
            case MEDIUM -> MEDIUM_HIT_COOLDOWN_EFFECT_AMPLIFIER;
            case FULL -> FULL_HIT_COOLDOWN_EFFECT_AMPLIFIER;
        };
    }
}
