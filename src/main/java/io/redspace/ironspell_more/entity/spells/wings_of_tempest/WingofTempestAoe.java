package io.redspace.ironspell_more.entity.spells.wings_of_tempest;

import com.github.L_Ender.cataclysm.client.particle.StormParticle;
import io.redspace.ironspell_more.registry.EntityRegistry;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import io.redspace.ironsspellbooks.entity.spells.AoeEntity;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class WingofTempestAoe extends AoeEntity implements AntiMagicSusceptible {

    public WingofTempestAoe(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setCircular();
        this.setRadius(13.0f);
        this.setDuration(600); // 30 seconds
        this.reapplicationDelay = 20; // refresh debuff every 1 second
    }

    public WingofTempestAoe(Level level) {
        this(EntityRegistry.WING_OF_TEMPEST_AOE.get(), level);
    }

    @Override
    public void tick() {
        Entity owner = getOwner();
        if (owner != null && owner.isAlive()) {
            // Keep the storm centered on the caster as they move
            this.setPos(owner.getX(), owner.getY(), owner.getZ());
        } else if (!this.level().isClientSide && tickCount > 5) {
            // If owner is gone or dead, dissipate storm
            this.discard();
            return;
        }

        super.tick();

        // Swirling / orbiting wind effect on nearby entities
        if (!this.level().isClientSide) {
            double radius = getRadius();
            double radiusSqr = radius * radius;
            AABB searchBox = this.getBoundingBox().inflate(radius, 4.0, radius);
            List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, searchBox);

            for (LivingEntity target : targets) {
                if (canHitEntity(target)) {
                    double dx = target.getX() - getX();
                    double dz = target.getZ() - getZ();
                    double distSq = dx * dx + dz * dz;

                    if (distSq > 0.36 && distSq <= radiusSqr) {
                        double dist = Math.sqrt(distSq);
                        // Tangential direction vector (-dz, dx) for smooth counter-clockwise swirl
                        double tangentX = -dz / dist;
                        double tangentZ = dx / dist;

                        Vec3 motion = target.getDeltaMovement();
                        double targetSpeed = 0.35; // gentle tangential orbit speed
                        double lerp = 0.25;

                        double newX = Mth.lerp(lerp, motion.x, tangentX * targetSpeed);
                        double newZ = Mth.lerp(lerp, motion.z, tangentZ * targetSpeed);

                        target.setDeltaMovement(newX, motion.y, newZ);
                        target.hurtMarked = true; // Sync velocity update to client
                    }
                }
            }

            // Play ambient swirling wind sound periodically
            if (this.tickCount % 40 == 0) {
                this.level().playSound(null, getX(), getY(), getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.6f, 1.2f);
            }
        }
    }

    @Override
    public void applyEffect(LivingEntity target) {
        // Nausea 2 for 6 seconds (amplifier 1)
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 1, false, true, true));

        // Blight 5 for 20 seconds (amplifier 4)
        target.addEffect(new MobEffectInstance(MobEffectRegistry.BLIGHT.get(), 400, 4, false, true, true));

        // Mining Fatigue 5 for 20 seconds (amplifier 4)
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 400, 4, false, true, true));

        // Slowness 3 for 20 seconds (amplifier 2)
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 2, false, true, true));
    }

    @Override
    protected boolean canHitEntity(Entity pTarget) {
        return super.canHitEntity(pTarget) && !DamageSources.isFriendlyFireBetween(this.getOwner(), pTarget);
    }

    @Override
    protected boolean canHitTargetForGroundContext(LivingEntity target) {
        // Allow hitting jumping or airborne targets within vertical bounds
        return Math.abs(target.getY() - getY()) < 6.0;
    }

    @Override
    public void ambientParticles() {
        if (!this.level().isClientSide) {
            return;
        }

        float radius = getRadius();
        int particleSpawns = 6;

        Entity owner = getOwner();
        int orbitEntityId = owner != null ? owner.getId() : this.getId();
        if (this.tickCount % 3 == 0) {
            float orbRadius = 2.0f + this.random.nextFloat() * (radius - 2.0f);
            float orbHeight = 0.2f + this.random.nextFloat() * 2.5f;
            this.level().addParticle(new StormParticle.OrbData(1.0f, 1.0f, 1.0f, orbRadius, orbHeight, orbitEntityId),
                    getX(), getY(), getZ(), 0, 0, 0);
        }

        for (int i = 0; i < particleSpawns; i++) {
            float r = 1.0f + this.random.nextFloat() * (radius - 1.0f);
            float angle = this.random.nextFloat() * 6.2831855f;
            double px = getX() + r * Mth.cos(angle);
            double py = getY() + 0.2 + this.random.nextDouble() * 2.2;
            double pz = getZ() + r * Mth.sin(angle);

            // Tangential velocity matching the swirl
            double speed = 0.18 + this.random.nextDouble() * 0.12;
            double vx = -Mth.sin(angle) * speed;
            double vy = (this.random.nextDouble() - 0.5) * 0.04;
            double vz = Mth.cos(angle) * speed;

            if (this.random.nextFloat() < 0.7f) {
                this.level().addParticle(ParticleTypes.CLOUD, px, py, pz, vx, vy, vz);
            } else {
                this.level().addParticle(ParticleTypes.POOF, px, py, pz, vx * 0.5, vy, vz * 0.5);
            }
        }
    }

    @Override
    public float getParticleCount() {
        return 1.0f;
    }

    @Override
    public Optional<ParticleOptions> getParticle() {
        return Optional.empty();
    }

    @Override
    public void onAntiMagic(MagicData magicData) {
        this.discard();
    }
}
