package io.redspace.ironspell_more.spells.ground;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.client.particle.ShockwaveParticleOptionCustom;
import io.redspace.ironspell_more.compat.epicfight.EpicFightCompat;
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
import io.redspace.ironsspellbooks.api.spells.ICastData;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.ImpulseCastData;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.network.debug.PlayPlayerAnimationPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.redspace.ironspell_more.config.SpellConfig;

@AutoSpellConfig
public class TigershadeTerrabreakSpell extends AbstractSpell {
    // ==========================================
    // SPELL TUNING CONSTANTS (Code Defaults)
    // ==========================================
    public static final float BASE_DAMAGE = 30.0F;
    public static final float DAMAGE_PER_LEVEL = 3.0F;
    public static final int BASE_MANA_COST = 40;
    public static final int MANA_COST_PER_LEVEL = 0;
    public static final double COOLDOWN_SECONDS = 30.0D;

    public static final ResourceLocation SPELL_RESOURCE = IronSpellMore.id("tigershade_terrabreak");

    // This is BHSchoolRegistry.GROUND_RESOURCE. Keeping the resource location here avoids
    // hard-loading BHSpells when the optional dependency is not installed.
    private static final ResourceLocation GROUND_SCHOOL_RESOURCE = ResourceLocation.fromNamespaceAndPath(
            "bhspells", "ground");

    public static final float ACQUISITION_RANGE = 20.0F;
    public static final float SLAM_MAX_DISTANCE = 8.0F;
    public static final float EXECUTE_MAX_DISTANCE = SLAM_MAX_DISTANCE;
    public static final float HEAL_ON_DEFEAT = 10.0F;
    public static final float HEAL_ON_EXECUTE = HEAL_ON_DEFEAT;
    public static final int MARK_DURATION_TICKS = 1200;
    public static final int SLAM_COOLDOWN_TICKS = (int) (COOLDOWN_SECONDS * 20);
    public static final int EXECUTE_COOLDOWN_TICKS = SLAM_COOLDOWN_TICKS;

    private static final double SLAM_FRACTURE_RADIUS = 3.0D;
    private static final double SLAM_DASH_SPEED = 1.35D;
    private static final int SLAM_DASH_TIMEOUT_TICKS = 10;
    private static final double SLAM_DASH_MAX_TRAVEL = SLAM_MAX_DISTANCE + SLAM_DASH_SPEED;
    private static final double SLAM_DASH_COLLISION_SAMPLE_DISTANCE = 0.25D;
    private static final double SLAM_DASH_FLOOR_CLEARANCE = 0.05D;
    private static final double SLAM_DASH_SIDE_CLEARANCE = 0.001D;
    private static final Map<UUID, SlamDashState> ACTIVE_SLAM_DASHES = new HashMap<>();

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(GROUND_SCHOOL_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(COOLDOWN_SECONDS)
            .build();

    public TigershadeTerrabreakSpell() {
        this.baseSpellPower = (int) BASE_DAMAGE;
        this.spellPowerPerLevel = (int) DAMAGE_PER_LEVEL;
        this.baseManaCost = BASE_MANA_COST;
        this.manaCostPerLevel = MANA_COST_PER_LEVEL;
        this.castTime = 0;
    }

    @Override
    public int getManaCost(int spellLevel) {
        return SpellConfig.TigershadeTerrabreak.getBaseMana() + (spellLevel - 1) * SpellConfig.TigershadeTerrabreak.getManaPerLevel();
    }

    @Override
    public int getSpellCooldown() {
        return (int) (SpellConfig.TigershadeTerrabreak.getCooldown() * 20);
    }

    public float getDamage(int spellLevel, LivingEntity caster) {
        float base = SpellConfig.TigershadeTerrabreak.getBaseDamage();
        float perLevel = SpellConfig.TigershadeTerrabreak.getDamagePerLevel();
        return (base + (spellLevel - 1) * perLevel) * getEntityPowerMultiplier(caster);
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
    public ICastDataSerializable getEmptyCastData() {
        return new ImpulseCastData();
    }

    @Override
    public void onClientCast(Level level, int spellLevel, LivingEntity caster, ICastData castData) {
        if (castData instanceof ImpulseCastData impulseData && impulseData.hasImpulse) {
            caster.hasImpulse = true;
            caster.setDeltaMovement(impulseData.x, impulseData.y, impulseData.z);
        }
        super.onClientCast(level, spellLevel, caster, castData);
    }

    @Override
    public SchoolType getSchoolType() {
        SchoolType school = SchoolRegistry.getSchool(GROUND_SCHOOL_RESOURCE);
        return school != null ? school : SchoolRegistry.EVOCATION.get();
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.ironspell_more.tigershade_slam_damage",
                        Utils.stringTruncation(getDamage(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.distance",
                        Utils.stringTruncation(SLAM_MAX_DISTANCE, 1)),
                Component.translatable("ui.irons_spellbooks.duration",
                        Utils.timeFromTicks(MARK_DURATION_TICKS, 1)),
                Component.translatable("ui.ironspell_more.tigershade_heal_on_defeat",
                        Utils.stringTruncation(HEAL_ON_DEFEAT, 1)),
                Component.translatable("ui.irons_spellbooks.cooldown",
                        Utils.timeFromTicks(SLAM_COOLDOWN_TICKS, 1)));
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

        if (caster.distanceToSqr(target) > SLAM_MAX_DISTANCE * SLAM_MAX_DISTANCE) {
            displayActionBar(caster, "ui.ironspell_more.tigershade_too_far");
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
            if (canSlam(caster, target) && level instanceof ServerLevel serverLevel
                    && caster instanceof ServerPlayer player) {
                startSlamDash(serverLevel, player, target, spellLevel, magicData);
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

    private boolean canSlam(LivingEntity caster, @Nullable LivingEntity target) {
        return target != null
                && target.isAlive()
                && caster.level() == target.level()
                && caster.distanceToSqr(target) <= SLAM_MAX_DISTANCE * SLAM_MAX_DISTANCE
                && isOwnedMark(caster, target);
    }

    private void startSlamDash(ServerLevel level, ServerPlayer caster, LivingEntity target, int spellLevel,
            MagicData magicData) {
        cancelActiveSlamDash(caster);
        Vec3 initialVelocity = calculateDashVelocity(caster, target);
        SlamDashState state = new SlamDashState(this, level, caster, target, spellLevel, caster.position());

        // Consume the hunt as soon as the dash is committed. The captured state keeps
        // only this bounded attempt alive while the normal recast/cooldown flow proceeds.
        clearHunt(caster);

        faceTarget(caster, getFacingYaw(caster.position(), target, caster.getYRot()));
        playIronSpellAnimation(caster, SpellAnimations.OVERHEAD_MELEE_SWING_ANIMATION);
        applyDashVelocity(caster, initialVelocity);
        magicData.setAdditionalCastData(new ImpulseCastData(
                (float) initialVelocity.x, (float) initialVelocity.y, (float) initialVelocity.z, true));
        ACTIVE_SLAM_DASHES.put(caster.getUUID(), state);
    }

    public static void tickActiveSlamDash(ServerPlayer caster) {
        SlamDashState state = ACTIVE_SLAM_DASHES.get(caster.getUUID());
        if (state != null) {
            state.spell.tickSlamDash(state);
        }
    }

    private void tickSlamDash(SlamDashState state) {
        if (state.terminal) {
            return;
        }

        Entity currentCaster = state.level.getEntity(state.casterId);
        Entity currentTarget = state.level.getEntity(state.targetId);
        if (currentCaster != state.caster || currentTarget != state.target
                || !state.caster.isAlive() || state.caster.isRemoved() || state.caster.isSpectator()
                || !isValidTarget(state.caster, state.target)
                || state.caster.level() != state.level || state.target.level() != state.level
                || !state.level.getWorldBorder().isWithinBounds(state.caster.getBoundingBox())) {
            abortSlamDash(state);
            return;
        }

        Vec3 currentPosition = state.caster.position();
        state.cumulativeTravel += currentPosition.distanceTo(state.lastPosition);
        state.lastPosition = currentPosition;

        if (state.cumulativeTravel > SLAM_DASH_MAX_TRAVEL + 1.0E-6D) {
            abortSlamDash(state);
            return;
        }

        if (hasReachedSlamTarget(state.caster, state.target)) {
            completeSlamDashState(state);
            finishSlamImpact(state.level, state.caster, state.target, state.spellLevel);
            return;
        }

        if (state.ticksElapsed >= SLAM_DASH_TIMEOUT_TICKS
                || state.cumulativeTravel >= SLAM_DASH_MAX_TRAVEL
                || state.caster.horizontalCollision) {
            abortSlamDash(state);
            return;
        }

        Vec3 velocity = calculateDashVelocity(state.caster, state.target);
        double remainingTravel = SLAM_DASH_MAX_TRAVEL - state.cumulativeTravel;
        if (velocity.length() > remainingTravel) {
            velocity = velocity.normalize().scale(remainingTravel);
        }
        if (velocity.lengthSqr() < 1.0E-6D || isDashPathBlocked(state.level, state.caster, velocity)) {
            abortSlamDash(state);
            return;
        }

        faceTarget(state.caster,
                getFacingYaw(state.caster.position(), state.target, state.caster.getYRot()));
        applyDashVelocity(state.caster, velocity);
        state.ticksElapsed++;
    }

    private static Vec3 calculateDashVelocity(LivingEntity caster, LivingEntity target) {
        Vec3 toTarget = target.getBoundingBox().getCenter().subtract(caster.getBoundingBox().getCenter());
        double distance = toTarget.length();
        if (distance < 1.0E-6D) {
            return Vec3.ZERO;
        }

        double stepDistance = Math.min(SLAM_DASH_SPEED, distance);
        Vec3 velocity = toTarget.scale(stepDistance / distance);
        if (caster.onGround() && velocity.y < 0.15D) {
            velocity = new Vec3(velocity.x, 0.15D, velocity.z);
        }
        if (velocity.lengthSqr() > SLAM_DASH_SPEED * SLAM_DASH_SPEED) {
            velocity = velocity.normalize().scale(SLAM_DASH_SPEED);
        }
        return velocity;
    }

    private static boolean isDashPathBlocked(ServerLevel level, LivingEntity caster, Vec3 velocity) {
        AABB bounds = caster.getBoundingBox();
        AABB sampleBounds = new AABB(
                bounds.minX + SLAM_DASH_SIDE_CLEARANCE,
                bounds.minY + SLAM_DASH_FLOOR_CLEARANCE,
                bounds.minZ + SLAM_DASH_SIDE_CLEARANCE,
                bounds.maxX - SLAM_DASH_SIDE_CLEARANCE,
                bounds.maxY - SLAM_DASH_SIDE_CLEARANCE,
                bounds.maxZ - SLAM_DASH_SIDE_CLEARANCE);
        int samples = Math.max(1,
                (int) Math.ceil(velocity.length() / SLAM_DASH_COLLISION_SAMPLE_DISTANCE));

        for (int sample = 1; sample <= samples; sample++) {
            Vec3 offset = velocity.scale(sample / (double) samples);
            AABB movedBounds = sampleBounds.move(offset);
            if (!level.getWorldBorder().isWithinBounds(movedBounds)
                    || level.getBlockCollisions(caster, movedBounds).iterator().hasNext()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasReachedSlamTarget(LivingEntity caster, LivingEntity target) {
        return caster.getBoundingBox().intersects(target.getBoundingBox());
    }

    private static void applyDashVelocity(LivingEntity caster, Vec3 velocity) {
        caster.hasImpulse = true;
        caster.setDeltaMovement(velocity);
        caster.hurtMarked = true;
        caster.resetFallDistance();
    }

    private static void abortSlamDash(SlamDashState state) {
        if (state.terminal) {
            return;
        }
        completeSlamDashState(state);

        if (state.level.getEntity(state.casterId) == state.caster) {
            stopDashMotion(state.caster);
        }
    }

    public static void cancelActiveSlamDash(LivingEntity caster) {
        SlamDashState state = ACTIVE_SLAM_DASHES.remove(caster.getUUID());
        if (state == null || state.terminal) {
            return;
        }
        state.terminal = true;
        if (state.level.getEntity(state.casterId) == state.caster) {
            stopDashMotion(state.caster);
        }
    }

    private static void completeSlamDashState(SlamDashState state) {
        state.terminal = true;
        ACTIVE_SLAM_DASHES.remove(state.casterId, state);
    }

    private static void stopDashMotion(LivingEntity caster) {
        caster.setDeltaMovement(Vec3.ZERO);
        caster.hurtMarked = true;
        caster.resetFallDistance();
    }

    private void finishSlamImpact(Level level, LivingEntity caster, LivingEntity target, int spellLevel) {
        stopDashMotion(caster);
        faceTarget(caster, getFacingYaw(caster.position(), target, caster.getYRot()));
        playIronSpellAnimation(caster, SpellAnimations.TOUCH_GROUND_ANIMATION);

        Vec3 impactPosition = target.position();

        target.setDeltaMovement(0.0D, -1.2D, 0.0D);
        target.hurtMarked = true;

        float damage = getDamage(spellLevel, caster);
        boolean damageAccepted = DamageSources.applyDamage(target, damage, getDamageSource(caster));

        // Epic Fight supplies the true block fracture when available. The vanilla
        // impact particles below remain visible when the optional mod is absent.
        try {
            EpicFightCompat.spawnFracture(caster, level, impactPosition, 1, 3, SLAM_FRACTURE_RADIUS);
        } catch (RuntimeException exception) {
            IronSpellMore.LOGGER.error("Tigershade ground fracture failed; continuing spell impact", exception);
        }
        playSlamEffects(level, target, impactPosition);

        // Heal on defeat if the target died from this slam
        if (damageAccepted && !target.isAlive()) {
            caster.heal(HEAL_ON_DEFEAT);
        }

    }

    private static void playIronSpellAnimation(LivingEntity caster, AnimationHolder animation) {
        if (!(caster instanceof ServerPlayer)) {
            return;
        }
        animation.getForPlayer().ifPresent(animationId ->
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(caster,
                        new PlayPlayerAnimationPacket(caster.getUUID(), animationId)));
    }

    private static float getFacingYaw(Vec3 casterPosition, LivingEntity target, float fallbackYaw) {
        double deltaX = target.getX() - casterPosition.x;
        double deltaZ = target.getZ() - casterPosition.z;
        if (deltaX * deltaX + deltaZ * deltaZ <= 1.0E-6D) {
            return fallbackYaw;
        }
        return (float) (Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0D);
    }

    private static void faceTarget(LivingEntity caster, float yaw) {
        caster.setYRot(yaw);
        caster.setYHeadRot(yaw);
        caster.setYBodyRot(yaw);
    }

    private static final class SlamDashState {
        private final TigershadeTerrabreakSpell spell;
        private final ServerLevel level;
        private final ServerPlayer caster;
        private final LivingEntity target;
        private final UUID casterId;
        private final UUID targetId;
        private final int spellLevel;
        private Vec3 lastPosition;
        private double cumulativeTravel;
        private int ticksElapsed = 1;
        private boolean terminal;

        private SlamDashState(TigershadeTerrabreakSpell spell, ServerLevel level, ServerPlayer caster,
                LivingEntity target, int spellLevel, Vec3 startPosition) {
            this.spell = spell;
            this.level = level;
            this.caster = caster;
            this.target = target;
            this.casterId = caster.getUUID();
            this.targetId = target.getUUID();
            this.spellLevel = spellLevel;
            this.lastPosition = startPosition;
        }
    }

    private void playSlamEffects(Level level, LivingEntity target, Vec3 impactPosition) {
        double impactX = impactPosition.x;
        double impactY = impactPosition.y;
        double impactZ = impactPosition.z;

        // Heavy visceral impact sound layering (removed anvil, added heavy sub-bass boom & physical shock)
        level.playSound(null, impactX, impactY, impactZ,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0F, 0.60F);
        level.playSound(null, impactX, impactY, impactZ,
                SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.PLAYERS, 2.0F, 0.70F);
        level.playSound(null, impactX, impactY, impactZ,
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.4F, 0.80F);
        level.playSound(null, impactX, impactY, impactZ,
                SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 1.6F, 0.65F);

        if (level instanceof ServerLevel serverLevel) {
            // 1. Concentric Ground Shockwaves (Outer Purple, Mid Magenta, Inner Gold)
            serverLevel.sendParticles(new ShockwaveParticleOptionCustom(
                            new Vector3f(0.58F, 0.12F, 0.92F), 6.8F, true, new Vector3f(0, 1, 0)),
                    impactX, impactY + 0.12D, impactZ, 1, 0, 0, 0, 0);
            serverLevel.sendParticles(new ShockwaveParticleOptionCustom(
                            new Vector3f(0.85F, 0.20F, 0.95F), 5.0F, true, new Vector3f(0, 1, 0)),
                    impactX, impactY + 0.15D, impactZ, 1, 0, 0, 0, 0);
            serverLevel.sendParticles(new ShockwaveParticleOptionCustom(
                            new Vector3f(1.0F, 0.82F, 0.18F), 3.2F, true, new Vector3f(0, 1, 0)),
                    impactX, impactY + 0.18D, impactZ, 1, 0, 0, 0, 0);

            // 2. Giant Hemispherical Violet / Purple Energy Dome (Canopy)
            double domeRadius = 5.2D;
            Vector3f deepPurple = new Vector3f(0.55F, 0.10F, 0.88F);
            Vector3f brightMagenta = new Vector3f(0.85F, 0.22F, 0.95F);
            DustParticleOptions purpleDust = new DustParticleOptions(deepPurple, 1.8F);
            DustParticleOptions magentaDust = new DustParticleOptions(brightMagenta, 1.4F);

            // Dome surface shell (elevation rings from horizon to apex)
            for (int ring = 1; ring <= 7; ring++) {
                double phi = (ring / 8.0D) * (Math.PI * 0.5D);
                double ringY = domeRadius * Math.sin(phi);
                double ringR = domeRadius * Math.cos(phi);
                int points = (int) Math.max(8, Math.round(ringR * 6.5D));

                for (int p = 0; p < points; p++) {
                    double theta = p * (Math.PI * 2.0D / points);
                    double px = impactX + ringR * Math.cos(theta);
                    double py = impactY + ringY;
                    double pz = impactZ + ringR * Math.sin(theta);

                    // Dual-color purple/magenta energy rim
                    if (p % 2 == 0) {
                        serverLevel.sendParticles(purpleDust, px, py, pz, 1, 0, 0, 0, 0);
                    } else {
                        serverLevel.sendParticles(magentaDust, px, py, pz, 1, 0, 0, 0, 0);
                    }

                    // Billowing dragon breath along the dome canopy
                    if ((p + ring) % 3 == 0) {
                        double vx = (ringR * Math.cos(theta) / domeRadius) * 0.08D;
                        double vy = (ringY / domeRadius) * 0.08D;
                        double vz = (ringR * Math.sin(theta) / domeRadius) * 0.08D;
                        serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, px, py, pz, 1, vx, vy, vz, 0.03D);
                    }

                    // Twinkling purple witch stars across the dome
                    if ((p + ring) % 5 == 0) {
                        serverLevel.sendParticles(ParticleTypes.WITCH, px, py, pz, 1, 0, 0, 0, 0);
                    }
                }
            }

            // 3. Curved Golden Tiger Slash Arcs (Sweeping upward & outward from ground zero)
            Vector3f goldColor = new Vector3f(1.0F, 0.85F, 0.18F);
            DustParticleOptions goldDust = new DustParticleOptions(goldColor, 1.6F);
            int arcCount = 8;
            for (int arc = 0; arc < arcCount; arc++) {
                double baseAngle = arc * (Math.PI * 2.0D / arcCount);
                for (int step = 0; step <= 14; step++) {
                    double progress = step / 14.0D;
                    double arcRadius = 0.4D + 3.8D * Math.pow(progress, 0.85D);
                    double arcAngle = baseAngle + 1.25D * progress;
                    double arcHeight = 0.2D + 3.2D * Math.pow(progress, 1.2D);

                    double px = impactX + arcRadius * Math.cos(arcAngle);
                    double py = impactY + arcHeight;
                    double pz = impactZ + arcRadius * Math.sin(arcAngle);

                    serverLevel.sendParticles(goldDust, px, py, pz, 1, 0, 0, 0, 0);
                    serverLevel.sendParticles(ParticleTypes.END_ROD, px, py, pz, 1, 0, 0, 0, 0);
                    if (step % 2 == 0) {
                        serverLevel.sendParticles(ParticleTypes.FLAME, px, py, pz, 1, 0.02D, 0.02D, 0.02D, 0.01D);
                    }
                }
            }

            // 4. Epicenter Core Flame & Flash Eruption
            serverLevel.sendParticles(ParticleTypes.FLASH,
                    impactX, impactY + target.getBbHeight() * 0.5D, impactZ, 1, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    impactX, impactY + 0.5D, impactZ, 1, 0, 0, 0, 0);
            serverLevel.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.65F, 0.10F), 2.4F),
                    impactX, impactY + 0.8D, impactZ, 55, 0.8D, 1.0D, 0.8D, 0.2D);
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    impactX, impactY + 0.6D, impactZ, 45, 0.7D, 0.9D, 0.7D, 0.18D);
            serverLevel.sendParticles(ParticleTypes.LAVA,
                    impactX, impactY + 0.5D, impactZ, 14, 0.5D, 0.5D, 0.5D, 0.15D);

            // 5. Shattered Dark Blackstone Shards & Dirt Bursting along the Crater Rim
            for (int i = 0; i < 28; i++) {
                double angle = i * (Math.PI * 2.0D / 28.0D);
                double rimDist = 2.6D + (i % 3) * 0.45D;
                double rx = impactX + Math.cos(angle) * rimDist;
                double rz = impactZ + Math.sin(angle) * rimDist;
                double vx = Math.cos(angle) * 0.35D;
                double vz = Math.sin(angle) * 0.35D;
                double vy = 0.32D + (i % 4) * 0.08D;

                serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLACKSTONE.defaultBlockState()),
                        rx, impactY + 0.1D, rz, 4, vx, vy, vz, 0.22D);
                serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COARSE_DIRT.defaultBlockState()),
                        rx, impactY + 0.1D, rz, 2, vx * 0.7D, vy * 0.7D, vz * 0.7D, 0.15D);
            }
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
