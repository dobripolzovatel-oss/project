package seq.sequencermod.size.net;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import seq.sequencermod.size.PlayerSizeData;
import seq.sequencermod.size.PlayerSizeServerStore;

import java.util.UUID;

public final class SizePackets {
    public static final Identifier S2C_SYNC = new Identifier("sequencermod", "size_sync");

    private SizePackets() {}

    public static PacketByteBuf makeSync(UUID uuid, PlayerSizeData data) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(uuid);
        if (data == null) {
            buf.writeBoolean(false);
            return buf;
        }
        buf.writeBoolean(true);
        buf.writeFloat(data.width);
        buf.writeFloat(data.height);
        buf.writeBoolean(false); // eye present? (ignored)
        buf.writeBoolean(false); // scale present? (ignored)
        return buf;
    }

    public static void broadcastAll(ServerPlayerEntity changedPlayer, PlayerSizeData data) {
        var server = changedPlayer.getServer();
        if (server == null) return;
        UUID id = changedPlayer.getUuid();
        for (ServerPlayerEntity sp : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(sp, S2C_SYNC, makeSync(id, data));
        }
    }

    public static void sendAllTo(ServerPlayerEntity target) {
        for (var e : PlayerSizeServerStore.all()) {
            ServerPlayNetworking.send(target, S2C_SYNC, makeSync(e.getKey(), e.getValue()));
        }
    }
}