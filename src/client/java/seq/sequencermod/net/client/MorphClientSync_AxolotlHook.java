package seq.sequencermod.net.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import seq.sequencermod.network.MorphPackets;
import seq.sequencermod.net.client.morphs.runtime.AxolotlMorphRuntime;

@Environment(EnvType.CLIENT)
public final class MorphClientSync_AxolotlHook {
    private MorphClientSync_AxolotlHook() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(MorphPackets.S2C_AXOLOTL_STATE, (client, handler, buf, rs) -> {
            var who = buf.readUuid();
            int playDeadTicks = buf.readVarInt();
            int variant = buf.readVarInt();
            int air = buf.readVarInt();
            client.execute(() -> {
                var rt = AxolotlMorphRuntime.get(who, true);
                // Жесткая синхронизация (клиент доверяет серверу)
                if (playDeadTicks > 0) rt.startPlayDeadLocal(playDeadTicks);
                else rt.stopPlayDeadLocal();
                rt.setVariant(variant);
                rt.setAir(air);
            });
        });
    }
}