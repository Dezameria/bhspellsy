package io.redspace.ironspell_more.spells.ground;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.client.particle.ShockwaveParticleOptionCustom;
import io.redspace.ironspell_more.effect.TigershadeMarkEffect;
import io.redspace.ironspell_more.effect.TigershadeStanceEffect;
import io.redspace.ironspell_more.network.TigershadeNetwork;
import io.redspace.ironspell_more.registry.MobEffectsRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.AutoSpellConfig;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

@AutoSpellConfig
public class TigershadeTerrabreakSpell extends AbstractSpell {
    public static final ResourceLocation SPELL_RESOURCE = IronSpellMore.id("tigershade_terrabreak");

    // This is BHSchoolRegistry.GROUND_RESOURCE. Keeping the resource location here avoids
    // hard-loading BHSpells when the optional dependency is not installed.
    private static final ResourceLocation GROUND_SCHOOL_RESOURCE = ResourceLocation.fromNamespaceAndPath(
            "bhspells", "ground");

    public static final float ACQUISITION_RANGE = 20.0F;
    public static final float EXECUTE_MAX_DISTANCE = 5.0F;
    public static final float EXECUTE_HEALTH_PERCENT = 0.10F;
    public static final float HEAL_ON_EXECUTE = 50.0F;
    public static final int MARK_DURATION_TICKS = 1200;
    public static final int EXECUTE_COOLDOWN_TICKS = 600;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(GROUND_SCHOOL_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(EXECUTE_COOLDOWN_TICKS / 20.0D)
            .build();

    public TigershadeTerrabreakSpell() {
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 1;
        this.baseManaCost = 40;
        this.manaCostPerLevel = 0;
        this.castTime = 0;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return SPELL_RESOURCE;
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
    public SchoolType getSchoolType() {
        SchoolType school = SchoolRegistry.getSchool(GROUND_SCHOOL_RESOURCE);
        return school != null ? school : SchoolRegistry.EVOCATION.get();
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.cooldown",
                        Utils.timeFromTicks(EXECUTE_COOLDOWN_TICKS, 1)),
                Component.translatable("ui.irons_spellbooks.distance",
                        Utils.stringTruncation(EXECUTE_MAX_DISTANCE, 1)),
                Component.translatable("ui.irons_spellbooks.duration",
                        Utils.timeFromTicks(MARK_DURATION_TICKS, 1)),
                Component.translatable("ui.irons_spellbooks.hp",
                        Utils.stringTruncation(HEAL_ON_EXECUTE, 1)));
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData magicData) {
        boolean hasRecast = magicData.getPlayerRecasts().hasRecastForSpell(this);
        boolean hasStance = caster.hasEffect(MobEffectsRegistry.TIGERSHADE_STANCE.get());

        // Repair stale state before deciding which phase this cast represents.
        if (hasRecast != hasStance) {
            clearHunt(caster);
            hasRecast = false;
            hasStance = false;
        }

        if (!hasRecast && !hasStance) {
            LivingEntity target = findLookTarget(level, caster, ACQUISITION_RANGE);
            if (target == null || !isTargetAvailable(caster, target)) {
                displayActionBar(caster, "ui.ironspell_more.tigershade_no_target");
                return false;
            }
            return true;
        }

        LivingEntity target = resolveMarkedTarget(caster);
        if (target == null) {
            displayActionBar(caster, "ui.ironspell_more.tigershade_target_lost");
            clearHunt(caster);
            return false;
        }

        if (caster.distanceToSqr(target) > EXECUTE_MAX_DISTANCE * EXECUTE_MAX_DISTANCE) {
            displayActionBar(caster, "ui.ironspell_more.tigershade_too_far");
            return false;
        }

        if (target.getHealth() > target.getMaxHealth() * EXECUTE_HEALTH_PERCENT) {
            displayActionBar(caster, "ui.ironspell_more.tigershade_hp_too_high");
            return false;
        }

        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData magicData) {
        boolean executing = magicData.getPlayerRecasts().hasRecastForSpell(this)
                && caster.hasEffect(MobEffectsRegistry.TIGERSHADE_STANCE.get());

        if (!executing) {
            LivingEntity target = findLookTarget(level, caster, ACQUISITION_RANGE);
            if (target != null && isTargetAvailable(caster, target)) {
                beginHunt(caster, target, spellLevel, castSource, magicData);
            }
        } else {
            LivingEntity target = resolveMarkedTarget(caster);
            if (canExecute(caster, target)) {
                executeTarget(level, caster, target);
            } else {
                // Fail closed if another event changed the target between pre-cast and cast.
                clearHunt(caster);
            }
        }

        super.onCast(level, spellLevel, caster, castSource, magicData);
    }

    private void beginHunt(LivingEntity caster, LivingEntity target, int spellLevel, CastSource castSource,
            MagicData magicData) {
        clearHunt(caster);

        boolean markApplied = target.addEffect(new MobEffectInstance(MobEffectsRegistry.TIGERSHADE_MARK.get(),
                MARK_DURATION_TICKS, 0, false, false, true));
        boolean stanceApplied = markApplied && caster.addEffect(new MobEffectInstance(
                MobEffectsRegistry.TIGERSHADE_STANCE.get(), MARK_DURATION_TICKS, 0, false, false, true));

        if (!markApplied || !stanceApplied) {
            target.removeEffect(MobEffectsRegistry.TIGERSHADE_MARK.get());
            caster.removeEffect(MobEffectsRegistry.TIGERSHADE_STANCE.get());
            return;
        }

        caster.getPersistentData().putString(TigershadeStanceEffect.TARGET_UUID_TAG, target.getStringUUID());
        caster.getPersistentData().putString(TigershadeStanceEffect.TARGET_DIM_TAG,
                target.level().dimension().location().toString());
        target.getPersistentData().putString(TigershadeMarkEffect.MARK_CASTER_UUID_TAG, caster.getStringUUID());
        target.getPersistentData().putString(TigershadeMarkEffect.MARK_CASTER_DIM_TAG,
                caster.level().dimension().location().toString());

        RecastInstance recast = new RecastInstance(getSpellId(), spellLevel, 2, MARK_DURATION_TICKS, castSource, null);
        if (!magicData.getPlayerRecasts().addRecast(recast, magicData)) {
            clearHunt(caster);
            return;
        }

        if (caster instanceof ServerPlayer player) {
            TigershadeNetwork.syncTarget(player, target.getUUID());
        }

        levelMarkEffects(caster, target);
    }

    private void levelMarkEffects(LivingEntity caster, LivingEntity target) {
        Level level = caster.level();
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.2F, 1.2F);
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 0.8F, 1.4F);

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.65F, 0.15F), 1.5F),
                    target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ(),
                    25, 0.4, 0.5, 0.4, 0.1);
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ(),
                    10, 0.25, 0.3, 0.25, 0.05);
            serverLevel.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.55F, 0.10F), 1.2F),
                    caster.getX(), caster.getY() + 0.3, caster.getZ(),
                    15, 0.35, 0.2, 0.3, 0.02);
        }
    }

    private boolean canExecute(LivingEntity caster, @Nullable LivingEntity target) {
        return target != null
                && target.isAlive()
                && caster.level() == target.level()
                && caster.distanceToSqr(target) <= EXECUTE_MAX_DISTANCE * EXECUTE_MAX_DISTANCE
                && target.getHealth() <= target.getMaxHealth() * EXECUTE_HEALTH_PERCENT
                && isOwnedMark(caster, target);
    }

    private void executeTarget(Level level, LivingEntity caster, LivingEntity target) {
        Vec3 toTarget = target.position().subtract(caster.position());
        caster.setDeltaMovement(new Vec3(toTarget.x, 0.15D, toTarget.z).normalize().scale(1.25D));
        caster.hurtMarked = true;

        target.setDeltaMovement(0.0D, -1.2D, 0.0D);
        target.hurtMarked = true;

        boolean damageAccepted = DamageSources.applyDamage(target, 999999.0F, getDamageSource(caster));
        if (damageAccepted && target.isAlive()) {
            target.setHealth(0.0F);
            target.die(getDamageSource(caster));
        }

        boolean defeated = !target.isAlive();
        if (defeated) {
            caster.heal(HEAL_ON_EXECUTE);
            playExecuteEffects(level, target);
        }

        // The attempt consumes the mark. Healing is awarded only when the target is
        // actually defeated; the framework applies the configured 30-second cooldown.
        clearHunt(caster);
    }

    private void playExecuteEffects(Level level, LivingEntity target) {
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.3F, 0.7F);
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.5F, 0.5F);

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(new ShockwaveParticleOptionCustom(
                            new Vector3f(1.0F, 0.65F, 0.15F), 5.0F, true, new Vector3f(0, 1, 0)),
                    target.getX(), target.getY() + 0.15, target.getZ(), 1, 0, 0, 0, 0);
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,
                            Blocks.COARSE_DIRT.defaultBlockState()),
                    target.getX(), target.getY() + 0.1, target.getZ(), 60, 1.2, 0.3, 1.2, 0.25);
            serverLevel.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.60F, 0.10F), 2.0F),
                    target.getX(), target.getY() + 0.8, target.getZ(), 40, 1.0, 0.8, 1.0, 0.25);
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    target.getX(), target.getY() + 0.6, target.getZ(), 25, 0.8, 0.6, 0.8, 0.15);
            serverLevel.sendParticles(ParticleTypes.LAVA,
                    target.getX(), target.getY() + 0.5, target.getZ(), 6, 0.5, 0.5, 0.5, 0.1);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    target.getX(), target.getY() + 0.5, target.getZ(), 1, 0, 0, 0, 0);
        }
    }

    @Override
    public void onRecastFinished(ServerPlayer player, RecastInstance recastInstance, RecastResult recastResult,
            ICastDataSerializable castData) {
        if (recastResult != RecastResult.USER_CANCEL) {
            clearHunt(player);
        }
        if (recastResult.isSuccess()) {
            super.onRecastFinished(player, recastInstance, recastResult, castData);
        }
    }

    public static void clearHunt(LivingEntity caster) {
        LivingEntity target = resolveLinkedEntity(caster, TigershadeStanceEffect.TARGET_UUID_TAG,
                TigershadeStanceEffect.TARGET_DIM_TAG);

        caster.getPersistentData().remove(TigershadeStanceEffect.TARGET_UUID_TAG);
        caster.getPersistentData().remove(TigershadeStanceEffect.TARGET_DIM_TAG);
        if (caster instanceof ServerPlayer player) {
            TigershadeNetwork.syncTarget(player, null);
        }

        if (target != null && isLinkedPair(caster, target)) {
            target.getPersistentData().remove(TigershadeMarkEffect.MARK_CASTER_UUID_TAG);
            target.getPersistentData().remove(TigershadeMarkEffect.MARK_CASTER_DIM_TAG);
            target.removeEffect(MobEffectsRegistry.TIGERSHADE_MARK.get());
        }

        caster.removeEffect(MobEffectsRegistry.TIGERSHADE_STANCE.get());
        cancelActiveRecast(caster);
    }

    public static void onStanceRemoved(LivingEntity caster) {
        LivingEntity target = resolveLinkedEntity(caster, TigershadeStanceEffect.TARGET_UUID_TAG,
                TigershadeStanceEffect.TARGET_DIM_TAG);

        caster.getPersistentData().remove(TigershadeStanceEffect.TARGET_UUID_TAG);
        caster.getPersistentData().remove(TigershadeStanceEffect.TARGET_DIM_TAG);
        if (caster instanceof ServerPlayer player) {
            TigershadeNetwork.syncTarget(player, null);
        }

        if (target != null && isLinkedPair(caster, target)) {
            target.getPersistentData().remove(TigershadeMarkEffect.MARK_CASTER_UUID_TAG);
            target.getPersistentData().remove(TigershadeMarkEffect.MARK_CASTER_DIM_TAG);
            target.removeEffect(MobEffectsRegistry.TIGERSHADE_MARK.get());
        }
        cancelActiveRecast(caster);
    }

    public static void onMarkRemoved(LivingEntity target) {
        LivingEntity caster = resolveLinkedEntity(target, TigershadeMarkEffect.MARK_CASTER_UUID_TAG,
                TigershadeMarkEffect.MARK_CASTER_DIM_TAG);

        target.getPersistentData().remove(TigershadeMarkEffect.MARK_CASTER_UUID_TAG);
        target.getPersistentData().remove(TigershadeMarkEffect.MARK_CASTER_DIM_TAG);

        if (caster != null && isCasterLinkedToTarget(caster, target)) {
            caster.getPersistentData().remove(TigershadeStanceEffect.TARGET_UUID_TAG);
            caster.getPersistentData().remove(TigershadeStanceEffect.TARGET_DIM_TAG);
            if (caster instanceof ServerPlayer player) {
                TigershadeNetwork.syncTarget(player, null);
            }
            caster.removeEffect(MobEffectsRegistry.TIGERSHADE_STANCE.get());
            cancelActiveRecast(caster);
        }
    }

    public static void syncHuntTarget(ServerPlayer player) {
        LivingEntity target = resolveMarkedTarget(player);
        TigershadeNetwork.syncTarget(player, target == null ? null : target.getUUID());
    }

    private static void cancelActiveRecast(LivingEntity caster) {
        if (!(caster instanceof ServerPlayer player)) {
            return;
        }
        var recasts = MagicData.getPlayerMagicData(player).getPlayerRecasts();
        RecastInstance recast = recasts.getRecastInstance(SPELL_RESOURCE.toString());
        if (recast != null) {
            recasts.removeRecast(recast, RecastResult.USER_CANCEL);
        }
    }

    private static boolean isTargetAvailable(LivingEntity caster, LivingEntity target) {
        if (!target.hasEffect(MobEffectsRegistry.TIGERSHADE_MARK.get())) {
            return true;
        }

        LivingEntity owner = resolveLinkedEntity(target, TigershadeMarkEffect.MARK_CASTER_UUID_TAG,
                TigershadeMarkEffect.MARK_CASTER_DIM_TAG);
        if (owner != null && owner.isAlive() && owner.hasEffect(MobEffectsRegistry.TIGERSHADE_STANCE.get())
                && hasActiveRecast(owner)
                && isCasterLinkedToTarget(owner, target)) {
            return owner == caster;
        }

        if (owner != null && isCasterLinkedToTarget(owner, target)) {
            clearHunt(owner);
        } else {
            target.getPersistentData().remove(TigershadeMarkEffect.MARK_CASTER_UUID_TAG);
            target.getPersistentData().remove(TigershadeMarkEffect.MARK_CASTER_DIM_TAG);
            target.removeEffect(MobEffectsRegistry.TIGERSHADE_MARK.get());
        }
        return true;
    }

    private static boolean hasActiveRecast(LivingEntity caster) {
        return !(caster instanceof ServerPlayer player)
                || MagicData.getPlayerMagicData(player).getPlayerRecasts().hasRecastForSpell(SPELL_RESOURCE.toString());
    }

    @Nullable
    private LivingEntity findLookTarget(Level level, LivingEntity caster, float range) {
        HitResult hitResult = Utils.raycastForEntity(level, caster, range, true, 0.6F);
        if (hitResult instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity livingTarget
                && isValidTarget(caster, livingTarget)) {
            return livingTarget;
        }

        Vec3 eyePos = caster.getEyePosition();
        Vec3 lookVec = caster.getLookAngle();
        AABB searchBox = caster.getBoundingBox().inflate(range);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                entity -> isValidTarget(caster, entity)
                        && entity.distanceToSqr(caster) <= (double) (range * range));

        LivingEntity bestTarget = null;
        double bestScore = 0.80D;
        for (LivingEntity candidate : candidates) {
            Vec3 toCandidate = candidate.getBoundingBox().getCenter().subtract(eyePos).normalize();
            double dot = lookVec.dot(toCandidate);
            if (dot > bestScore && Utils.hasLineOfSight(level, caster, candidate, true)) {
                bestScore = dot;
                bestTarget = candidate;
            }
        }
        return bestTarget;
    }

    @Nullable
    private static LivingEntity resolveMarkedTarget(LivingEntity caster) {
        LivingEntity target = resolveLinkedEntity(caster, TigershadeStanceEffect.TARGET_UUID_TAG,
                TigershadeStanceEffect.TARGET_DIM_TAG);
        if (target == null || target.level() != caster.level() || !target.isAlive()
                || !target.hasEffect(MobEffectsRegistry.TIGERSHADE_MARK.get())
                || !isOwnedMark(caster, target)) {
            return null;
        }
        return target;
    }

    @Nullable
    private static LivingEntity resolveLinkedEntity(LivingEntity holder, String uuidTag, String dimensionTag) {
        if (holder.getServer() == null || !holder.getPersistentData().contains(uuidTag)
                || !holder.getPersistentData().contains(dimensionTag)) {
            return null;
        }

        try {
            UUID uuid = UUID.fromString(holder.getPersistentData().getString(uuidTag));
            ResourceLocation dimensionId = ResourceLocation.tryParse(holder.getPersistentData().getString(dimensionTag));
            if (dimensionId == null) {
                return null;
            }
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
            ServerLevel level = holder.getServer().getLevel(dimension);
            if (level != null) {
                Entity entity = level.getEntity(uuid);
                if (entity instanceof LivingEntity living) {
                    return living;
                }
            }
        } catch (IllegalArgumentException ignored) {
            // Invalid legacy/stale persistent data is treated as an absent link.
        }
        return null;
    }

    private static boolean isOwnedMark(LivingEntity caster, LivingEntity target) {
        return isLinkedPair(caster, target)
                && caster.level().dimension().location().toString().equals(
                        target.getPersistentData().getString(TigershadeMarkEffect.MARK_CASTER_DIM_TAG))
                && caster.level() == target.level();
    }

    private static boolean isLinkedPair(LivingEntity caster, LivingEntity target) {
        return caster.getStringUUID().equals(
                target.getPersistentData().getString(TigershadeMarkEffect.MARK_CASTER_UUID_TAG))
                && isCasterLinkedToTarget(caster, target);
    }

    private static boolean isCasterLinkedToTarget(LivingEntity caster, LivingEntity target) {
        return target.getStringUUID().equals(
                caster.getPersistentData().getString(TigershadeStanceEffect.TARGET_UUID_TAG))
                && target.level().dimension().location().toString().equals(
                        caster.getPersistentData().getString(TigershadeStanceEffect.TARGET_DIM_TAG));
    }

    private boolean isValidTarget(LivingEntity caster, LivingEntity target) {
        return target != caster
                && target.isAlive()
                && !target.isRemoved()
                && !target.isSpectator()
                && !target.isAlliedTo(caster)
                && !DamageSources.isFriendlyFireBetween(caster, target);
    }

    private static void displayActionBar(LivingEntity caster, String translationKey) {
        if (caster instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable(translationKey), true);
        }
    }
}
