package io.redspace.ironspell_more.compat.avalon;

import io.redspace.ironspell_more.compat.api.CompatResult;
import io.redspace.ironspell_more.compat.api.VfxRequest;
import io.redspace.ironspell_more.compat.avalon.particle.AvalonVfx;

/**
 * Package-private loaded bridge for Avalon calls.
 */
final class AvalonLoadedBridge {
    private AvalonLoadedBridge() {
    }

    static CompatResult spawnVfx(VfxRequest request) {
        return AvalonVfx.spawnVfx(request);
    }
}
