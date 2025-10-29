package seq.sequencermod.net.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import seq.sequencermod.network.MorphPackets;
import seq.sequencermod.size.PlayerClientSizes;
import seq.sequencermod.size.PlayerSizeData;

import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class PlayerSizeClientSync {
    private PlayerSizeClientSync() {}

    private static boolean inited = false;

    public static void bootstrap() {
        if (inited) return;
        inited = true;

        ClientPlayNetworking.registerGlobalReceiver(MorphPackets.S2C_PLAYER_SIZE_SYNC, (client, handler, buf, rs) -> {
            UUID who = buf.readUuid();
            boolean active = buf.readBoolean();

            float w = 0f, h = 0f;
            Float eye = null;

            if (active) {
                w = buf.readFloat();
                h = buf.readFloat();
                boolean hasEye = buf.readBoolean();
                if (hasEye) eye = buf.readFloat();
            }

            final float fW = w, fH = h;
            final Float fEye = eye;

            client.execute(() -> {
                if (!active) {
                    // Удаляем запись только для конкретного игрока
                    PlayerClientSizes.remove(who);
                } else {
                    // Единый стор: кладём PlayerSizeData
                    PlayerClientSizes.put(who, new PlayerSizeData(fW, fH, fEye, null, false, false));
                }

                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null && mc.player != null && who.equals(mc.player.getUuid())) {
                    try { mc.player.calculateDimensions(); } catch (Throwable ignored) {}
                }
            });
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            // Полная очистка стора при отключении
            PlayerClientSizes.clear();
        });
    }
}