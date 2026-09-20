package io.redspace.ironspell_more.entity.spells.gale_piercer;

import io.redspace.ironspell_more.registry.SpellRegistry;
import io.redspace.ironspell_more.spells.nature.GalePiercerSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractGalePiercerArrowEntity extends AbstractMagicProjectile {
    protected static final EntityDataAccessor<Integer> DATA_TARGET_ID =
            SynchedEntityData.defineId(AbstractGalePiercerArrowEntity.class, EntityDataSerializers.INT);

    public static final int MAX_LIFETIME_TICKS = 300; // 15 seconds max

    @Nullable
    protected UUID targetUUID;
    @Nullable
    protected LivingEntity cachedTarget;
    protected float damageAmount;
    protected int spellLevel = 1;
    protected boolean resolved;
    protected int ticksAlive;

    public AbstractGalePiercerArrowEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public AbstractGalePiercerArrowEntity(EntityType<? extends Projectile> type, Level level, LivingEntity owner) {
        this(type, level);
        setOwner(owner);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_TARGET_ID, -1);
    }

    public void initializeArrow(@Nullable LivingEntity target, float damage, int spellLevel, Vec3 initialDirection, double initialSpeed) {
        this.cachedTarget = target;
        if (target != null) {
            this.targetUUID = target.getUUID();
            this.entityData.set(DATA_TARGET_ID, target.getId());
        }
        this.damageAmount = damage;
        this.spellLevel = spellLevel;

        Vec3 dir = initialDirection.lengthSqr() < 1.0E-6D ? new Vec3(0, 0, 1) : initialDirection.normalize();
        setDeltaMovement(dir.scale(initialSpeed));
        updateRotationFromMotion(dir);
    }

    protected void updateRotationFromMotion(Vec3 motion) {
        if (motion.lengthSqr() > 1.0E-6D) {
            double horizontalMag = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
            setYRot((float) (Mth.atan2(motion.x, motion.z) * (180.0D / Math.PI)));
            setXRot((float) (Mth.atan2(motion.y, horizontalMag) * (180.0D / Math.PI)));
            yRotO = getYRot();
            xRotO = getXRot();
        }
    }

    @Nullable
    public LivingEntity getTarget() {
        if (cachedTarget != null && isTargetValid(cachedTarget)) {
            return cachedTarget;
        }

        if (level().isClientSide) {
            int id = entityData.get(DATA_TARGET_ID);
            if (id != -1 && level().getEntity(id) instanceof LivingEntity living && isTargetValid(living)) {
                cachedTarget = living;
                return cachedTarget;
            }
        } else if (level() instanceof ServerLevel serverLevel && targetUUID != null) {
            Entity entity = serverLevel.getEntity(targetUUID);
            if (entity instanceof LivingEntity living && isTargetValid(living)) {
                cachedTarget = living;
                return cachedTarget;
            }
        }
        return null;
    }

    protected boolean isTargetValid(@Nullable LivingEntity entity) {
        if (entity == null || !entity.isAlive() || entity.isRemoved() || entity.isSpectator()) {
            return false;
        }
        return entity.level().dimension().equals(this.level().dimension());
    }

    @Nullable
    protected LivingEntity getLivingTarget(Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            return livingEntity;
        }
        if (entity instanceof PartEntity<?> part && part.getParent() instanceof LivingEntity livingEntity) {
            return livingEntity;
        }
        return null;
    }

    @Override
    public Optional<java.util.function.Supplier<net.minecraft.sounds.SoundEvent>> getImpactSound() {
        return Optional.of(() -> net.minecraft.sounds.SoundEvents.ARROW_HIT);
    }

    @Override
    public void impactParticles(double x, double y, double z) {
        io.redspace.ironsspellbooks.capabilities.magic.MagicManager.spawnParticles(level(),
                ParticleTypes.CLOUD, x, y, z, 15, 0.2D, 0.2D, 0.2D, 0.1D, true);
        io.redspace.ironsspellbooks.capabilities.magic.MagicManager.spawnParticles(level(),
                ParticleTypes.POOF, x, y, z, 10, 0.15D, 0.15D, 0.15D, 0.05D, true);
    }

    @Override
    public void trailParticles() {
    }

    @Override
    public void tick() {
        super.tick();
        ticksAlive++;
        if (!level().isClientSide && ticksAlive >= MAX_LIFETIME_TICKS) {
            resolveMiss(position());
            return;
        }

        handleFlightAndCollision();
    }

    protected abstract void handleFlightAndCollision();

    @Override
    public void travel() {
        // Controlled directly in handleFlightAndCollision
    }

    protected void resolveMiss(Vec3 pos) {
        if (resolved) {
            return;
        }
        resolved = true;
        setPos(pos);
        discard();
    }

    @Nullable
    protected HitResult findEntityAlongSegment(Vec3 start, Vec3 end, boolean targetOnly) {
        AABB scanBox = new AABB(start, end).inflate(1.0D);
        LivingEntity target = getTarget();

        List<Entity> candidates = level().getEntities(this, scanBox, entity -> {
            if (entity == this || entity == getOwner() || entity.isSpectator() || !entity.isAlive()) {
                return false;
            }
            if (targetOnly) {
                if (target == null) return false;
                return entity == target || (entity instanceof PartEntity<?> part && part.getParent() == target);
            }
            if (getOwner() instanceof LivingEntity owner && DamageSources.isFriendlyFireBetween(owner, entity)) {
                return false;
            }
            return true;
        });

        EntityHitResult closestHit = null;
        double closestDistSqr = Double.MAX_VALUE;

        for (Entity candidate : candidates) {
            AABB bb = candidate.getBoundingBox().inflate(0.3D);
            Optional<Vec3> clip = bb.clip(start, end);
            if (clip.isPresent()) {
                double distSqr = start.distanceToSqr(clip.get());
                if (distSqr < closestDistSqr) {
                    closestDistSqr = distSqr;
                    closestHit = new EntityHitResult(candidate, clip.get());
                }
            }
        }
        return closestHit;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 64.0D * 64.0D;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (targetUUID != null) {
            tag.putUUID("TargetUUID", targetUUID);
        }
        tag.putFloat("DamageAmount", damageAmount);
        tag.putInt("SpellLevel", spellLevel);
        tag.putInt("TicksAlive", ticksAlive);
        tag.putBoolean("Resolved", resolved);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("TargetUUID")) {
            targetUUID = tag.getUUID("TargetUUID");
        }
        damageAmount = tag.getFloat("DamageAmount");
        spellLevel = tag.getInt("SpellLevel");
        ticksAlive = tag.getInt("TicksAlive");
        resolved = tag.getBoolean("Resolved");
    }
}
