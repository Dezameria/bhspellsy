package io.redspace.ironspell_more.network;

import io.redspace.ironspell_more.client.event.TigershadeClientEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;

public final class SyncTigershadeTargetPacket {
    @Nullable
    private final UUID targetUuid;

    public SyncTigershadeTargetPacket(@Nullable UUID targetUuid) {
        this.targetUuid = targetUuid;
    }

    public static void encode(SyncTigershadeTargetPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.targetUuid != null);
        if (packet.targetUuid != null) {
            buffer.writeUUID(packet.targetUuid);
        }
    }

    public static SyncTigershadeTargetPacket decode(FriendlyByteBuf buffer) {
        return new SyncTigershadeTargetPacket(buffer.readBoolean() ? buffer.readUUID() : null);
    }

    public static void handle(SyncTigershadeTargetPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> TigershadeClientEvents.setMarkedTarget(packet.targetUuid)));
        context.setPacketHandled(true);
    }
}
