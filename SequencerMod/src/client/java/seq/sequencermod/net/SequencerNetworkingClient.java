package seq.sequencermod.net;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import seq.sequencermod.net.client.MorphClientSync;
import seq.sequencermod.network.MorphPackets; // <-- добавлен импорт

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static seq.sequencermod.net.SequencerNetworking.C2S_REQUEST_SEQUENCES;
import static seq.sequencermod.net.SequencerNetworking.S2C_APPLY_MORPH;
import static seq.sequencermod.net.SequencerNetworking.S2C_CLEAR_MORPH;
import static seq.sequencermod.net.SequencerNetworking.S2C_SEQUENCES;

public final class SequencerNetworkingClient {
    // Было: Set<String>. GUI удобнее по индексу – держим список.
    public static final List<String> CLIENT_SEQUENCES = new ArrayList<>();

    public static void initClient() {
        // Получить список секвенций с сервера
        ClientPlayNetworking.registerGlobalReceiver(S2C_SEQUENCES, (client, handler, buf, responseSender) -> {
            int n = buf.readVarInt();
            Set<String> tmp = new LinkedHashSet<>(n);
            for (int i = 0; i < n; i++) tmp.add(buf.readString());
            var list = new ArrayList<>(tmp);
            client.execute(() -> {
                CLIENT_SEQUENCES.clear();
                CLIENT_SEQUENCES.addAll(list);
            });
        });

        // Совместимость со старыми серверными каналами морфа
        ClientPlayNetworking.registerGlobalReceiver(S2C_APPLY_MORPH, (client, handler, buf, responseSender) -> {
            UUID who = buf.readUuid();
            String entityId = buf.readString();
            client.execute(() -> {
                if (who != null && entityId != null && !entityId.isBlank()) {
                    MorphClientSync.setLocalMorph(who, new Identifier(entityId));
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(S2C_CLEAR_MORPH, (client, handler, buf, responseSender) -> {
            UUID who = buf.readUuid();
            client.execute(() -> MorphClientSync.clearLocalMorph(who));
        });
    }

    public static void requestSequencesC2S() {
        if (MinecraftClient.getInstance().getNetworkHandler() == null) return;
        PacketByteBuf buf = PacketByteBufs.create();
        ClientPlayNetworking.send(C2S_REQUEST_SEQUENCES, buf);
    }

    /**
     * Запросить морф у сервера. Если сети нет (singleplayer/офлайн) – применить локально.
     * Передай null, чтобы снять морф.
     */
    public static void requestMorphC2S(Identifier typeOrNull) {
        MinecraftClient mc = MinecraftClient.getInstance();

        // Оффлайн/нет сетевого хендлера – локальный фолбэк
        if (mc.getNetworkHandler() == null) {
            if (mc.player != null) {
                if (typeOrNull == null) MorphClientSync.clearLocalMorph(mc.player.getUuid());
                else MorphClientSync.setLocalMorph(mc.player.getUuid(), typeOrNull);
            }
            return;
        }

        // Онлайн – шлём на сервер единый C2S канал
        PacketByteBuf out = PacketByteBufs.create();
        // Протокол: отправляем Identifier; для "снять морф" – шлём заведомо несуществующий id (minecraft:air).
        // На сервере MorphServer воспримет как "unknown" и очистит морф.
        out.writeIdentifier(typeOrNull != null ? typeOrNull : new Identifier("minecraft", "air"));
        ClientPlayNetworking.send(MorphPackets.C2S_REQUEST_MORPH, out);
    }

    private SequencerNetworkingClient() {}
}