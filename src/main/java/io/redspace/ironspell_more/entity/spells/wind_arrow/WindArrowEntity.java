package io.redspace.ironspell_more.entity.spells.wind_arrow;

import io.redspace.ironspell_more.registry.EntityRegistry;
import io.redspace.ironspell_more.registry.SpellRegistry;
import io.redspace.ironspell_more.spells.nature.WindArrowSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraftforge.entity.PartEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Supplier;

public class WindArrowEntity extends AbstractMagicProjectile {
    private static final EntityDataAccessor<Boolean> DATA_FULL_CHARGE =
            SynchedEntityData.defineId(WindArrowEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_PROJECTILE_SPEED =
            SynchedEntityData.defineId(WindArrowEntity.class, EntityDataSerializers.FLOAT);

    private static final int MAX_FLIGHT_TICKS = 160;
    private static final double MAX_HOMING_DISTANCE_SQR = 64.0D * 64.0D;

    private boolean resolved;
    private int flightTicks;

    public WindArrowEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public WindArrowEntity(Level level, LivingEntity owner) {
        this(EntityRegistry.WIND_ARROW.get(), level);
        setOwner(owner);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_FULL_CHARGE, false);
        entityData.define(DATA_PROJECTILE_SPEED, WindArrowSpell.NORMAL_SPEED);
    }

    public void configure(Vec3 direction, boolean fullCharge, LivingEntity target, float damage) {
        entityData.set(DATA_FULL_CHARGE, fullCharge);
        float speed = fullCharge ? WindArrowSpell.FULL_CHARGE_SPEED : WindArrowSpell.NORMAL_SPEED;
        entityData.set(DATA_PROJECTILE_SPEED, speed);
        setDamage(damage);

        Vec3 normalized = direction.lengthSqr() < 1.0E-8D ? new Vec3(0, 0, 1) : direction.normalize();
        setDeltaMovement(normalized.scale(speed));
        shoot(normalized);

        if (target != null && target.isAlive()) {
            setHomingTarget(target);
        }
    }

    public boolean isFullCharge() {
        return entityData.get(DATA_FULL_CHARGE);
    }

    @Override
    public float getSpeed() {
        return entityData.get(DATA_PROJECTILE_SPEED);
    }

    @Override
    public Optional<Supplier<SoundEvent>> getImpactSound() {
        return Optional.of(() -> isFullCharge() ? SoundEvents.FIRECHARGE_USE : SoundEvents.ARROW_HIT);
    }

    @Override
    public void tick() {
        super.tick();
        flightTicks++;
        if (!level().isClientSide && flightTicks > MAX_FLIGHT_TICKS) {
            discard();
        }
    }

    @Override
    public float getHitDetectionInflation() {
        return isFullCharge() ? 1.25F : 0.4F;
    }

    @Override
    protected void handleEntityHoming() {
        if (cachedHomingTarget == null && homingTargetUUID != null && level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(homingTargetUUID);
            if (entity instanceof LivingEntity living) {
                cachedHomingTarget = living;
            }
        }

        if (cachedHomingTarget != null) {
            if (!cachedHomingTarget.isAlive() || cachedHomingTarget.isRemoved()) {
                stopEntityHoming();
                return;
            }

            if (distanceToSqr(cachedHomingTarget) > MAX_HOMING_DISTANCE_SQR) {
                stopEntityHoming();
                return;
            }

            // Cannot track through solid walls/blocks
            Vec3 currentPos = position();
            Vec3 targetEye = cachedHomingTarget.getEyePosition();
            BlockHitResult blockCheck = level().clip(new ClipContext(
                    currentPos,
                    targetEye,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    this
            ));

            if (blockCheck.getType() != HitResult.Type.MISS) {
                // Obstructed by block, stop homing and fly straight
                stopEntityHoming();
                return;
            }

            boolean full = isFullCharge();
            Vec3 currentMotion = getDeltaMovement();
            Vec3 toTarget = targetEye.subtract(currentPos);
            double distSqr = toTarget.lengthSqr();

            if (distSqr < 1.0E-4D) {
                return;
            }

            if (!full) {
                // Normal Mode:
                // Tracks immediately, but if the target moves or maneuvers fast,
                // the arrow's turn rate cannot keep up. Once it passes the target (dot <= 0),
                // it stops homing and flies straight to crash into the ground or wall behind.
                if (currentMotion.dot(toTarget) <= 0.0D) {
                    stopEntityHoming();
                    return;
                }

                // Smooth aerodynamic turning (~95% accuracy on static/slow targets, but dodgeable by fast targets)
                float turnRate = 0.13F;
                Vec3 desiredMotion = toTarget.normalize().scale(getSpeed());
                Vec3 newMotion = homeTowards(desiredMotion, turnRate);
                setDeltaMovement(newMotion);
            } else {
                // Full Charge (ศรวายุเพลิง):
                // 100% Guaranteed Hit!
                // Predictive lead tracking + aggressive steering + close range target snapping
                double dist = Math.sqrt(distSqr);
                double speed = getSpeed();
                double timeToTarget = dist / Math.max(speed, 0.1D);

                Vec3 targetVel = cachedHomingTarget.getDeltaMovement();
                Vec3 aimPoint = targetEye.add(targetVel.scale(Math.min(timeToTarget, 4.0D)));
                Vec3 desiredDirection = aimPoint.subtract(currentPos).normalize();

                if (dist <= 4.0D) {
                    // Close range: snap vector directly into target's eye/center to guarantee hit
                    setDeltaMovement(toTarget.normalize().scale(speed));
                } else {
                    // Mid/Long range: aggressive predictive tracking
                    Vec3 desiredMotion = desiredDirection.scale(speed);
                    Vec3 newMotion = homeTowards(desiredMotion, 0.85F);
                    setDeltaMovement(newMotion);
                }
            }
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        if (resolved || isRemoved()) {
            return;
        }

        Entity hitEntity = result.getEntity();
        LivingEntity livingTarget = getLivingTarget(hitEntity);

        if (getOwner() instanceof LivingEntity owner && livingTarget != null) {
            if (DamageSources.isFriendlyFireBetween(owner, livingTarget)) {
                return;
            }
        }

        boolean full = isFullCharge();
        var spell = SpellRegistry.WIND_ARROW_SPELL.get();
        DamageSources.applyDamage(hitEntity, getDamage(), spell.getDamageSource(this, getOwner()));

        Vec3 hitDirection = getDeltaMovement().normalize();
        if (hitDirection.lengthSqr() < 1.0E-6D) {
            hitDirection = getLookAngle().normalize();
        }

        if (livingTarget != null) {
            float knockbackStrength = full ? WindArrowSpell.FULL_CHARGE_KNOCKBACK : WindArrowSpell.NORMAL_KNOCKBACK;
            livingTarget.push(hitDirection.x * knockbackStrength, 0.15D * knockbackStrength, hitDirection.z * knockbackStrength);
            livingTarget.hurtMarked = true;

            if (full) {
                livingTarget.setSecondsOnFire(WindArrowSpell.FULL_CHARGE_FIRE_SECONDS);
                livingTarget.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN,
                        WindArrowSpell.FULL_CHARGE_SLOWNESS_TICKS,
                        0,
                        false,
                        true,
                        true
                ));
            }
        }

        resolved = true;
        impactParticles(getX(), getY(), getZ());
        discard();
    }

    private LivingEntity getLivingTarget(Entity entity) {
        if (entity instanceof LivingEntity living) {
            return living;
        }
        if (entity instanceof PartEntity<?> part && part.getParent() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult result) {
        super.onHitBlock(result);
        if (resolved || isRemoved()) {
            return;
        }
        resolved = true;
        setPos(result.getLocation());
        impactParticles(getX(), getY(), getZ());
        discard();
    }

    @Override
    public void trailParticles() {
        Vec3 motion = getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-6D) {
            return;
        }

        boolean full = isFullCharge();
        Vec3 pos = position();

        if (full) {
            // Gale Fire Arrow trail: Spiraling vortex of wind + fire embers
            Vec3 dir = motion.normalize();
            Vec3 up = Math.abs(dir.y) > 0.95D ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            Vec3 right = dir.cross(up).normalize();
            up = right.cross(dir).normalize();

            double spiralRadius = 0.35D;
            double angle = (tickCount * 0.75D);

            // Double spiral (opposing arms)
            for (int i = 0; i < 2; i++) {
                double a = angle + i * Math.PI;
                Vec3 offset = right.scale(Math.cos(a) * spiralRadius).add(up.scale(Math.sin(a) * spiralRadius));
                Vec3 pPos = pos.add(offset);

                level().addParticle(ParticleTypes.SMALL_FLAME, pPos.x, pPos.y, pPos.z,
                        -dir.x * 0.05D, -dir.y * 0.05D, -dir.z * 0.05D);
            }

            // Core fiery wind particles
            level().addParticle(ParticleTypes.FLAME, pos.x, pos.y, pos.z,
                    -dir.x * 0.1D, -dir.y * 0.1D, -dir.z * 0.1D);
            level().addParticle(ParticleTypes.CLOUD, pos.x, pos.y, pos.z,
                    0, 0, 0);
        } else {
            // Normal Wind Arrow trail: light breeze / subtle puff
            level().addParticle(ParticleTypes.POOF, pos.x, pos.y, pos.z,
                    0, 0, 0);
            if (tickCount % 2 == 0) {
                level().addParticle(ParticleTypes.CLOUD, pos.x, pos.y, pos.z,
                        -motion.x * 0.05D, -motion.y * 0.05D, -motion.z * 0.05D);
            }
        }
    }

    @Override
    public void impactParticles(double x, double y, double z) {
        boolean full = isFullCharge();
        if (full) {
            // Fiery wind burst
            for (int i = 0; i < 16; i++) {
                double speed = 0.15D + random.nextDouble() * 0.25D;
                double theta = random.nextDouble() * Math.PI * 2.0D;
                double phi = random.nextDouble() * Math.PI - Math.PI / 2.0D;
                double vx = Math.cos(theta) * Math.cos(phi) * speed;
                double vy = Math.sin(phi) * speed;
                double vz = Math.sin(theta) * Math.cos(phi) * speed;

                level().addParticle(ParticleTypes.FLAME, x, y, z, vx, vy, vz);
                if (i % 2 == 0) {
                    level().addParticle(ParticleTypes.LAVA, x, y, z, vx * 0.5, vy * 0.5, vz * 0.5);
                }
            }
            level().addParticle(ParticleTypes.EXPLOSION, x, y, z, 0, 0, 0);
        } else {
            // Gentle wind poof
            for (int i = 0; i < 8; i++) {
                double vx = (random.nextDouble() - 0.5D) * 0.2D;
                double vy = (random.nextDouble() - 0.5D) * 0.2D;
                double vz = (random.nextDouble() - 0.5D) * 0.2D;
                level().addParticle(ParticleTypes.POOF, x, y, z, vx, vy, vz);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("FullCharge", isFullCharge());
        tag.putFloat("Speed", getSpeed());
        tag.putInt("FlightTicks", flightTicks);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_FULL_CHARGE, tag.getBoolean("FullCharge"));
        entityData.set(DATA_PROJECTILE_SPEED, tag.getFloat("Speed"));
        flightTicks = tag.getInt("FlightTicks");
    }
}
