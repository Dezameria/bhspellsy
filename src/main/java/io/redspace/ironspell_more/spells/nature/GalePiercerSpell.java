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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

@AutoSpellConfig
public class GalePiercerSpell extends AbstractSpell {
    private static final ResourceLocation SPELL_ID = IronSpellMore.id("gale_piercer");

    public static final int FULL_CHARGE_TICKS = 200; // 10 seconds
    public static final int NATIVE_HOLD_CAST_TICKS = 1_000_000_000;
    public static final float MAX_LOCK_RANGE = 48.0F;

    public static final float NORMAL_BASE_DAMAGE = 12.0F;
    public static final float NORMAL_DAMAGE_PER_LEVEL = 2.0F;
    public static final float FULL_BASE_DAMAGE = 28.0F;
    public static final float FULL_DAMAGE_PER_LEVEL = 4.0F;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.NATURE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(20)
            .build();

    public GalePiercerSpell() {
        this.baseManaCost = 40;
        this.manaCostPerLevel = 5;
        this.baseSpellPower = (int) NORMAL_BASE_DAMAGE;
        this.spellPowerPerLevel = (int) NORMAL_DAMAGE_PER_LEVEL;
        this.castTime = FULL_CHARGE_TICKS;
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
            if (elapsedTicks == FULL_CHARGE_TICKS) {
                // Audio and message notifying full charge reached
                serverPlayer.displayClientMessage(Component.translatable("ui.ironspell_more.gale_piercer_full_charge"), true);
                level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.8F);
                level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                        SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.6F);
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
        float powerMultiplier = getSpellPower(spellLevel, caster) / Math.max(1.0F, (float) baseSpellPower);
        return (NORMAL_BASE_DAMAGE + (spellLevel - 1) * NORMAL_DAMAGE_PER_LEVEL) * powerMultiplier;
    }

    public float getFullChargeDamage(int spellLevel, LivingEntity caster) {
        float powerMultiplier = getSpellPower(spellLevel, caster) / Math.max(1.0F, (float) baseSpellPower);
        return (FULL_BASE_DAMAGE + (spellLevel - 1) * FULL_DAMAGE_PER_LEVEL) * powerMultiplier;
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
