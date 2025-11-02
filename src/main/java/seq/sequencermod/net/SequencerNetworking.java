package seq.sequencermod.net;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import seq.sequencermod.core.SequencerMod;
import seq.sequencermod.sequencer.SequenceRegistry;
import seq.sequencermod.sequencer.SequenceRunnerManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class SequencerNetworking {
    // Каналы
    public static final Identifier C2S_PLAY = id("seq_play");
    public static final Identifier C2S_STOP = id("seq_stop");
    public static final Identifier C2S_REQUEST_SEQUENCES = id("seq_request_list");

    public static final Identifier S2C_APPLY_MORPH = id("apply_morph");
    public static final Identifier S2C_CLEAR_MORPH = id("clear_morph");
    public static final Identifier S2C_SEQUENCES = id("sequences");

    public static void init() {
        // C2S
        ServerPlayNetworking.registerGlobalReceiver(C2S_PLAY, (server, player, handler, buf, responseSender) -> {
            String id = buf.readString();
            server.execute(() -> {
                if (SequenceRegistry.isKnown(id)) {
                    SequenceRunnerManager.playFor(player, id);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(C2S_STOP, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> SequenceRunnerManager.stopFor(player));
        });

        ServerPlayNetworking.registerGlobalReceiver(C2S_REQUEST_SEQUENCES, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> sendSequencesToClient(player));
        });
    }

    public static void sendSequencesToClient(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();

        // Берём актуальные ID секвенций
        List<String> ids = new ArrayList<>(SequenceRegistry.listIds());
        ids.sort(String::compareTo);

        buf.writeVarInt(ids.size());
        for (String id : ids) {
            buf.writeString(id);
        }
        ServerPlayNetworking.send(player, S2C_SEQUENCES, buf);
    }

    public static void sendApplyMorphToAll(ServerPlayerEntity sourcePlayer, String entityId) {
        for (ServerPlayerEntity p : getAllPlayersTracking(sourcePlayer)) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeUuid(sourcePlayer.getUuid());
            buf.writeString(entityId);
            ServerPlayNetworking.send(p, S2C_APPLY_MORPH, buf);
        }
    }

    public static void sendClearMorphToAll(ServerPlayerEntity sourcePlayer) {
        for (ServerPlayerEntity p : getAllPlayersTracking(sourcePlayer)) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeUuid(sourcePlayer.getUuid());
            ServerPlayNetworking.send(p, S2C_CLEAR_MORPH, buf);
        }
    }

    private static Collection<ServerPlayerEntity> getAllPlayersTracking(ServerPlayerEntity player) {
        // MVP: отправляем всем онлайн игрокам.
        return player.getServer().getPlayerManager().getPlayerList();
    }

    private static Identifier id(String path) {
        return new Identifier(SequencerMod.MOD_ID, path);
    }

    private SequencerNetworking() {}
}