package io.redspace.ironspell_more.compat.avalon.particle;

import io.redspace.ironspell_more.compat.api.CompatResult;
import io.redspace.ironspell_more.compat.api.VfxCue;
import io.redspace.ironspell_more.compat.api.VfxRequest;
import net.minecraft.world.entity.player.Player;

/**
 * Direct Avalon VFX operations.
 */
public final class AvalonVfx {
    private AvalonVfx() {
    }

    public static CompatResult spawnVfx(VfxRequest request) {
        if (request == null || request.level().isClientSide) {
            return CompatResult.UNSUPPORTED;
        }

        if (request.cue() == VfxCue.AVALON_ENERGY) {
            // Avalon energy or camera shake trigger
            if (request.source() instanceof Player player) {
                try {
                    // Avalon camera shake
                    com.merlin204.avalon.network.NetworkHandler.sendToClient(
                        new com.merlin204.avalon.network.server.ShakeCameraPacket(10, 0.4F, 1.2F, player.position(), 16.0F),
                        (net.minecraft.server.level.ServerPlayer) player
                    );
                    return CompatResult.APPLIED;
                } catch (Throwable ignored) {
                }
            }
        }

        return CompatResult.UNSUPPORTED;
    }
}
