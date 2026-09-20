package io.redspace.ironspell_more.compat.epicfight.skills.blazing_chakra;

import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.particle.ShockwaveParticleOptions;
import io.redspace.ironsspellbooks.registries.ParticleRegistry;
import io.redspace.ironspell_more.compat.epicfight.common.particle.GroundShockwaveVfx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * Dedicated VFX and combat impact logic for the Blazing Chakra lance attack.
 * Uses Iron's Spells fiery particles (deep red & vibrant orange) with zero blue soul fire,
 * generating a water-ripple concentric flame wave across 10-12 blocks with proximity damage falloff.
 */
public final class BlazingChakraVfx {
    public static final float MAX_RANGE = 12.0F;
    public static final float CLOSE_RANGE = 3.0F;
    public static final float MAX_DAMAGE = 24.0F;
    public static final float MIN_DAMAGE = 6.0F;

    private BlazingChakraVfx() {
    }

    /**
     * Client VFX: Concentric water-ripple flame wave expanding across 10-12 blocks,
     * weapon fire swirl, chest flame arc, and ground impact scatter.
     */
    public static void spawnImpactClientVfx(LivingEntityPatch<?> patch) {
        if (patch == null || patch.getOriginal() == null) {
            return;
        }
        LivingEntity entity = patch.getOriginal();
        Level level = entity.level();
        if (!level.isClientSide()) {
            return;
        }

        Vec3 origin = entity.position().add(0, 0.05D, 0);

        // 1. Primary & Secondary Iron's Spells Ground Shockwave Rings (Water-Ripple effect)
        // Deep fiery red-orange color: R=1.0, G=0.22, B=0.04
        Vector3f darkRedOrange = new Vector3f(1.0F, 0.22F, 0.04F);
        level.addParticle(
            new ShockwaveParticleOptions(darkRedOrange, MAX_RANGE, true),
            origin.x, origin.y + 0.05D, origin.z,
            0.0D, 0.0D, 0.0D
        );

        // Inner glowing orange ripple (scale 6.5 blocks)
        Vector3f brightOrange = new Vector3f(1.0F, 0.45F, 0.06F);
        level.addParticle(
            new ShockwaveParticleOptions(brightOrange, 6.5F, true),
            origin.x, origin.y + 0.08D, origin.z,
            0.0D, 0.0D, 0.0D
        );

        // 2. Concentric Fire Waves (6 Expanding Rings reaching 11.5 blocks)
        int rings = 6;
        int pointsPerRing = 48;
        float maxRadius = 11.5F;

        for (int r = 0; r < rings; r++) {
            float radius = maxRadius * (r + 1.0F) / (float) rings;
            for (int p = 0; p < pointsPerRing; p++) {
                double angle = (2.0D * Math.PI * p) / (double) pointsPerRing;
                float x = (float) (radius * Math.cos(angle));
                float z = (float) (radius * Math.sin(angle));

                // Iron's Spells Fire Particle
                level.addParticle(
                    ParticleRegistry.FIRE_PARTICLE.get(),
                    origin.x + x, origin.y + 0.05D, origin.z + z,
                    x * 0.08D, 0.05D, z * 0.08D
                );

                // Fiery Embers
                if (p % 2 == 0) {
                    level.addParticle(
                        ParticleRegistry.EMBER_PARTICLE.get(),
                        origin.x + x, origin.y + 0.1D, origin.z + z,
                        (Math.random() - 0.5D) * 0.1D,
                        0.15D + Math.random() * 0.1D,
                        (Math.random() - 0.5D) * 0.1D
                    );
                }

                // Fiery Smoke on outer rings
                if (p % 4 == 0) {
                    level.addParticle(
                        ParticleRegistry.FIERY_SMOKE_PARTICLE.get(),
                        origin.x + x, origin.y + 0.08D, origin.z + z,
                        x * 0.04D, 0.08D, z * 0.04D
                    );
                }
            }

            // Lava splash at outer wave crests
            if (r >= rings - 2) {
                for (int p = 0; p < pointsPerRing / 2; p++) {
                    double angle = (2.0D * Math.PI * p) / (double) (pointsPerRing / 2);
                    float x = (float) (radius * Math.cos(angle));
                    float z = (float) (radius * Math.sin(angle));
                    level.addParticle(
                        ParticleTypes.LAVA,
                        origin.x + x, origin.y + 0.1D, origin.z + z,
                        x * 0.15D, 0.18D, z * 0.15D
                    );
                }
            }
        }

        // 3. Spear Joint Fiery Swirl around ToolR (Replaced soulfire with Iron's Spells fire & embers)
        try {
            Armature armature = patch.getArmature();
            if (armature != null) {
                Pose pose = patch.getAnimator().getPose(0.0F);
                HumanoidArmature humanoidArmature = (HumanoidArmature) Armatures.BIPED.get();
                Joint toolR = humanoidArmature.toolR;
                OpenMatrix4f toolRTransform = armature.getBoundTransformFor(pose, toolR);

                toolRTransform.translate(new Vec3f(-0.2F, 0.0F, 0.4F));
                OpenMatrix4f rot = new OpenMatrix4f();
                rot.rotate((float) -Math.toRadians(entity.getYRot() + 180.0F), new Vec3f(0.0F, 1.0F, 0.0F));
                OpenMatrix4f.mul(toolRTransform, rot, toolRTransform);

                for (int i = 0; i < 75; i++) {
                    double phi = Math.random() * 2.0D * Math.PI;
                    double theta = Math.acos(Math.random() * 2.0D - 1.0D);
                    float vx = (float) (0.12D * Math.sin(theta) * Math.cos(phi));
                    float vy = (float) (0.12D * Math.sin(theta) * Math.sin(phi));
                    float vz = (float) (0.12D * Math.cos(theta));

                    double px = toolRTransform.m30 + entity.getX();
                    double py = toolRTransform.m31 + entity.getY() + Math.random() * 2.9D;
                    double pz = toolRTransform.m32 + entity.getZ();

                    level.addParticle(
                        (i % 2 == 0) ? ParticleRegistry.FIRE_PARTICLE.get() : ParticleRegistry.EMBER_PARTICLE.get(),
                        px, py, pz,
                        vx, vy, vz
                    );

                    if (i % 3 == 0) {
                        level.addParticle(ParticleRegistry.FIERY_SMOKE_PARTICLE.get(), px, py, pz, vx * 0.5, vy * 0.5, vz * 0.5);
                    }
                }

                // 4. Forward Fiery Arc Blast from Chest
                Joint chest = humanoidArmature.chest;
                OpenMatrix4f chestTransform = armature.getBoundTransformFor(pose, chest);
                OpenMatrix4f chestRot = new OpenMatrix4f();
                chestRot.rotate((float) -Math.toRadians(entity.getYRot() + 180.0F), new Vec3f(0.0F, 1.0F, 0.0F));
                OpenMatrix4f.mul(chestTransform, chestRot, chestTransform);
                chestTransform.translate(new Vec3f(0.0F, 0.3F, -0.5F));

                double radius = 1.25D;
                double step = 0.006D;
                int arcCount = 350;

                for (int side = 0; side < 2; side++) {
                    float pitchAngle = (side == 0) ? 110.0F : 70.0F;
                    for (int i = 0; i < arcCount; i++) {
                        double phi = Math.random() * 2.0D * Math.PI;
                        double angleVal = (Math.random() - 0.2D) * Math.PI * step / radius;

                        Vec3f v = new Vec3f(
                            (float) (radius * Math.cos(angleVal) * Math.cos(phi)),
                            (float) (radius * Math.cos(angleVal) * Math.sin(phi) * 1.5F),
                            (float) (radius * Math.sin(angleVal))
                        );

                        OpenMatrix4f transform = new OpenMatrix4f();
                        transform.rotate((float) Math.toRadians(-entity.getYRot() + 90.0F), new Vec3f(0.0F, 1.0F, 0.0F));
                        transform.rotate((float) Math.toRadians(pitchAngle), new Vec3f(1.0F, 0.0F, 0.0F));
                        OpenMatrix4f.transform3v(transform, v, v);

                        v.scale(0.3F + 0.3F * (float) Math.random());

                        SimpleParticleType particleType;
                        double rand = Math.random();
                        if (rand < 0.5D) {
                            particleType = ParticleRegistry.FIRE_PARTICLE.get();
                        } else if (rand < 0.8D) {
                            particleType = ParticleRegistry.EMBER_PARTICLE.get();
                        } else {
                            particleType = ParticleRegistry.FIERY_SMOKE_PARTICLE.get();
                        }

                        double px = chestTransform.m30 + entity.getX();
                        double py = chestTransform.m31 + entity.getY() + 0.7D;
                        double pz = chestTransform.m32 + entity.getZ();

                        level.addParticle(
                            particleType,
                            px, py, pz,
                            v.x * 1.3F, v.y * 1.5F, v.z * 1.3F
                        );
                    }
                }
            }
        } catch (Throwable ignored) {
            // Graceful fallback
        }

        // 5. Broad Ground Impact Scatter across 11 blocks
        for (int i = 0; i < 300; i++) {
            double angle = Math.random() * 2.0D * Math.PI;
            double dist = maxRadius * Math.random();
            Vec3 p = entity.position().add(dist * Math.cos(angle), 0.1D, dist * Math.sin(angle));

            SimpleParticleType pType = (Math.random() < 0.5D)
                ? ParticleRegistry.FIRE_PARTICLE.get()
                : ParticleRegistry.EMBER_PARTICLE.get();

            level.addParticle(
                pType,
                p.x, p.y, p.z,
                (Math.random() - 0.5D) * 0.25D,
                0.15D + Math.random() * 0.4D,
                (Math.random() - 0.5D) * 0.25D
            );

            if (i % 4 == 0) {
                level.addParticle(
                    ParticleRegistry.FIERY_SMOKE_PARTICLE.get(),
                    p.x, p.y + 0.05D, p.z,
                    0.0D, 0.03D, 0.0D
                );
            }
            if (i % 5 == 0) {
                level.addParticle(
                    ParticleTypes.LAVA,
                    p.x, p.y + 0.1D, p.z,
                    0.0D, 0.1D, 0.0D
                );
            }
        }
    }


    /**
     * Server Combat Impact: Proximity Damage Falloff across 10-12 blocks.
     * Enemies near the center (<= 3 blocks) take full heavy damage (24.0) + 5s fire.
     * Enemies further away take gradually reduced damage (down to 6.0 at 12 blocks) + 2s fire.
     */
    public static void applyShockwaveDamage(ServerLevel level, LivingEntity caster, Vec3 origin, float maxRadius) {
        if (level == null || caster == null) {
            return;
        }

        AABB area = new AABB(
            origin.x - maxRadius, origin.y - 3.0D, origin.z - maxRadius,
            origin.x + maxRadius, origin.y + 3.0D, origin.z + maxRadius
        );

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (target == caster || !target.isAlive() || target.isInvulnerable() || target.isSpectator()) {
                continue;
            }
            if (DamageSources.isFriendlyFireBetween(caster, target)) {
                continue;
            }

            double dist = target.position().distanceTo(origin);
            if (dist > maxRadius) {
                continue;
            }

            float damage;
            int fireSeconds;
            if (dist <= CLOSE_RANGE) {
                damage = MAX_DAMAGE;
                fireSeconds = 5;
            } else {
                float falloffRatio = (float) ((dist - CLOSE_RANGE) / (maxRadius - CLOSE_RANGE));
                falloffRatio = Mth.clamp(falloffRatio, 0.0F, 1.0F);
                damage = Mth.lerp(falloffRatio, MAX_DAMAGE, MIN_DAMAGE);
                fireSeconds = 2;
            }

            // Always ignite all targets within skill range (even if blocking or in i-frames)
            target.setSecondsOnFire(fireSeconds);

            net.minecraft.world.damagesource.DamageSource source = (caster instanceof net.minecraft.world.entity.player.Player player)
                ? level.damageSources().playerAttack(player)
                : level.damageSources().mobAttack(caster);

            boolean damaged = DamageSources.applyDamage(target, damage, source);
            if (damaged) {

                // Radial knockback radiating outward from ground slam
                Vec3 pushDir = target.position().subtract(origin);
                double horizontalDist = Math.sqrt(pushDir.x * pushDir.x + pushDir.z * pushDir.z);
                if (horizontalDist > 0.001D) {
                    double strength = (1.0D - (dist / maxRadius)) * 1.2D + 0.3D;
                    target.push(
                        (pushDir.x / horizontalDist) * strength,
                        0.35D,
                        (pushDir.z / horizontalDist) * strength
                    );
                    target.hurtMarked = true;
                }
            }
        }
    }

    /**
     * 1.85s Server Shockwave: Secondary expanding 4-ring spiral shockwave.
     */
    public static void spawnSpiralShockwave(ServerLevel level, LivingEntity entity) {
        GroundShockwaveVfx.spawnSpiralFireShockwave(level, entity);
    }
}
