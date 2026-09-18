package io.redspace.ironspell_more.spells.nature;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.entity.spells.wind_arrow.WindArrowEntity;
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
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.network.casting.SyncTargetingDataPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
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
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@AutoSpellConfig
public class WindArrowSpell extends AbstractSpell {
    public static final ResourceLocation SPELL_ID = IronSpellMore.id("wind_arrow");

    public static final int FULL_CHARGE_TICKS = 200; // 10.0 seconds
    public static final int NATIVE_HOLD_CAST_TICKS = 72000;
    public static final int FULL_CHARGE_HOLD_REFRESH_THRESHOLD = 20;
    public static final int FULL_CHARGE_HOLD_USE_DURATION = 7200;

    public static final float NORMAL_SPEED = 2.2F;
    public static final float FULL_CHARGE_SPEED = 3.8F;

    public static final float NORMAL_TURN_RATE = 0.14F;
    public static final float FULL_CHARGE_TURN_RATE = 0.42F;

    public static final float NORMAL_KNOCKBACK = 0.45F;
    public static final float FULL_CHARGE_KNOCKBACK = 1.25F;

    public static final int FULL_CHARGE_FIRE_SECONDS = 4;
    public static final int FULL_CHARGE_SLOWNESS_TICKS = 80;

    public static final int MAX_TARGET_RANGE = 48;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.NATURE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(12.0)
            .build();

    public WindArrowSpell() {
        this.baseManaCost = 35;
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 3;
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
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.FIRE_ARROW_CHARGE.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.FIRE_ARROW_CAST.get());
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
        // Run crosshair targeting helper to lock candidate in crosshair if present
        Utils.preCastTargetHelper(level, caster, magicData, this, MAX_TARGET_RANGE, 0.35F);
        // Always return true so untargeted casting is permitted
        return true;
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity caster, MagicData magicData) {
        super.onServerCastTick(level, spellLevel, caster, magicData);

        LivingEntity previousTarget = null;
        if (magicData.getAdditionalCastData() instanceof TargetEntityCastData targetData && level instanceof ServerLevel serverLevel) {
            previousTarget = targetData.getTarget(serverLevel);
        }

        // Update crosshair target dynamically during aiming/charging
        HitResult hit = Utils.raycastForEntity(level, caster, (float) MAX_TARGET_RANGE, true, 0.35F);
        LivingEntity newTarget = null;
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living && living.isAlive() && caster.hasLineOfSight(living)) {
            newTarget = living;
        }

        if (newTarget != previousTarget) {
            if (newTarget != null) {
                magicData.setAdditionalCastData(new TargetEntityCastData(newTarget));
                if (caster instanceof ServerPlayer player) {
                    PacketDistributor.sendToPlayer(player, new SyncTargetingDataPacket(newTarget, this));
                }
            } else {
                magicData.setAdditionalCastData(null);
                if (caster instanceof ServerPlayer player) {
                    PacketDistributor.sendToPlayer(player, new SyncTargetingDataPacket(this, Collections.emptyList()));
                }
            }
        }

        // Notify caster upon reaching full charge
        int chargeTicks = magicData.getCastDuration() - magicData.getCastDurationRemaining();
        if (chargeTicks == FULL_CHARGE_TICKS && caster instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("ui.ironspell_more.wind_arrow_full_charge"), true);
            player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 2.0F);
        }
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity caster, MagicData magicData, boolean cancelled) {
        try {
            if (caster instanceof ServerPlayer player) {
                PacketDistributor.sendToPlayer(player, new SyncTargetingDataPacket(this, Collections.emptyList()));
            }

            // Releasing the button early or after full charge cancels the raw LONG cast in Iron's Spells.
            // Convert that release into the projectile firing.
            if (cancelled && caster instanceof ServerPlayer serverPlayer && caster.isAlive() && !caster.isRemoved()
                    && magicData != null && magicData.isCasting() && getSpellId().equals(magicData.getCastingSpellId())) {
                CastSource castSource = magicData.getCastSource();
                castSpell(level, spellLevel, serverPlayer, castSource, true);
                super.onServerCastComplete(level, spellLevel, caster, magicData, false);
                return;
            }
            super.onServerCastComplete(level, spellLevel, caster, magicData, cancelled);
        } finally {
            if (magicData != null) {
                magicData.resetAdditionalCastData();
            }
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData magicData) {
        int chargeTicks = magicData != null ? magicData.getCastDuration() - magicData.getCastDurationRemaining() : 0;
        boolean full = chargeTicks >= FULL_CHARGE_TICKS;

        LivingEntity homingTarget = null;
        if (magicData != null && magicData.getAdditionalCastData() instanceof TargetEntityCastData targetData
                && level instanceof ServerLevel serverLevel) {
            LivingEntity target = targetData.getTarget(serverLevel);
            if (target != null && target.isAlive() && caster.hasLineOfSight(target)) {
                homingTarget = target;
            }
        }

        Vec3 look = caster.getLookAngle().normalize();
        Vec3 spawnPos = caster.getEyePosition().add(look.scale(0.8D));

        float damage = getDamage(spellLevel, caster, full);

        WindArrowEntity arrow = new WindArrowEntity(level, caster);
        arrow.setPos(spawnPos.x, spawnPos.y - arrow.getBbHeight() * 0.5D, spawnPos.z);
        arrow.configure(look, full, homingTarget, damage);
        level.addFreshEntity(arrow);

        if (full) {
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.1F);
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.PHANTOM_SWOOP, SoundSource.PLAYERS, 0.8F, 1.4F);
        } else {
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        super.onCast(level, spellLevel, caster, castSource, magicData);
    }

    public float getDamage(int spellLevel, LivingEntity caster, boolean fullCharge) {
        float base = fullCharge ? 22.0F + (spellLevel - 1) * 5.0F : 8.0F + (spellLevel - 1) * 2.0F;
        return base * getSpellPower(spellLevel, caster);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.ironspell_more.wind_arrow_normal_damage", Utils.stringTruncation(getDamage(spellLevel, caster, false), 1)),
                Component.translatable("ui.ironspell_more.wind_arrow_full_damage", Utils.stringTruncation(getDamage(spellLevel, caster, true), 1)),
                Component.translatable("ui.ironspell_more.wind_arrow_charge_time", "10")
        );
    }
}
