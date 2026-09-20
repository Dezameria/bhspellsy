package io.redspace.ironspell_more.compat.aaaparticles;

import mod.chloeprime.aaaparticles.api.common.AAALevel;
import mod.chloeprime.aaaparticles.api.common.ParticleEmitterInfo;
import net.minecraft.world.level.Level;

/**
 * Package-private loaded bridge directly invoking AAA Particles (Effekseer).
 */
final class AaaParticlesLoadedBridge {
    private AaaParticlesLoadedBridge() {
    }

    static boolean addParticle(Level level, double maxDistance, ParticleEmitterInfo info) {
        AAALevel.addParticle(level, maxDistance, info);
        return true;
    }
}
