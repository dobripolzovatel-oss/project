package seq.sequencermod.size.net;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import seq.sequencermod.size.PlayerClientSizes;
import seq.sequencermod.size.PlayerSizeData;
import java.util.UUID;

public class SizePacketsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(SizePackets.S2C_SYNC, (client, handler, buf, responseSender) -> {
            UUID id = buf.readUuid();
            boolean has = buf.readBoolean();
            PlayerSizeData data = null;
            if (has) {
                float w = buf.readFloat();
                float h = buf.readFloat();
                boolean hasEye = buf.readBoolean(); Float eye = hasEye ? buf.readFloat() : null;
                boolean hasScale = buf.readBoolean(); if (hasScale) buf.readFloat(); // не используется
                data = new PlayerSizeData(w, h, eye, null, false, false);
            }
            PlayerSizeData finalData = data;
            client.execute(() -> applyClientSync(client, id, finalData));
        });
    }

    private static void applyClientSync(MinecraftClient client, UUID id, PlayerSizeData data) {
        if (data == null) {
            PlayerClientSizes.remove(id);
        } else {
            PlayerClientSizes.put(id, data);
        }

        if (client.world != null) {
            var e = client.world.getPlayerByUuid(id);
            if (e != null) e.calculateDimensions();
        }
        if (client.player != null && client.player.getUuid().equals(id)) {
            client.player.calculateDimensions();
        }
    }
}