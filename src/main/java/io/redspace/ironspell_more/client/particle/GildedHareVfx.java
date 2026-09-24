package io.redspace.ironspell_more.client.particle;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class GildedHareVfx {
    public static final Vector3f COLOR_LIGHT_GOLD = new Vector3f(1.0F, 0.88F, 0.40F);
    public static final Vector3f COLOR_BRIGHT_GOLD = new Vector3f(1.0F, 0.82F, 0.10F);
    public static final Vector3f COLOR_WARM_GOLD = new Vector3f(0.98F, 0.70F, 0.05F);

    public static final Vector3f[] GOLD_PALETTE = new Vector3f[]{
            COLOR_LIGHT_GOLD,
            COLOR_BRIGHT_GOLD,
            COLOR_WARM_GOLD
    };

    /**
     * Spawns faint, gentle golden dust particles around the caster while Gilded Hare is active.
     */
    public static void spawnDustAura(LivingEntity entity) {
        Level level = entity.level();
        if (level == null) {
            return;
        }

        double time = entity.tickCount * 0.15D;
        double height = entity.getBbHeight();
        double radius = Math.max(0.5D, entity.getBbWidth() * 0.7D);

        for (int i = 0; i < 2; i++) {
            double angle = time + (i * Math.PI);
            double px = entity.getX() + Math.cos(angle) * radius;
            double pz = entity.getZ() + Math.sin(angle) * radius;
            double py = entity.getY() + 0.2D + ((Math.sin(time * 0.6D + (i * Math.PI)) + 1.0D) * 0.5D) * (height * 0.7D);

            Vector3f color = GOLD_PALETTE[i % GOLD_PALETTE.length];
            DustParticleOptions dust = new DustParticleOptions(color, 0.8F);

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(dust, px, py, pz, 1, 0.02D, 0.03D, 0.02D, 0.01D);
            } else {
                level.addParticle(dust, px, py, pz, 0.01D, 0.02D, 0.01D);
            }
        }

        // Occasional faint golden sparkle
        if (entity.getRandom().nextFloat() < 0.25F) {
            double rx = entity.getX() + (entity.getRandom().nextDouble() - 0.5D) * radius * 1.5D;
            double ry = entity.getY() + entity.getRandom().nextDouble() * height;
            double rz = entity.getZ() + (entity.getRandom().nextDouble() - 0.5D) * radius * 1.5D;
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.WAX_OFF, rx, ry, rz, 1, 0.02D, 0.02D, 0.02D, 0.01D);
            } else {
                level.addParticle(ParticleTypes.WAX_OFF, rx, ry, rz, 0.0D, 0.01D, 0.0D);
            }
        }
    }

    /**
     * Hit impact VFX when a kick hits an enemy.
     */
    public static void spawnKickImpactVfx(ServerLevel level, LivingEntity attacker, LivingEntity victim) {
        Vec3 impactPos = victim.position().add(0, victim.getBbHeight() * 0.5D, 0);

        // Disperse faint golden ribbons/dust along strike angle
        Vec3 dir = attacker.getLookAngle();
        for (int i = 0; i < 8; i++) {
            double spreadX = (victim.getRandom().nextDouble() - 0.5D) * 0.4D + dir.x * 0.1D;
            double spreadY = (victim.getRandom().nextDouble() - 0.5D) * 0.3D + 0.05D;
            double spreadZ = (victim.getRandom().nextDouble() - 0.5D) * 0.4D + dir.z * 0.1D;

            Vector3f color = GOLD_PALETTE[i % GOLD_PALETTE.length];
            level.sendParticles(new DustParticleOptions(color, 1.0F),
                    impactPos.x, impactPos.y, impactPos.z,
                    1, spreadX, spreadY, spreadZ, 0.05D);
        }

        level.sendParticles(ParticleTypes.CRIT,
                impactPos.x, impactPos.y, impactPos.z,
                4, 0.2D, 0.2D, 0.2D, 0.1D);

        level.playSound(null, impactPos.x, impactPos.y, impactPos.z,
                SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 0.9F, 1.3F);
        level.playSound(null, impactPos.x, impactPos.y, impactPos.z,
                SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.8F, 1.6F);
    }

    /**
     * Cocoon stun explosion when 5-hit finisher triggers.
     */
    public static void spawnCocoonStunBurst(ServerLevel level, LivingEntity victim) {
        double cx = victim.getX();
        double cy = victim.getY() + victim.getBbHeight() * 0.5D;
        double cz = victim.getZ();

        // Expanding golden crystal chime ring
        int ringSamples = 24;
        double radius = Math.max(0.7D, victim.getBbWidth() * 1.2D);
        for (int i = 0; i < ringSamples; i++) {
            double angle = (2.0D * Math.PI / ringSamples) * i;
            double px = cx + Math.cos(angle) * radius;
            double pz = cz + Math.sin(angle) * radius;
            double py = victim.getY() + 0.1D;

            double vx = Math.cos(angle) * 0.15D;
            double vz = Math.sin(angle) * 0.15D;

            level.sendParticles(new DustParticleOptions(COLOR_BRIGHT_GOLD, 1.2F),
                    px, py, pz, 0, vx, 0.05D, vz, 1.0D);
        }

        level.sendParticles(ParticleTypes.FLASH, cx, cy, cz, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.WAX_OFF, cx, cy, cz, 20, 0.4D, 0.6D, 0.4D, 0.05D);

        level.playSound(null, cx, cy, cz, SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.2F, 1.5F);
        level.playSound(null, cx, cy, cz, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, 1.2F);
        level.playSound(null, cx, cy, cz, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /**
     * Cocoon shatter/burst VFX and audio when the 1-second stun ends.
     */
    public static void spawnCocoonShatterVfx(ServerLevel level, LivingEntity victim) {
        double cx = victim.getX();
        double cy = victim.getY() + victim.getBbHeight() * 0.5D;
        double cz = victim.getZ();

        // Bursting golden ribbon & crystal shards in all directions
        int shardCount = 36;
        for (int i = 0; i < shardCount; i++) {
            double angle = (2.0D * Math.PI / shardCount) * i;
            double pitch = (victim.getRandom().nextDouble() - 0.5D) * Math.PI;
            double speed = 0.25D + victim.getRandom().nextDouble() * 0.25D;

            double vx = Math.cos(angle) * Math.cos(pitch) * speed;
            double vy = Math.sin(pitch) * speed + 0.1D;
            double vz = Math.sin(angle) * Math.cos(pitch) * speed;

            Vector3f color = GOLD_PALETTE[i % GOLD_PALETTE.length];
            level.sendParticles(new DustParticleOptions(color, 1.3F),
                    cx, cy, cz, 0, vx, vy, vz, 1.0D);
        }

        level.sendParticles(ParticleTypes.CRIT, cx, cy, cz, 16, 0.4D, 0.5D, 0.4D, 0.2D);
        level.sendParticles(ParticleTypes.WAX_OFF, cx, cy, cz, 24, 0.5D, 0.6D, 0.5D, 0.08D);

        // Sound: crystal ribbon shatter and snap
        level.playSound(null, cx, cy, cz, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.3F, 1.3F);
        level.playSound(null, cx, cy, cz, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.4F);
        level.playSound(null, cx, cy, cz, SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);
    }
}
