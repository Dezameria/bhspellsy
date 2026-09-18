package io.redspace.ironspell_more.entity.spells.venomous_blossomfall;

import io.redspace.ironspell_more.client.particle.ShockwaveParticleOptionCustom;
import io.redspace.ironspell_more.compat.epicfight.EpicFightFractureHelper;
import io.redspace.ironspell_more.registry.EntityRegistry;
import io.redspace.ironspell_more.registry.MobEffectsRegistry;
import io.redspace.ironspell_more.registry.SpellRegistry;
import io.redspace.ironspell_more.spells.nature.VenomousBlossomfallSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.capabilities.magic.CooldownInstance;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.particle.SparkParticleOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class AzureVenomNeedleEntity extends AbstractMagicProjectile {
    private static final EntityDataAccessor<Float> DATA_DIRECTION_X =
            SynchedEntityData.defineId(AzureVenomNeedleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIRECTION_Y =
            SynchedEntityData.defineId(AzureVenomNeedleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIRECTION_Z =
            SynchedEntityData.defineId(AzureVenomNeedleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SPEED =
            SynchedEntityData.defineId(AzureVenomNeedleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_CHARGE =
            SynchedEntityData.defineId(AzureVenomNeedleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_FULL_CHARGE =
            SynchedEntityData.defineId(AzureVenomNeedleEntity.class, EntityDataSerializers.BOOLEAN);

    private static final DustParticleOptions AZURE_DUST =
            new DustParticleOptions(new Vector3f(0.05F, 0.95F, 0.78F), 1.0F);
    private static final SparkParticleOptions AZURE_SPARKS =
            new SparkParticleOptions(new Vector3f(0.05F, 1.0F, 0.8F));

    private double traveledDistance;
    private double nextFractureDistance = VenomousBlossomfallSpell.FRACTURE_SAMPLE_INTERVAL;
    private double fractureOriginY;
    private int clientSonicRingIndex;
    private boolean resolved;

    public AzureVenomNeedleEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public AzureVenomNeedleEntity(Level level, LivingEntity owner) {
        this(EntityRegistry.AZURE_VENOM_NEEDLE.get(), level);
        setOwner(owner);
    }

    public void configure(Vec3 direction, double speed, float chargeProgress) {
        Vec3 normalizedDirection = direction.lengthSqr() < 1.0E-8D ? new Vec3(0.0D, 0.0D, 1.0D)
                : direction.normalize();
        setDirection(normalizedDirection);
        setSpeed(speed);
        setChargeProgress(chargeProgress);
        entityData.set(DATA_FULL_CHARGE, VenomousBlossomfallSpell.isFullCharge(chargeProgress));
        setDeltaMovement(normalizedDirection.scale(speed));
        LivingEntity owner = getOwner() instanceof LivingEntity livingOwner ? livingOwner : null;
        fractureOriginY = owner != null ? owner.getY() + 0.2D : getY() - 1.2D;
    }

    public Vec3 getFlightDirection() {
        Vec3 direction = new Vec3(entityData.get(DATA_DIRECTION_X), entityData.get(DATA_DIRECTION_Y),
                entityData.get(DATA_DIRECTION_Z));
        return direction.lengthSqr() < 1.0E-8D ? getDeltaMovement().normalize() : direction.normalize();
    }

    private void setDirection(Vec3 direction) {
        entityData.set(DATA_DIRECTION_X, (float) direction.x);
        entityData.set(DATA_DIRECTION_Y, (float) direction.y);
        entityData.set(DATA_DIRECTION_Z, (float) direction.z);
    }

    public float getChargeProgress() {
        return entityData.get(DATA_CHARGE);
    }

    private void setChargeProgress(float chargeProgress) {
        entityData.set(DATA_CHARGE, VenomousBlossomfallSpell.normalizeCharge(chargeProgress));
    }

    public boolean isFullCharge() {
        return entityData.get(DATA_FULL_CHARGE);
    }

    public double getProjectileSpeed() {
        return entityData.get(DATA_SPEED);
    }

    private void setSpeed(double speed) {
        entityData.set(DATA_SPEED, (float) speed);
    }

    @Override
    public float getSpeed() {
        return entityData.get(DATA_SPEED);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0D;
    }

    /**
     * AbstractMagicProjectile still owns spawning, friendly-fire filtering, anti-magic,
     * trail hooks and rotation. This override replaces its movement pass with a
     * range-clamped continuous sweep.
     */
    @Override
    public void handleHitDetection() {
        Vec3 direction = getFlightDirection();
        double projectileRange = getProjectileRange();
        double remainingRange = projectileRange - traveledDistance;
        if (resolved || remainingRange <= 1.0E-6D || direction.lengthSqr() < 1.0E-8D) {
            if (!level().isClientSide) {
                resolveMiss(position());
            }
            return;
        }

        double stepLength = Math.min(getProjectileSpeed(), remainingRange);
        Vec3 previousPosition = position();
        Vec3 intendedNextPosition = previousPosition.add(direction.scale(stepLength));

        if (level().isClientSide) {
            double nextDistance = traveledDistance + stepLength;
            spawnFullChargeSonicRingsAlongPath(previousPosition, intendedNextPosition, traveledDistance,
                    nextDistance);
            setDeltaMovement(direction.scale(getProjectileSpeed()));
            setPos(intendedNextPosition);
            traveledDistance = nextDistance;
            return;
        }

        BlockHitResult blockHit = level().clip(new ClipContext(previousPosition, intendedNextPosition,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 entitySweepEnd = blockHit.getType() == HitResult.Type.MISS
                ? intendedNextPosition
                : blockHit.getLocation();

        List<HitResult> entityHits = raycastForEntitiesAlongPath(entitySweepEnd, previousPosition);
        for (HitResult hit : entityHits) {
            if (hit instanceof EntityHitResult entityHit
                    && !MinecraftForge.EVENT_BUS.post(new ProjectileImpactEvent(this, entityHit))) {
                moveAndSampleFractures(previousPosition, entityHit.getLocation());
                setPos(entityHit.getLocation());
                onHit(entityHit);
                return;
            }
        }

        if (blockHit.getType() != HitResult.Type.MISS) {
            moveAndSampleFractures(previousPosition, blockHit.getLocation());
            setPos(blockHit.getLocation());
            onHit(blockHit);
            return;
        }

        moveAndSampleFractures(previousPosition, intendedNextPosition);
        setPos(intendedNextPosition);
        setDeltaMovement(direction.scale(getProjectileSpeed()));

        if (traveledDistance + 1.0E-6D >= projectileRange) {
            resolveMiss(intendedNextPosition);
        }
    }

    @Override
    public void travel() {
        // Movement is completed inside handleHitDetection so the collision segment and
        // traveled-distance accounting always refer to the same start/end positions.
    }

    private void moveAndSampleFractures(Vec3 start, Vec3 end) {
        double segmentLength = start.distanceTo(end);
        if (segmentLength <= 1.0E-8D) {
            return;
        }

        double previousDistance = traveledDistance;
        double newDistance = Math.min(getProjectileRange(),
                previousDistance + segmentLength);

        if (isFullCharge() && getOwner() instanceof LivingEntity owner) {
            while (nextFractureDistance <= newDistance + 1.0E-6D) {
                double segmentProgress = Mth.clamp((nextFractureDistance - previousDistance) / segmentLength,
                        0.0D, 1.0D);
                Vec3 sample = start.lerp(end, segmentProgress);
                Vec3 fractureSample = new Vec3(sample.x, fractureOriginY, sample.z);
                EpicFightFractureHelper.trySpawnFracture(owner, level(), fractureSample, 2, 6,
                        VenomousBlossomfallSpell.FRACTURE_RADIUS);
                nextFractureDistance += VenomousBlossomfallSpell.FRACTURE_SAMPLE_INTERVAL;
            }
        }

        traveledDistance = newDistance;
    }

    private double getProjectileRange() {
        return VenomousBlossomfallSpell.projectileRange(getChargeProgress());
    }

    /**
     * Emits shockwaves continuously at fixed distances along the swept client
     * movement segment. Distance sampling keeps the rings following the needle at
     * even spacing when it crosses several blocks in one tick.
     */
    private void spawnFullChargeSonicRingsAlongPath(Vec3 start, Vec3 end, double previousDistance,
            double newDistance) {
        if (!isFullCharge()) {
            return;
        }

        double segmentLength = start.distanceTo(end);
        if (segmentLength <= 1.0E-8D) {
            return;
        }

        Vec3 direction = getFlightDirection();
        Vector3f particleDirection = new Vector3f((float) direction.x, (float) direction.y,
                (float) direction.z);

        while (true) {
            double ringDistance = (clientSonicRingIndex + 1)
                    * VenomousBlossomfallSpell.FULL_CHARGE_SONIC_RING_INTERVAL;
            if (ringDistance > newDistance + 1.0E-6D) {
                break;
            }

            double segmentProgress = Mth.clamp((ringDistance - previousDistance) / segmentLength,
                    0.0D, 1.0D);
            Vec3 ringPosition = start.lerp(end, segmentProgress);
            float radius = VenomousBlossomfallSpell.FULL_CHARGE_SONIC_RING_BASE_RADIUS
                    + (clientSonicRingIndex % VenomousBlossomfallSpell.FULL_CHARGE_SONIC_RING_RADIUS_STEPS)
                    * VenomousBlossomfallSpell.FULL_CHARGE_SONIC_RING_RADIUS_STEP;

            level().addParticle(new ShockwaveParticleOptionCustom(
                    new Vector3f(0.05F, 0.95F, 0.78F), radius, true, particleDirection),
                    ringPosition.x, ringPosition.y, ringPosition.z, 0.0D, 0.0D, 0.0D);
            clientSonicRingIndex++;
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        if (resolved || isRemoved()) {
            return;
        }

        Entity hitEntity = result.getEntity();
        LivingEntity effectTarget = getLivingTarget(hitEntity);
        DamageSources.applyDamage(hitEntity, VenomousBlossomfallSpell.directDamage(getChargeProgress()),
                SpellRegistry.VENOMOUS_BLOSSOMFALL_SPELL.get().getDamageSource(this, getOwner()));

        if (effectTarget != null) {
            effectTarget.addEffect(new MobEffectInstance(MobEffectsRegistry.AZURE_VENOMOUS.get(),
                    VenomousBlossomfallSpell.AZURE_VENOM_DURATION_TICKS, 0, false, true, true));
        }

        resolved = true;
        applyCooldownMarker(true);
        upgradeOwnerCooldownForDirectHit();
        discard();
    }

    private LivingEntity getLivingTarget(Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            return livingEntity;
        }
        if (entity instanceof PartEntity<?> part && part.getParent() instanceof LivingEntity livingEntity) {
            return livingEntity;
        }
        return null;
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult result) {
        super.onHitBlock(result);
        resolveMiss(result.getLocation());
    }

    private void resolveMiss(Vec3 location) {
        if (resolved) {
            return;
        }
        resolved = true;
        setPos(location);
        applyCooldownMarker(false);
        discard();
    }

    private void applyCooldownMarker(boolean directHit) {
        if (!(getOwner() instanceof LivingEntity owner) || level().isClientSide) {
            return;
        }

        int durationSeconds = directHit
                ? VenomousBlossomfallSpell.DIRECT_HIT_COOLDOWN_SECONDS
                : VenomousBlossomfallSpell.MISS_COOLDOWN_SECONDS;
        int amplifier = VenomousBlossomfallSpell.cooldownEffectAmplifier(getChargeProgress(), directHit);

        owner.removeEffect(MobEffectsRegistry.COOLDOWN.get());
        owner.addEffect(new MobEffectInstance(MobEffectsRegistry.COOLDOWN.get(), durationSeconds * 20,
                amplifier, false, false, true));
    }

    private void upgradeOwnerCooldownForDirectHit() {
        if (!(getOwner() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        var spell = SpellRegistry.VENOMOUS_BLOSSOMFALL_SPELL.get();
        var cooldowns = MagicData.getPlayerMagicData(serverPlayer).getPlayerCooldowns();
        CooldownInstance existing = cooldowns.getSpellCooldowns().get(spell.getSpellId());
        if (existing == null) {
            return;
        }

        int hitCooldown = existing.getSpellCooldown()
                * VenomousBlossomfallSpell.DIRECT_HIT_COOLDOWN_SECONDS
                / VenomousBlossomfallSpell.MISS_COOLDOWN_SECONDS;
        cooldowns.addCooldown(spell, hitCooldown);
        cooldowns.syncToPlayer(serverPlayer);
    }

    @Override
    public void trailParticles() {
        Vec3 direction = getFlightDirection();
        if (direction.lengthSqr() < 1.0E-8D) {
            return;
        }

        float charge = getChargeProgress();
        int count = 3 + (int) (charge * 7.0F);
        double trailLength = Math.min(5.0D, 1.2D + getProjectileSpeed() * 0.7D + charge * 1.5D);
        Vec3 axis = Math.abs(direction.y) > 0.95D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = direction.cross(axis).normalize();
        Vec3 up = right.cross(direction).normalize();

        for (int index = 0; index < count; index++) {
            double distance = trailLength * index / Math.max(1.0D, count - 1.0D);
            double angle = (tickCount * 0.8D) + index * 1.7D;
            double radius = 0.035D + charge * 0.065D;
            Vec3 spiral = right.scale(Math.cos(angle) * radius).add(up.scale(Math.sin(angle) * radius));
            Vec3 particlePosition = position().subtract(direction.scale(distance)).add(spiral);
            level().addParticle(AZURE_DUST, true, particlePosition.x, particlePosition.y, particlePosition.z,
                    0.0D, 0.0D, 0.0D);
            if (index % 3 == 0) {
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, true,
                        particlePosition.x, particlePosition.y, particlePosition.z,
                        -direction.x * 0.03D, -direction.y * 0.03D, -direction.z * 0.03D);
            }
        }
    }

    @Override
    public void impactParticles(double x, double y, double z) {
        MagicManager.spawnParticles(level(), AZURE_SPARKS, x, y, z, 18, 0.2D, 0.2D, 0.2D, 0.18D, true);
        MagicManager.spawnParticles(level(), AZURE_DUST, x, y, z, 12, 0.15D, 0.15D, 0.15D, 0.08D, true);
    }

    @Override
    public Optional<Supplier<SoundEvent>> getImpactSound() {
        return Optional.of(() -> SoundEvents.TRIDENT_HIT);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_DIRECTION_X, 0.0F);
        entityData.define(DATA_DIRECTION_Y, 0.0F);
        entityData.define(DATA_DIRECTION_Z, 1.0F);
        entityData.define(DATA_SPEED, (float) VenomousBlossomfallSpell.SHORT_CHARGE_SPEED);
        entityData.define(DATA_CHARGE, 0.0F);
        entityData.define(DATA_FULL_CHARGE, false);
        super.defineSynchedData();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        Vec3 direction = getFlightDirection();
        tag.putDouble("DirectionX", direction.x);
        tag.putDouble("DirectionY", direction.y);
        tag.putDouble("DirectionZ", direction.z);
        tag.putDouble("NeedleSpeed", getProjectileSpeed());
        tag.putFloat("ChargeProgress", getChargeProgress());
        tag.putBoolean("FullCharge", isFullCharge());
        tag.putDouble("TraveledDistance", traveledDistance);
        tag.putDouble("NextFractureDistance", nextFractureDistance);
        tag.putDouble("FractureOriginY", fractureOriginY);
        tag.putBoolean("Resolved", resolved);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setDirection(new Vec3(tag.getDouble("DirectionX"), tag.getDouble("DirectionY"),
                tag.getDouble("DirectionZ")));
        setSpeed(tag.getDouble("NeedleSpeed"));
        setChargeProgress(tag.getFloat("ChargeProgress"));
        entityData.set(DATA_FULL_CHARGE, tag.getBoolean("FullCharge"));
        traveledDistance = tag.getDouble("TraveledDistance");
        nextFractureDistance = tag.contains("NextFractureDistance")
                ? tag.getDouble("NextFractureDistance")
                : VenomousBlossomfallSpell.FRACTURE_SAMPLE_INTERVAL;
        fractureOriginY = tag.contains("FractureOriginY")
                ? tag.getDouble("FractureOriginY")
                : (getOwner() != null ? getOwner().getY() + 0.2D : getY() - 1.2D);
        resolved = tag.getBoolean("Resolved");
        setDeltaMovement(getFlightDirection().scale(getProjectileSpeed()));
    }
}
