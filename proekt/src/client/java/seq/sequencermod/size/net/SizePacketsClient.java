package seq.sequencermod.size.net;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.World;
import seq.sequencermod.client.debug.CameraDebugClient;
import seq.sequencermod.size.PlayerClientSizes;
import seq.sequencermod.size.PlayerSizeData;

import java.util.UUID;

public class SizePacketsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CameraDebugClient.init();

        ClientPlayNetworking.registerGlobalReceiver(SizePackets.S2C_SYNC, (client, handler, buf, responseSender) -> {
            UUID id = buf.readUuid();
            boolean has = buf.readBoolean();
            PlayerSizeData data = null;
            if (has) {
                float w = buf.readFloat();
                float h = buf.readFloat();
                boolean hasEye = buf.readBoolean(); if (hasEye) buf.readFloat();
                boolean hasScale = buf.readBoolean(); if (hasScale) buf.readFloat();
                data = new PlayerSizeData(w, h, null, null, false, false);
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

        World w = client.world;
        if (w != null) {
            var e = w.getPlayerByUuid(id);
            if (e != null) e.calculateDimensions();
        }
        if (client.player != null && client.player.getUuid().equals(id)) {
            client.player.calculateDimensions();
        }
    }
}