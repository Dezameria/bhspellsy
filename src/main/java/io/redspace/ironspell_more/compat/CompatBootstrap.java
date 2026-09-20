package io.redspace.ironspell_more.compat;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.compat.epicfight.EpicFightCompat;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Early lifecycle bootstrap for conditional mod integrations.
 */
public final class CompatBootstrap {
    private static boolean initialized;

    private CompatBootstrap() {
    }

    public static void init(IEventBus modEventBus) {
        if (initialized) {
            return;
        }
        initialized = true;

        if (CompatMods.isEpicFightLoaded()) {
            try {
                EpicFightCompat.registerModEvents(modEventBus);
                IronSpellMore.LOGGER.info("Epic Fight compatibility initialized successfully.");
            } catch (Throwable t) {
                IronSpellMore.LOGGER.error("Failed to initialize Epic Fight compatibility", t);
            }
        }

        if (CompatMods.isAvalonLoaded()) {
            IronSpellMore.LOGGER.info("Epic Fight - Avalon compatibility detected.");
        }

        if (CompatMods.isAaaParticlesLoaded()) {
            IronSpellMore.LOGGER.info("AAA Particles (Effekseer) compatibility detected.");
        }
    }
}
