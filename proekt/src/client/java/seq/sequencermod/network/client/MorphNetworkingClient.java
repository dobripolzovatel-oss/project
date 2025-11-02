package seq.sequencermod.network.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import seq.sequencermod.network.MorphPackets;

@Environment(EnvType.CLIENT)
public final class MorphNetworkingClient {
    private static final Identifier AIR = new Identifier("minecraft", "air");

    private MorphNetworkingClient() {}

    // Обычный запрос: только id
    public static void requestMorph(@Nullable Identifier vanillaEntityIdOrNull) {
        Identifier toSend = vanillaEntityIdOrNull == null ? AIR : vanillaEntityIdOrNull;
        PacketByteBuf out = PacketByteBufs.create();
        out.writeIdentifier(toSend);
        ClientPlayNetworking.send(MorphPackets.C2S_REQUEST_MORPH, out);
        System.out.println("[SequencerMod] C2S_REQUEST_MORPH sent: " + toSend);
    }

    // Вариант B: небезопасный запрос с размерами
    public static void requestMorphWithSize(Identifier id, float width, float height, @Nullable Float eyeHeight) {
        PacketByteBuf out = MorphPackets.buildMorphWithSizeC2S(id, width, height, eyeHeight);
        ClientPlayNetworking.send(MorphPackets.C2S_REQUEST_MORPH_WITH_SIZE, out);
        System.out.println("[SequencerMod] C2S_REQUEST_MORPH_WITH_SIZE sent: id=" + id + " w=" + width + " h=" + height + " eye=" + eyeHeight);
    }
}