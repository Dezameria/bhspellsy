package io.redspace.ironspell_more.compat.aaaparticles;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.compat.CompatMods;
import mod.chloeprime.aaaparticles.api.common.ParticleEmitterInfo;
import net.minecraft.world.level.Level;

/**
 * Public safe facade for AAA Particles (Effekseer) playback.
 */
public final class AaaParticlesCompat {
    private static boolean linkageFailed;

    private AaaParticlesCompat() {
    }

    public static boolean isAvailable() {
        return CompatMods.isAaaParticlesLoaded() && !linkageFailed;
    }

    public static boolean addParticle(Level level, double maxDistance, ParticleEmitterInfo info) {
        if (!isAvailable() || level == null || info == null) {
            return false;
        }

        try {
            return AaaParticlesLoadedBridge.addParticle(level, maxDistance, info);
        } catch (LinkageError error) {
            linkageFailed = true;
            IronSpellMore.LOGGER.error("AAA Particles invocation failed due to linkage error", error);
            return false;
        }
    }
}
