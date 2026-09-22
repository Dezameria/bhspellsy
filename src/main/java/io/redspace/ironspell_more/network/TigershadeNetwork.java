package io.redspace.ironspell_more.network;

import io.redspace.ironspell_more.IronSpellMore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public final class TigershadeNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            IronSpellMore.id("tigershade"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private TigershadeNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(
                0,
                SyncTigershadeTargetPacket.class,
                SyncTigershadeTargetPacket::encode,
                SyncTigershadeTargetPacket::decode,
                SyncTigershadeTargetPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void syncTarget(ServerPlayer player, @Nullable UUID targetUuid) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncTigershadeTargetPacket(targetUuid));
    }
}
