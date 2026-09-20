package io.redspace.ironspell_more.compat.avalon;

import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.compat.CompatMods;
import io.redspace.ironspell_more.compat.api.CompatResult;
import io.redspace.ironspell_more.compat.api.VfxRequest;

/**
 * Public safe facade for Epic Fight - Avalon integration.
 */
public final class AvalonCompat {
    private static boolean linkageFailed;

    private AvalonCompat() {
    }

    public static boolean isAvailable() {
        return CompatMods.isAvalonLoaded() && !linkageFailed;
    }

    public static CompatResult spawnVfx(VfxRequest request) {
        if (!isAvailable() || request == null) {
            return CompatResult.UNAVAILABLE;
        }

        try {
            return AvalonLoadedBridge.spawnVfx(request);
        } catch (LinkageError error) {
            linkageFailed = true;
            IronSpellMore.LOGGER.error("Avalon VFX invocation failed due to linkage error", error);
            return CompatResult.FAILED;
        }
    }
}
