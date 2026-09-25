package io.redspace.ironspell_more.client.particle;

import io.redspace.ironspell_more.registry.ParticleRegistry;
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
     * Hit impact VFX when a kick hits an enemy with progressive ribbon intensity.
     */
    public static void spawnKickImpactVfx(ServerLevel level, LivingEntity attacker, LivingEntity victim, int comboCount) {
        Vec3 impactPos = victim.position().add(0, victim.getBbHeight() * 0.5D, 0);

        // Disperse golden ribbons/dust along strike angle, scaled by combo
        Vec3 dir = attacker.getLookAngle();
        int dustCount = 6 + comboCount * 3;
        for (int i = 0; i < dustCount; i++) {
            double spreadX = (victim.getRandom().nextDouble() - 0.5D) * 0.4D + dir.x * 0.12D;
            double spreadY = (victim.getRandom().nextDouble() - 0.5D) * 0.3D + 0.06D;
            double spreadZ = (victim.getRandom().nextDouble() - 0.5D) * 0.4D + dir.z * 0.12D;

            Vector3f color = GOLD_PALETTE[i % GOLD_PALETTE.length];
            level.sendParticles(new DustParticleOptions(color, 1.0F + comboCount * 0.1F),
                    impactPos.x, impactPos.y, impactPos.z,
                    1, spreadX, spreadY, spreadZ, 0.05D);
        }

        level.sendParticles(ParticleTypes.CRIT,
                impactPos.x, impactPos.y, impactPos.z,
                3 + comboCount, 0.2D, 0.2D, 0.2D, 0.1D);

        level.sendParticles(ParticleTypes.WAX_OFF,
                impactPos.x, impactPos.y, impactPos.z,
                2 + comboCount * 2, 0.25D, 0.25D, 0.25D, 0.04D);

        // Scatter golden hare particles leaping outward on impact
        int hareCount = 3 + comboCount;
        for (int i = 0; i < hareCount; i++) {
            double angle = victim.getRandom().nextDouble() * Math.PI * 2.0D;
            double speed = 0.12D + victim.getRandom().nextDouble() * 0.16D;
            double vx = Math.cos(angle) * speed + dir.x * 0.08D;
            double vy = 0.12D + victim.getRandom().nextDouble() * 0.14D;
            double vz = Math.sin(angle) * speed + dir.z * 0.08D;

            level.sendParticles(ParticleRegistry.GILDED_HARE.get(),
                    impactPos.x, impactPos.y, impactPos.z,
                    0, vx, vy, vz, 1.0D);
        }

        float pitch = 1.3F + (comboCount - 1) * 0.1F;
        level.playSound(null, impactPos.x, impactPos.y, impactPos.z,
                SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 0.9F, pitch);
        level.playSound(null, impactPos.x, impactPos.y, impactPos.z,
                SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.8F, pitch + 0.3F);
    }

    public static void spawnKickImpactVfx(ServerLevel level, LivingEntity attacker, LivingEntity victim) {
        spawnKickImpactVfx(level, attacker, victim, 1);
    }

    /**
     * Special Cocoon stun explosion when 5-hit finisher triggers (stack is full).
     * Emits expanding golden shockwave, radiant totem shower, end rod light beams,
     * golden helix ribbons, and chime resonance.
     */
    public static void spawnCocoonStunBurst(ServerLevel level, LivingEntity victim) {
        double cx = victim.getX();
        double cy = victim.getY() + victim.getBbHeight() * 0.5D;
        double cz = victim.getZ();

        // 1. Custom Golden Shockwave expanding outward
        float shockwaveRadius = (float) Math.max(1.8D, victim.getBbWidth() * 2.2D);
        level.sendParticles(new ShockwaveParticleOptionCustom(
                        new Vector3f(1.0F, 0.84F, 0.22F), shockwaveRadius, true, new Vector3f(0, 1, 0)),
                cx, victim.getY() + 0.15D, cz, 1, 0, 0, 0, 0);

        // 2. Radiant Golden Totem Shower
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                cx, cy, cz, 40, 0.45D, 0.65D, 0.45D, 0.35D);

        // 3. Ascending End Rod light motes
        level.sendParticles(ParticleTypes.END_ROD,
                cx, cy, cz, 20, 0.3D, 0.5D, 0.3D, 0.15D);

        // 4. Expanding golden crystal chime ring
        int ringSamples = 32;
        double radius = Math.max(0.8D, victim.getBbWidth() * 1.35D);
        for (int i = 0; i < ringSamples; i++) {
            double angle = (2.0D * Math.PI / ringSamples) * i;
            double px = cx + Math.cos(angle) * radius;
            double pz = cz + Math.sin(angle) * radius;
            double py = victim.getY() + 0.1D;

            double vx = Math.cos(angle) * 0.20D;
            double vz = Math.sin(angle) * 0.20D;

            level.sendParticles(new DustParticleOptions(COLOR_BRIGHT_GOLD, 1.4F),
                    px, py, pz, 0, vx, 0.06D, vz, 1.0D);
        }

        // 5. Vertical ascending golden helix ribbons around cocoon
        for (int i = 0; i < 20; i++) {
            double t = i / 20.0D;
            double h = victim.getY() + t * victim.getBbHeight();
            double a1 = t * Math.PI * 4.0D;
            double a2 = a1 + Math.PI;
            double hr = victim.getBbWidth() * 0.65D + 0.1D;

            level.sendParticles(new DustParticleOptions(COLOR_LIGHT_GOLD, 1.2F),
                    cx + Math.cos(a1) * hr, h, cz + Math.sin(a1) * hr, 1, 0, 0.02D, 0, 0.01D);
            level.sendParticles(new DustParticleOptions(COLOR_WARM_GOLD, 1.2F),
                    cx + Math.cos(a2) * hr, h, cz + Math.sin(a2) * hr, 1, 0, 0.02D, 0, 0.01D);
        }

        // 6. Radiant Flash and Wax Off sparkle bursts
        level.sendParticles(ParticleTypes.FLASH, cx, cy, cz, 2, 0.1D, 0.1D, 0.1D, 0);
        level.sendParticles(ParticleTypes.WAX_OFF, cx, cy, cz, 35, 0.45D, 0.7D, 0.45D, 0.08D);
        level.sendParticles(ParticleTypes.ENCHANT, cx, cy, cz, 30, 0.6D, 0.6D, 0.6D, 0.8D);

        // 7. Leaping Golden Hare burst in all radial directions
        int hareBurst = 12;
        for (int i = 0; i < hareBurst; i++) {
            double angle = (2.0D * Math.PI / hareBurst) * i + (victim.getRandom().nextDouble() - 0.5D) * 0.25D;
            double speed = 0.20D + victim.getRandom().nextDouble() * 0.15D;
            double vx = Math.cos(angle) * speed;
            double vy = 0.18D + victim.getRandom().nextDouble() * 0.14D;
            double vz = Math.sin(angle) * speed;

            level.sendParticles(ParticleRegistry.GILDED_HARE.get(),
                    cx, cy, cz, 0, vx, vy, vz, 1.0D);
        }

        // 8. Resonant audio
        level.playSound(null, cx, cy, cz, SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.4F, 1.4F);
        level.playSound(null, cx, cy, cz, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.6F, 1.2F);
        level.playSound(null, cx, cy, cz, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2F, 0.9F);
        level.playSound(null, cx, cy, cz, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.6F, 1.8F);
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

        for (int i = 0; i < 6; i++) {
            double angle = (2.0D * Math.PI / 6) * i;
            double speed = 0.20D + victim.getRandom().nextDouble() * 0.10D;
            level.sendParticles(ParticleRegistry.GILDED_HARE.get(),
                    cx, cy, cz, 0, Math.cos(angle) * speed, 0.14D, Math.sin(angle) * speed, 1.0D);
        }

        level.sendParticles(ParticleTypes.CRIT, cx, cy, cz, 20, 0.4D, 0.5D, 0.4D, 0.25D);
        level.sendParticles(ParticleTypes.WAX_OFF, cx, cy, cz, 30, 0.5D, 0.6D, 0.5D, 0.08D);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, cx, cy, cz, 20, 0.4D, 0.5D, 0.4D, 0.2D);

        // Sound: crystal ribbon shatter and snap
        level.playSound(null, cx, cy, cz, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.3F, 1.3F);
        level.playSound(null, cx, cy, cz, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.4F);
        level.playSound(null, cx, cy, cz, SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);
    }
}
