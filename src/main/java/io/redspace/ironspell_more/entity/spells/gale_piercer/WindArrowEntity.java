package io.redspace.ironspell_more.entity.spells.gale_piercer;

import io.redspace.ironspell_more.registry.EntityRegistry;
import io.redspace.ironspell_more.registry.SpellRegistry;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
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
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public class WindArrowEntity extends AbstractGalePiercerArrowEntity {
    public static final double SPEED = 3.2D;
    public static final double MAX_TURN_RATE = 0.08D; // radians per tick (~4.58 deg/tick)
    public static final float KNOCKBACK_STRENGTH = 0.4F;

    private static final DustParticleOptions PALE_WIND_DUST =
            new DustParticleOptions(new Vector3f(0.92F, 0.94F, 1.0F), 0.75F);

    public WindArrowEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    public WindArrowEntity(Level level, LivingEntity owner) {
        super(EntityRegistry.WIND_ARROW.get(), level, owner);
    }

    @Override
    public float getSpeed() {
        return (float) SPEED;
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
        if (target != null && !level().isClientSide) {
            Vec3 targetCenter = target.getBoundingBox().getCenter();
            Vec3 toTarget = targetCenter.subtract(currentPos);
            if (toTarget.lengthSqr() > 1.0E-4D) {
                Vec3 desiredDir = toTarget.normalize();
                Vec3 currentDir = motion.normalize();

                // Compute angle between vectors
                double dot = Mth.clamp(currentDir.dot(desiredDir), -1.0D, 1.0D);
                double angle = Math.acos(dot);

                if (angle > 1.0E-4D) {
                    double turnStep = Math.min(angle, MAX_TURN_RATE);
                    // Rotation axis: cross product
                    Vec3 axis = currentDir.cross(desiredDir);
                    if (axis.lengthSqr() > 1.0E-6D) {
                        axis = axis.normalize();
                        // Rodriques rotation formula or SLERP
                        Vec3 newDir = rotateAroundAxis(currentDir, axis, turnStep).normalize();
                        motion = newDir.scale(currentSpeed);
                        setDeltaMovement(motion);
                        updateRotationFromMotion(motion);
                    }
                }
            }
        }

        Vec3 nextPos = currentPos.add(motion);

        // Spawn client wind trail particles
        if (level().isClientSide) {
            spawnWindTrailParticles(currentPos, nextPos);
            setPos(nextPos);
            updateRotationFromMotion(motion);
            return;
        }

        // Server-side continuous swept collision
        // 1. Block hit check first (cannot penetrate blocks)
        BlockHitResult blockHit = level().clip(new ClipContext(currentPos, nextPos,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

        Vec3 sweepEnd = blockHit.getType() == HitResult.Type.MISS ? nextPos : blockHit.getLocation();

        // 2. Entity sweep up to block hit
        HitResult entityHit = findEntityAlongSegment(currentPos, sweepEnd, false);
        if (entityHit instanceof EntityHitResult eHit) {
            if (!MinecraftForge.EVENT_BUS.post(new ProjectileImpactEvent(this, eHit))) {
                setPos(eHit.getLocation());
                onHitEntity(eHit);
                return;
            }
        }

        // 3. If block was hit and no entity hit intervened, stop and destroy immediately
        if (blockHit.getType() != HitResult.Type.MISS) {
            setPos(blockHit.getLocation());
            onHitBlock(blockHit);
            return;
        }

        // Move to next position
        setPos(nextPos);
    }

    private Vec3 rotateAroundAxis(Vec3 v, Vec3 axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return v.scale(cos)
                .add(axis.cross(v).scale(sin))
                .add(axis.scale(axis.dot(v) * (1.0D - cos)));
    }

    private void spawnWindTrailParticles(Vec3 start, Vec3 end) {
        double distance = start.distanceTo(end);
        if (distance < 1.0E-4D) {
            return;
        }

        int steps = Math.min(4, Math.max(1, (int) Math.ceil(distance * 1.25D)));
        for (int i = 0; i < steps; i++) {
            double progress = (i + 0.5D) / steps;
            Vec3 pos = start.lerp(end, progress);
            level().addParticle(PALE_WIND_DUST,
                    pos.x + (random.nextDouble() - 0.5D) * 0.035D,
                    pos.y + (random.nextDouble() - 0.5D) * 0.035D,
                    pos.z + (random.nextDouble() - 0.5D) * 0.035D,
                    0.0D, 0.0D, 0.0D);
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

            // Apply slight wind knockback
            Vec3 knockbackDir = getDeltaMovement().normalize();
            livingTarget.knockback(KNOCKBACK_STRENGTH, -knockbackDir.x, -knockbackDir.z);
        }

        resolved = true;
        discard();
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult result) {
        if (resolved || isRemoved()) {
            return;
        }
        super.onHitBlock(result);
        // Destroyed immediately on block collision
        resolveMiss(result.getLocation());
    }
}
