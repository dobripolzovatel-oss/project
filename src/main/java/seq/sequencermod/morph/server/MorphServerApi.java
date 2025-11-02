package seq.sequencermod.morph.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import seq.sequencermod.morph.api.MorphAccess;
import seq.sequencermod.network.MorphPackets;

public final class MorphServerApi {
    private MorphServerApi() {}

    /**
     * Применяет морф к игроку на сервере:
     * - записывает id в MorphAccess
     * - пересчитывает размеры хитбокса
     * - рассылает синхронизацию всем клиентам (включая самого игрока)
     */
    public static void applyMorph(ServerPlayerEntity player, Identifier morphTypeId) {
        if (!(player instanceof MorphAccess access)) return;

        access.sequencer$setMorphTypeId(morphTypeId);

        // Критично: немедленно пересчитать размеры
        player.calculateDimensions();

        // Готовим payload один раз
        var payload = PacketByteBufs.create();
        payload.writeUuid(player.getUuid());
        payload.writeBoolean(morphTypeId != null);
        if (morphTypeId != null) payload.writeIdentifier(morphTypeId);

        // Рассылаем всем игрокам (включая самого морфнувшегося)
        for (var p : player.server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, MorphPackets.S2C_MORPH_SYNC, PacketByteBufs.copy(payload));
        }
    }
}