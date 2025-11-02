package seq.sequencermod.morph.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * Совместимый враппер для старых импортов.
 * Делегирует в общий seq.sequencermod.network.MorphPackets.
 */
public final class MorphPackets {
    public static final Identifier C2S_REQUEST_MORPH           = seq.sequencermod.network.MorphPackets.C2S_REQUEST_MORPH;
    public static final Identifier S2C_MORPH_SYNC              = seq.sequencermod.network.MorphPackets.S2C_MORPH_SYNC;
    public static final Identifier C2S_REQUEST_MORPH_WITH_SIZE = seq.sequencermod.network.MorphPackets.C2S_REQUEST_MORPH_WITH_SIZE;

    // Старый helper: оставить для совместимости
    public static PacketByteBuf makeRequestBuf(Identifier morphId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeIdentifier(morphId);
        return buf;
    }

    // Новый helper: делегат
    public static PacketByteBuf buildMorphWithSizeC2S(Identifier id, float width, float height, Float eyeHeight) {
        return seq.sequencermod.network.MorphPackets.buildMorphWithSizeC2S(id, width, height, eyeHeight);
    }

    private MorphPackets() {}
}