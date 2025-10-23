package seq.sequencermod.network.server;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import seq.sequencermod.network.MorphPackets;
import seq.sequencermod.morph.server.MorphServerState;

public final class MorphNetworkingServer {
    private MorphNetworkingServer() {}

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(MorphPackets.C2S_REQUEST_MORPH, (server, player, handler, buf, rs) -> {
            Identifier requested = buf.readIdentifier();
            server.execute(() -> {
                System.out.println("[SequencerMod] C2S morph request from " + player.getEntityName() + ": " + requested);
                MorphServerState.set(player, requested);
                player.calculateDimensions();
                // здесь можно отправить S2C синхронизацию морфа, если предусмотрено
            });
        });
    }
}