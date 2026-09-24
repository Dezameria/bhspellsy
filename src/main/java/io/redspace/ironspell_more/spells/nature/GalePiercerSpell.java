package io.redspace.ironspell_more.spells.nature;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.entity.spells.gale_piercer.GaleArrowEntity;
import io.redspace.ironspell_more.entity.spells.gale_piercer.WindArrowEntity;
import io.redspace.ironspell_more.event.GalePiercerCastingEvents;
import io.redspace.ironspell_more.registry.ParticleRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

import io.redspace.ironspell_more.config.SpellConfig;

@AutoSpellConfig
public class GalePiercerSpell extends AbstractSpell {
    // ==========================================
    // SPELL TUNING CONSTANTS (Code Defaults)
    // ==========================================
    public static final float NORMAL_BASE_DAMAGE = 12.0F;
    public static final float NORMAL_DAMAGE_PER_LEVEL = 2.0F;
    public static final float FULL_BASE_DAMAGE = 28.0F;
    public static final float FULL_DAMAGE_PER_LEVEL = 4.0F;
    public static final int BASE_MANA_COST = 40;
    public static final int MANA_COST_PER_LEVEL = 5;
    public static final double COOLDOWN_SECONDS = 20.0D;

    public static final ResourceLocation SPELL_ID = IronSpellMore.id("gale_piercer");

    public static final int FULL_CHARGE_TICKS = 200; // 10 seconds
    public static final int NATIVE_HOLD_CAST_TICKS = 1_000_000_000;
    public static final float MAX_LOCK_RANGE = 48.0F;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.NATURE_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(COOLDOWN_SECONDS)
            .build();

    public GalePiercerSpell() {
        this.baseManaCost = BASE_MANA_COST;
        this.manaCostPerLevel = MANA_COST_PER_LEVEL;
        this.baseSpellPower = (int) NORMAL_BASE_DAMAGE;
        this.spellPowerPerLevel = (int) NORMAL_DAMAGE_PER_LEVEL;
        this.castTime = FULL_CHARGE_TICKS;
    }

    @Override
    public int getManaCost(int spellLevel) {
        return SpellConfig.GalePiercer.getBaseMana() + (spellLevel - 1) * SpellConfig.GalePiercer.getManaPerLevel();
    }

    @Override
    public int getSpellCooldown() {
        return (int) (SpellConfig.GalePiercer.getCooldown() * 20);
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
        return NATIVE_HOLD_CAST_TICKS;
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
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ARROW_SHOOT);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity caster, MagicData magicData) {
        if (caster instanceof ServerPlayer serverPlayer) {
            GalePiercerCastingEvents.updateLock(serverPlayer, this, MAX_LOCK_RANGE);

            int elapsedTicks = getElapsedCastTicks(magicData);

            // Compute arrow position in front of caster's right hand
            Vec3 lookDir = caster.getLookAngle().normalize();
            Vec3 rightDir = lookDir.cross(new Vec3(0, 1, 0)).normalize();
            if (rightDir.lengthSqr() < 1.0E-4D) {
                rightDir = new Vec3(1, 0, 0);
            }
            Vec3 arrowPos = caster.getEyePosition().add(lookDir.scale(0.7D)).add(rightDir.scale(0.25D)).subtract(0, 0.12D, 0);

            if (serverPlayer.level() instanceof ServerLevel serverLevel) {
                // 1. Continuous inward-swirling particles orbiting toward the arrow
                int particleCount = (elapsedTicks >= FULL_CHARGE_TICKS) ? 4 : 3;
                for (int i = 0; i < particleCount; i++) {
                    double radius = 0.55D + caster.getRandom().nextDouble() * 0.35D;
                    double theta = caster.getRandom().nextDouble() * Math.PI * 2.0D;
                    double phi = (caster.getRandom().nextDouble() - 0.5D) * Math.PI;
                    Vec3 pOffset = new Vec3(
                            Math.cos(theta) * Math.cos(phi) * radius,
                            Math.sin(phi) * radius,
                            Math.sin(theta) * Math.cos(phi) * radius
                    );
                    Vec3 spawnPos = arrowPos.add(pOffset);
                    Vec3 inwardVel = arrowPos.subtract(spawnPos).scale(0.14D);

                    if (elapsedTicks < FULL_CHARGE_TICKS) {
                        // White-gray / wind silver swirling inward particles
                        serverLevel.sendParticles(ParticleTypes.ENCHANT, spawnPos.x, spawnPos.y, spawnPos.z, 0, inwardVel.x, inwardVel.y, inwardVel.z, 1.0D);
                        if (i == 0) {
                            serverLevel.sendParticles(ParticleRegistry.WHITE_EMBER.get(), spawnPos.x, spawnPos.y, spawnPos.z, 0, inwardVel.x, inwardVel.y, inwardVel.z, 0.08D);
                        }
                    } else {
                        // Fully charged: fiery sparks and flames swirling inward
                        serverLevel.sendParticles(ParticleTypes.SMALL_FLAME, spawnPos.x, spawnPos.y, spawnPos.z, 0, inwardVel.x, inwardVel.y, inwardVel.z, 0.10D);
                        if (i == 0) {
                            serverLevel.sendParticles(ParticleTypes.FLAME, spawnPos.x, spawnPos.y, spawnPos.z, 0, inwardVel.x * 0.8D, inwardVel.y * 0.8D, inwardVel.z * 0.8D, 0.06D);
                        }
                    }
                }

                // 2. Full charge reached: audio notification and outward particle dispersion burst
                if (elapsedTicks == FULL_CHARGE_TICKS) {
                    serverPlayer.displayClientMessage(Component.translatable("ui.ironspell_more.gale_piercer_full_charge"), true);
                    level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.8F);
                    level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                            SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.6F);

                    // Outward particle dispersion burst
                    for (int i = 0; i < 24; i++) {
                        double yaw = (i / 24.0D) * Math.PI * 2.0D;
                        double pitch = (caster.getRandom().nextDouble() - 0.5D) * 0.7D;
                        double speed = 0.16D + caster.getRandom().nextDouble() * 0.1D;
                        double vx = Math.cos(yaw) * Math.cos(pitch) * speed;
                        double vy = Math.sin(pitch) * speed;
                        double vz = Math.sin(yaw) * Math.cos(pitch) * speed;

                        serverLevel.sendParticles(ParticleTypes.FLAME, arrowPos.x, arrowPos.y, arrowPos.z, 0, vx, vy, vz, 1.0D);
                        if (i % 2 == 0) {
                            serverLevel.sendParticles(ParticleRegistry.WHITE_FIRE.get(), arrowPos.x, arrowPos.y, arrowPos.z, 0, vx * 0.8D, vy * 0.8D, vz * 0.8D, 1.0D);
                        }
                    }
                    serverLevel.sendParticles(ParticleTypes.FLASH, arrowPos.x, arrowPos.y, arrowPos.z, 1, 0, 0, 0, 0);
                }
            }
        }
        super.onServerCastTick(level, spellLevel, caster, magicData);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData magicData) {
        int elapsedTicks = getElapsedCastTicks(magicData);
        boolean isFullCharge = elapsedTicks >= FULL_CHARGE_TICKS;

        LivingEntity lockedTarget = null;
        if (caster instanceof ServerPlayer serverPlayer) {
            lockedTarget = GalePiercerCastingEvents.getLockedTarget(serverPlayer, MAX_LOCK_RANGE);
            GalePiercerCastingEvents.clearLock(serverPlayer, this);
        }

        Vec3 lookDir = caster.getLookAngle().normalize();
        Vec3 spawnPos = caster.getEyePosition().add(lookDir.scale(0.8D));

        if (isFullCharge) {
            // Full Charge: Gale Arrow ("ศรวายุ")
            float damage = getFullChargeDamage(spellLevel, caster);
            GaleArrowEntity galeArrow = new GaleArrowEntity(level, caster);
            galeArrow.setPos(spawnPos.x, spawnPos.y - galeArrow.getBbHeight() * 0.5D, spawnPos.z);
            galeArrow.initializeArrow(lockedTarget, damage, spellLevel, lookDir, GaleArrowEntity.SPEED);
            level.addFreshEntity(galeArrow);

            // Explosive wind release audio
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5F, 1.3F);
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.2F, 1.6F);
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.8F, 0.7F);
        } else {
            // Early Release: Wind Arrow
            float damage = getNormalDamage(spellLevel, caster);
            WindArrowEntity windArrow = new WindArrowEntity(level, caster);
            windArrow.setPos(spawnPos.x, spawnPos.y - windArrow.getBbHeight() * 0.5D, spawnPos.z);
            windArrow.initializeArrow(lockedTarget, damage, spellLevel, lookDir, WindArrowEntity.SPEED);
            level.addFreshEntity(windArrow);

            // Wind whoosh audio
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.4F, 1.1F);
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.2F, 1.2F);
        }

        super.onCast(level, spellLevel, caster, castSource, magicData);
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity caster, MagicData magicData, boolean cancelled) {
        try {
            // Early release conversion
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
                GalePiercerCastingEvents.clearLock(serverPlayer, this);
            }
        }
    }

    private int getElapsedCastTicks(MagicData magicData) {
        if (magicData == null || !magicData.isCasting()) {
            return 0;
        }
        return Math.max(0, magicData.getCastDuration() - magicData.getCastDurationRemaining());
    }

    public float getNormalDamage(int spellLevel, LivingEntity caster) {
        float base = SpellConfig.GalePiercer.getNormalBaseDamage();
        float perLevel = SpellConfig.GalePiercer.getNormalDamagePerLevel();
        return (base + (spellLevel - 1) * perLevel) * getEntityPowerMultiplier(caster);
    }

    public float getFullChargeDamage(int spellLevel, LivingEntity caster) {
        float base = SpellConfig.GalePiercer.getFullBaseDamage();
        float perLevel = SpellConfig.GalePiercer.getFullDamagePerLevel();
        return (base + (spellLevel - 1) * perLevel) * getEntityPowerMultiplier(caster);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.ironspell_more.gale_piercer_damage_normal",
                        Utils.stringTruncation(getNormalDamage(spellLevel, caster), 1)),
                Component.translatable("ui.ironspell_more.gale_piercer_damage_full",
                        Utils.stringTruncation(getFullChargeDamage(spellLevel, caster), 1)),
                Component.translatable("ui.ironspell_more.gale_piercer_lock_range",
                        Utils.stringTruncation(MAX_LOCK_RANGE, 0))
        );
    }
}
