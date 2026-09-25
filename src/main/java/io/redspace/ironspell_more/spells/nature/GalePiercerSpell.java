package io.redspace.ironspell_more.spells.nature;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.entity.spells.gale_piercer.GaleArrowEntity;
import io.redspace.ironspell_more.entity.spells.gale_piercer.WindArrowEntity;
import io.redspace.ironspell_more.event.GalePiercerCastingEvents;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.joml.Vector3f;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
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
    public static final double MIN_PROJECTILE_SPEED = 0.8D;

    private static final DustParticleOptions WHITE_WIND_DUST =
            new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 0.85F);
    private static final DustParticleOptions PALE_WIND_DUST =
            new DustParticleOptions(new Vector3f(0.92F, 0.94F, 1.0F), 0.9F);
    private static final DustParticleOptions ORANGE_SPARK_DUST =
            new DustParticleOptions(new Vector3f(1.0F, 0.38F, 0.08F), 1.0F);
    private static final DustParticleOptions CRIMSON_SPARK_DUST =
            new DustParticleOptions(new Vector3f(1.0F, 0.12F, 0.04F), 0.85F);

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

            // Compute arrow position in front of caster's hand holding the arrow
            Vec3 eyePos = caster.getEyePosition();
            Vec3 lookDir = caster.getLookAngle().normalize();
            Vec3 referenceAxis = Math.abs(lookDir.y) > 0.9D ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            Vec3 rightDir = lookDir.cross(referenceAxis).normalize();
            Vec3 upDir = rightDir.cross(lookDir).normalize();

            double sideOffset = (caster.getMainArm() == HumanoidArm.RIGHT ? 0.22D : -0.22D);
            double baseDown = 0.35D;
            Vec3 arrowCenter = eyePos.add(lookDir.scale(0.42D)).add(rightDir.scale(sideOffset)).subtract(0.0D, baseDown, 0.0D);

            if (serverPlayer.level() instanceof ServerLevel serverLevel) {
                boolean fullyCharged = elapsedTicks >= FULL_CHARGE_TICKS;

                // Keep both wind layers locked to the rendered arrow while limiting network traffic.
                if ((elapsedTicks & 1) == 0) {
                    spawnChargingWind(serverLevel, arrowCenter, lookDir, rightDir, upDir, elapsedTicks, fullyCharged);
                }

                if (elapsedTicks % 24 == 0) {
                    level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                            SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.35F,
                            1.4F + caster.getRandom().nextFloat() * 0.2F);
                }

                if (elapsedTicks == FULL_CHARGE_TICKS) {
                    serverPlayer.displayClientMessage(Component.translatable("ui.ironspell_more.gale_piercer_full_charge"), true);
                    level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.8F);
                    level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                            SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.6F);

                    serverLevel.sendParticles(ParticleTypes.FLASH, arrowCenter.x, arrowCenter.y, arrowCenter.z, 1, 0, 0, 0, 0);
                    spawnFullChargeCollapse(serverLevel, arrowCenter, lookDir, rightDir, upDir);
                }
            }
        }
        super.onServerCastTick(level, spellLevel, caster, magicData);
    }

    private static void spawnChargingWind(ServerLevel level, Vec3 arrowCenter, Vec3 lookDir,
                                          Vec3 rightDir, Vec3 upDir, int elapsedTicks,
                                          boolean fullyCharged) {
        double phase = elapsedTicks * (fullyCharged ? 0.48D : 0.34D);
        double helixRadius = fullyCharged ? 0.13D : 0.10D;

        // Two wind ribbons rotate around points sampled directly along the arrow shaft.
        for (int arm = 0; arm < 2; arm++) {
            for (int step = 0; step < 3; step++) {
                double progress = (step + 0.5D) / 3.0D;
                double shaftOffset = -0.30D + progress * 0.60D;
                double angle = phase + arm * Math.PI + progress * Math.PI * 2.5D;
                Vec3 radial = rightDir.scale(Math.cos(angle) * helixRadius)
                        .add(upDir.scale(Math.sin(angle) * helixRadius));
                Vec3 windPos = arrowCenter.add(lookDir.scale(shaftOffset)).add(radial);

                level.sendParticles(arm == 0 ? WHITE_WIND_DUST : PALE_WIND_DUST,
                        windPos.x, windPos.y, windPos.z, 1, 0, 0, 0, 0);

                if (fullyCharged && arm == 0) {
                    level.sendParticles(ParticleTypes.SMALL_FLAME,
                            windPos.x, windPos.y, windPos.z, 1, 0, 0, 0, 0);
                }

                // Full charge adds sparse sparks inside the wind instead of a flame shell.
                if (fullyCharged && step == arm + 1) {
                    DustParticleOptions spark = arm == 0 ? ORANGE_SPARK_DUST : CRIMSON_SPARK_DUST;
                    level.sendParticles(spark, windPos.x, windPos.y, windPos.z, 1, 0, 0, 0, 0);
                }
            }
        }

        // Three complete gathering streams remain visible at once instead of collapsing into one clump.
        for (int lane = 0; lane < 3; lane++) {
            double shaftOffset = -0.24D + lane * 0.24D;
            Vec3 sink = arrowCenter.add(lookDir.scale(shaftOffset));

            for (int sample = 0; sample < 3; sample++) {
                double convergence = (elapsedTicks * 0.025D + sample / 3.0D) % 1.0D;
                double radius = 1.80D - convergence * 1.72D;
                double angle = phase * 0.45D
                        + lane * Math.PI * 2.0D / 3.0D
                        + convergence * Math.PI * 1.5D;
                Vec3 gatherPos = sink
                        .add(rightDir.scale(Math.cos(angle) * radius))
                        .add(upDir.scale(Math.sin(angle) * radius));

                if (sample == 1) {
                    Vec3 inwardVelocity = sink.subtract(gatherPos).normalize().scale(0.08D);
                    level.sendParticles(ParticleHelper.EMBERS, gatherPos.x, gatherPos.y, gatherPos.z,
                            0, inwardVelocity.x, inwardVelocity.y, inwardVelocity.z, 1.0D);
                } else {
                    level.sendParticles(PALE_WIND_DUST, gatherPos.x, gatherPos.y, gatherPos.z,
                            1, 0, 0, 0, 0);
                }
            }

            level.sendParticles(WHITE_WIND_DUST, sink.x, sink.y, sink.z, 1, 0, 0, 0, 0);
        }
    }

    private static void spawnFullChargeCollapse(ServerLevel level, Vec3 arrowCenter, Vec3 lookDir,
                                                Vec3 rightDir, Vec3 upDir) {
        for (int ring = 0; ring < 3; ring++) {
            double radius = 0.66D - ring * 0.22D;
            for (int point = 0; point < 6; point++) {
                double angle = point * Math.PI * 2.0D / 6.0D + ring * 0.45D;
                double shaftOffset = -0.24D + point * 0.096D;
                Vec3 shaftPoint = arrowCenter.add(lookDir.scale(shaftOffset));
                Vec3 spawnPos = shaftPoint
                        .add(rightDir.scale(Math.cos(angle) * radius))
                        .add(upDir.scale(Math.sin(angle) * radius));
                Vec3 inward = shaftPoint.subtract(spawnPos).normalize().scale(0.20D + ring * 0.03D);

                level.sendParticles(ParticleHelper.EMBERS, spawnPos.x, spawnPos.y, spawnPos.z,
                        0, inward.x, inward.y, inward.z, 1.0D);
            }
        }
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
            double projectileSpeed = getProjectileSpeedForCharge(elapsedTicks);
            WindArrowEntity windArrow = new WindArrowEntity(level, caster);
            windArrow.setPos(spawnPos.x, spawnPos.y - windArrow.getBbHeight() * 0.5D, spawnPos.z);
            windArrow.initializeArrow(lockedTarget, damage, spellLevel, lookDir, projectileSpeed);
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

    public static double getProjectileSpeedForCharge(int elapsedTicks) {
        double progress = Mth.clamp(elapsedTicks, 0, FULL_CHARGE_TICKS) / (double) FULL_CHARGE_TICKS;
        double easedProgress = progress * progress * (3.0D - 2.0D * progress);
        return MIN_PROJECTILE_SPEED + (GaleArrowEntity.SPEED - MIN_PROJECTILE_SPEED) * easedProgress;
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
