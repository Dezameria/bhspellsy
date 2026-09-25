package io.redspace.ironspell_more.entity.spells.gale_piercer;

import io.redspace.ironspell_more.registry.EntityRegistry;
import io.redspace.ironspell_more.registry.SpellRegistry;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public class GaleArrowEntity extends AbstractGalePiercerArrowEntity {
    public static final double SPEED = 6.0D;
    public static final float KNOCKBACK_STRENGTH = 0.8F;
    public static final int FIRE_SECONDS = 4;
    public static final int SLOWNESS_TICKS = 100; // 5 seconds

    private static final DustParticleOptions WHITE_WIND_DUST =
            new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 0.9F);
    private static final DustParticleOptions PALE_WIND_DUST =
            new DustParticleOptions(new Vector3f(0.92F, 0.94F, 1.0F), 0.9F);
    private static final DustParticleOptions ORANGE_SPARK_DUST =
            new DustParticleOptions(new Vector3f(1.0F, 0.34F, 0.06F), 1.0F);
    private static final DustParticleOptions CRIMSON_SPARK_DUST =
            new DustParticleOptions(new Vector3f(1.0F, 0.10F, 0.03F), 0.85F);

    public GaleArrowEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    public GaleArrowEntity(Level level, LivingEntity owner) {
        super(EntityRegistry.GALE_ARROW.get(), level, owner);
    }

    @Override
    public float getSpeed() {
        return (float) SPEED;
    }

    @Override
    public java.util.Optional<java.util.function.Supplier<net.minecraft.sounds.SoundEvent>> getImpactSound() {
        return java.util.Optional.of(() -> net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE);
    }

    @Override
    public void impactParticles(double x, double y, double z) {
        io.redspace.ironsspellbooks.capabilities.magic.MagicManager.spawnParticles(level(),
                ParticleTypes.FLAME, x, y, z, 40, 0.4D, 0.4D, 0.4D, 0.18D, true);
        io.redspace.ironsspellbooks.capabilities.magic.MagicManager.spawnParticles(level(),
                ParticleTypes.LAVA, x, y, z, 10, 0.25D, 0.25D, 0.25D, 0.12D, true);
        io.redspace.ironsspellbooks.capabilities.magic.MagicManager.spawnParticles(level(),
                WHITE_WIND_DUST, x, y, z, 25, 0.35D, 0.35D, 0.35D, 0.12D, true);
        io.redspace.ironsspellbooks.capabilities.magic.MagicManager.spawnParticles(level(),
                ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0, 0, 0, 0, true);
    }

    @Override
    protected void handleFlightAndCollision() {
        Vec3 currentPos = position();
        Vec3 motion = getDeltaMovement();
        double currentSpeed = motion.length();
        if (currentSpeed < 1.0E-4D) {
            currentSpeed = SPEED;
            motion = Vec3.directionFromRotation(getXRot(), getYRot()).scale(currentSpeed);
        }

        LivingEntity target = getTarget();
        if (target != null) {
            Vec3 targetCenter = target.getBoundingBox().getCenter();
            Vec3 toTarget = targetCenter.subtract(currentPos);
            if (toTarget.lengthSqr() > 1.0E-4D) {
                // 100% accurate persistent homing
                Vec3 desiredDir = toTarget.normalize();
                motion = desiredDir.scale(SPEED);
                setDeltaMovement(motion);
                updateRotationFromMotion(motion);
            }
        }

        Vec3 nextPos = currentPos.add(motion);

        // Spawn client-side vortex, trails, and orange-red fiery sparks
        if (level().isClientSide) {
            spawnGaleVortexParticles(currentPos, nextPos);
            setPos(nextPos);
            updateRotationFromMotion(motion);
            return;
        }

        // Server-side: Pierces through all blocks/walls! No block collision query.
        // Swept collision against entities
        HitResult entityHit = findEntityAlongSegment(currentPos, nextPos, target != null);
        if (entityHit instanceof EntityHitResult eHit) {
            if (!MinecraftForge.EVENT_BUS.post(new ProjectileImpactEvent(this, eHit))) {
                setPos(eHit.getLocation());
                onHitEntity(eHit);
                return;
            }
        }

        // Check if inside target bounding box
        if (target != null && target.getBoundingBox().inflate(0.5D).contains(nextPos)) {
            EntityHitResult directHit = new EntityHitResult(target, target.getBoundingBox().getCenter());
            if (!MinecraftForge.EVENT_BUS.post(new ProjectileImpactEvent(this, directHit))) {
                setPos(directHit.getLocation());
                onHitEntity(directHit);
                return;
            }
        }

        // Move to next position (phasing through any blocks)
        setPos(nextPos);
    }

    private void spawnGaleVortexParticles(Vec3 start, Vec3 end) {
        double distance = start.distanceTo(end);
        if (distance < 1.0E-4D) {
            return;
        }

        Vec3 dir = end.subtract(start).scale(1.0D / distance);
        Vec3 referenceAxis = Math.abs(dir.y) > 0.9D ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = dir.cross(referenceAxis).normalize();
        Vec3 up = right.cross(dir).normalize();
        int axialSteps = Math.min(8, Math.max(4, (int) Math.ceil(distance * 1.1D)));

        for (int step = 0; step < axialSteps; step++) {
            double progress = (step + 0.5D) / axialSteps;
            Vec3 center = start.lerp(end, progress);
            double radius = 0.60D - progress * 0.30D;

            for (int arm = 0; arm < 3; arm++) {
                double angle = tickCount * 0.75D
                        + progress * Math.PI * 4.5D
                        + arm * Math.PI * 2.0D / 3.0D;
                Vec3 offset = right.scale(Math.cos(angle) * radius)
                        .add(up.scale(Math.sin(angle) * radius));
                Vec3 particlePos = center.add(offset);
                Vec3 spiralVelocity = right.scale(-Math.sin(angle))
                        .add(up.scale(Math.cos(angle)))
                        .scale(0.07D)
                        .subtract(dir.scale(0.04D));

                level().addParticle(arm == 1 ? WHITE_WIND_DUST : PALE_WIND_DUST,
                        particlePos.x, particlePos.y, particlePos.z,
                        spiralVelocity.x, spiralVelocity.y, spiralVelocity.z);

                if (arm < 2) {
                    level().addParticle(ParticleTypes.SMALL_FLAME,
                            particlePos.x, particlePos.y, particlePos.z,
                            spiralVelocity.x, spiralVelocity.y, spiralVelocity.z);
                }

                // Three hot accents are distributed from the broad wake toward the arrowhead.
                boolean accent = (arm == 0 && step == 1)
                        || (arm == 1 && step == axialSteps / 2)
                        || (arm == 2 && step == axialSteps - 2);
                if (accent) {
                    DustParticleOptions spark = arm == 0 ? ORANGE_SPARK_DUST : CRIMSON_SPARK_DUST;
                    level().addParticle(spark, particlePos.x, particlePos.y, particlePos.z,
                            spiralVelocity.x, spiralVelocity.y, spiralVelocity.z);
                }
            }
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        if (resolved || isRemoved()) {
            return;
        }
        super.onHitEntity(result);

        LivingEntity livingTarget = getLivingTarget(result.getEntity());
        if (livingTarget != null) {
            var spell = SpellRegistry.GALE_PIERCER_SPELL.get();
            var damageSource = spell.getDamageSource(this, getOwner());
            DamageSources.applyDamage(livingTarget, damageAmount, damageSource);

            // Strong wind knockback + lift
            Vec3 knockbackDir = getDeltaMovement().normalize();
            livingTarget.knockback(KNOCKBACK_STRENGTH, -knockbackDir.x, -knockbackDir.z);
            livingTarget.push(0.0D, 0.25D, 0.0D);

            // Ignite target for short duration (Fire DoT)
            livingTarget.setSecondsOnFire(FIRE_SECONDS);

            // Inflict Slowness I temporarily (5 seconds)
            livingTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOWNESS_TICKS, 0, false, true, true));

            // Impact sound & particle blast
            level().playSound(null, getX(), getY(), getZ(),
                    net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 1.4F);
        }

        resolved = true;
        discard();
    }
}
