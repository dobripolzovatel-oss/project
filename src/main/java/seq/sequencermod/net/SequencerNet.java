package seq.sequencermod.net;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import seq.sequencermod.extendedreach.BlockReach;

public final class SequencerNet {
    public static final String MODID = "sequencermod";
    public static final Identifier REACH_SYNC = new Identifier(MODID, "reach_sync");

    private SequencerNet() { }

    public static void initServer() {
        // При входе игрока отправляем значение reach
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            sendReach(handler.player);
        });

        // Если нужно — добавьте тут обработчики входящих C2S пакетов от клиента
        // ServerPlayNetworking.registerGlobalReceiver(new Identifier(MODID, "some_c2s"), (server, player, handler, buf, responseSender) -> { ... });
    }

    public static void sendReach(ServerPlayerEntity player) {
        float reach = (float) BlockReach.getReach(player);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeFloat(reach);
        ServerPlayNetworking.send(player, REACH_SYNC, buf);
    }
}