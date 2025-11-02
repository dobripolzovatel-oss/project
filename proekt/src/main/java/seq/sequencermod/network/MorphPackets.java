package seq.sequencermod.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

import java.util.UUID;

/**
 * Общие (shared) идентификаторы и конструкторы буферов для сетевых пакетов.
 * Только то, что требуется как клиенту, так и серверу (без клиент-рендер импортов).
 */
public final class MorphPackets {
    private MorphPackets() {}

    public static final String MODID = "sequencermod";

    // --- Базовые ---
    public static final Identifier C2S_REQUEST_MORPH = new Identifier(MODID, "morph_request");
    public static final Identifier S2C_MORPH_SYNC    = new Identifier(MODID, "morph_sync");

    // --- Вариант B: небезопасный канал с размерами от клиента ---
    public static final Identifier C2S_REQUEST_MORPH_WITH_SIZE = new Identifier(MODID, "morph_request_with_size");

    // --- Независимый синк размеров игрока (вне морфов) ---
    public static final Identifier S2C_PLAYER_SIZE_SYNC = new Identifier(MODID, "player_size_sync");

    // --- Прочие (аксолотль) ---
    public static final Identifier C2S_AXOLOTL_PLAY_DEAD   = new Identifier(MODID, "axolotl_play_dead");
    public static final Identifier C2S_AXOLOTL_SET_VARIANT = new Identifier(MODID, "axolotl_variant");
    public static final Identifier S2C_AXOLOTL_STATE       = new Identifier(MODID, "axolotl_state");

    // ======================= Helper builders =======================

    public static PacketByteBuf buildPlayDeadC2S(boolean start) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(start);
        return buf;
    }

    public static PacketByteBuf buildVariantC2S(int variant) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(variant);
        return buf;
    }

    public static PacketByteBuf buildAxolotlStateS2C(UUID who, int playDeadTicks, int variant, int air) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(who);
        buf.writeVarInt(playDeadTicks);
        buf.writeVarInt(variant);
        buf.writeVarInt(air);
        return buf;
    }

    // === Вариант B: C2S (morph id + размеры) ===
    // Если eyeHeight == null — пишем hasEye=false и глазную высоту не передаём.
    public static PacketByteBuf buildMorphWithSizeC2S(Identifier id, float width, float height, Float eyeHeight) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeIdentifier(id);
        buf.writeFloat(width);
        buf.writeFloat(height);
        boolean hasEye = eyeHeight != null;
        buf.writeBoolean(hasEye);
        if (hasEye) buf.writeFloat(eyeHeight);
        return buf;
    }

    // === Независимый синк размеров игрока S2C ===
    // active=false => очистка; active=true => (w,h[,eye])
    public static PacketByteBuf buildPlayerSizeSyncS2C(UUID who, boolean active, float w, float h, Float eye) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(who);
        buf.writeBoolean(active);
        if (active) {
            buf.writeFloat(w);
            buf.writeFloat(h);
            boolean hasEye = eye != null;
            buf.writeBoolean(hasEye);
            if (hasEye) buf.writeFloat(eye);
        }
        return buf;
    }
}