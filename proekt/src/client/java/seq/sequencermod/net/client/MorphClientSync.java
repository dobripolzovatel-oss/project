package seq.sequencermod.net.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import seq.sequencermod.net.client.morphs.MorphRuntimeFlags;
import seq.sequencermod.network.MorphPackets;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class MorphClientSync {
    private MorphClientSync() {}

    private static boolean inited = false;
    private static final Map<UUID, Identifier> MORPHED = new HashMap<>();

    private static boolean isVanillaAllay(Identifier id) {
        return id != null && "minecraft".equals(id.getNamespace()) && "allay".equals(id.getPath());
    }

    private static void refreshRenderOnlyFlag(UUID who, Identifier type) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return;
        mc.world.getPlayers().forEach(p -> {
            if (p.getUuid().equals(who)) {
                MorphRuntimeFlags.setAllayMorph(p, isVanillaAllay(type));
            }
        });
    }

    // Пересчитать размеры у локального игрока (камера/клиентские проверки)
    private static void recalcIfLocal(UUID who) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (who.equals(mc.player.getUuid())) {
            try { mc.player.calculateDimensions(); } catch (Throwable ignored) {}
        }
    }

    public static void bootstrap() {
        if (inited) return;
        inited = true;

        // S2C: синхронизация морфа (+ опционально размеры хитбокса от сервера, Вариант B)
        ClientPlayNetworking.registerGlobalReceiver(MorphPackets.S2C_MORPH_SYNC, (client, handler, buf, rs) -> {
            UUID who = buf.readUuid();
            boolean active = buf.readBoolean();

            if (!active) {
                client.execute(() -> {
                    Identifier prev = MORPHED.remove(who);
                    // Очистить клиентские оверрайды размеров, если были
                    MorphClientSizes.clear(who);
                    if (isVanillaAllay(prev)) refreshRenderOnlyFlag(who, null);
                    recalcIfLocal(who);
                });
                return;
            }

            // Активный морф
            Identifier type = buf.readIdentifier();

            // Начиная с Варианта B сервер может прислать размеры:
            // hasSize, width, height, hasEye, (eye?)
            boolean hasSize = false;
            float w = 0f, h = 0f;
            @Nullable Float eye = null;

            // Робастная обработка: если сервер старой версии, блока размеров может не быть
            if (buf.isReadable()) {
                try {
                    hasSize = buf.readBoolean();
                    if (hasSize) {
                        w = buf.readFloat();
                        h = buf.readFloat();
                        boolean hasEye = buf.readBoolean();
                        if (hasEye) eye = buf.readFloat();
                    }
                } catch (IndexOutOfBoundsException ignored) {
                    hasSize = false;
                    eye = null;
                }
            }

            final boolean fHasSize = hasSize;
            final float fW = w, fH = h;
            final Float fEye = eye;

            client.execute(() -> {
                MORPHED.put(who, type);

                // Сохраняем/очищаем размеры, пришедшие с сервера
                if (fHasSize) {
                    MorphClientSizes.set(who, type, fW, fH, fEye);
                } else {
                    MorphClientSizes.clear(who);
                }

                refreshRenderOnlyFlag(who, type);
                recalcIfLocal(who);
            });
        });

        // Очистка при disconnect
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            MORPHED.clear();
            MorphClientSizes.clear(null); // no-op safe, но оставим явную очистку ниже
            if (client.world != null) {
                client.world.getPlayers().forEach(p -> {
                    MorphRuntimeFlags.setAllayMorph(p, false);
                    MorphClientSizes.clear(p.getUuid());
                });
            }
        });

        SimpleMorphs.bootstrap();
    }

    public static boolean shouldHidePlayerModel(UUID playerUuid, boolean isLocalPlayer, boolean isFirstPerson) {
        Identifier id = MORPHED.get(playerUuid);
        boolean morphed = id != null;
        if (!morphed) return false;
        if (isVanillaAllay(id)) return true;
        if (isLocalPlayer && isFirstPerson) return false;
        return true;
    }

    public static Identifier getMorphType(UUID playerUuid) {
        return MORPHED.get(playerUuid);
    }

    // === Local (offline/dev) ===
    public static void setLocalMorph(UUID who, Identifier type) {
        if (who == null || type == null) return;
        MORPHED.put(who, type);
        refreshRenderOnlyFlag(who, type);
        recalcIfLocal(who);
    }

    public static void clearLocalMorph(UUID who) {
        if (who == null) return;
        Identifier prev = MORPHED.remove(who);
        MorphClientSizes.clear(who);
        if (isVanillaAllay(prev)) refreshRenderOnlyFlag(who, null);
        recalcIfLocal(who);
    }

    // === C2S: запрос применения морфа (только id) ===
    public static void requestMorph(Identifier typeOrAir) {
        if (!ClientPlayNetworking.canSend(MorphPackets.C2S_REQUEST_MORPH)) {
            System.out.println("[Sequencer|Client] Cannot send C2S_REQUEST_MORPH, channel not available");
            return;
        }
        System.out.println("[Sequencer|Client] C2S_REQUEST_MORPH send id=" + typeOrAir);
        PacketByteBuf out = PacketByteBufs.create();
        out.writeIdentifier(typeOrAir);
        ClientPlayNetworking.send(MorphPackets.C2S_REQUEST_MORPH, out);
    }

    public static void requestClear() {
        requestMorph(new Identifier("minecraft", "air"));
    }

    // === C2S: Вариант B — запрос с размерами (НЕБЕЗОПАСНО, только для доверенных серверов) ===
    public static void requestMorphWithSize(Identifier id, float width, float height, @Nullable Float eyeHeight) {
        if (!ClientPlayNetworking.canSend(MorphPackets.C2S_REQUEST_MORPH_WITH_SIZE)) {
            System.out.println("[Sequencer|Client] Cannot send C2S_REQUEST_MORPH_WITH_SIZE, channel not available");
            return;
        }
        PacketByteBuf out = MorphPackets.buildMorphWithSizeC2S(id, width, height, eyeHeight);
        ClientPlayNetworking.send(MorphPackets.C2S_REQUEST_MORPH_WITH_SIZE, out);
        System.out.println("[Sequencer|Client] C2S_REQUEST_MORPH_WITH_SIZE send id=" + id
                + " w=" + width + " h=" + height + " eye=" + eyeHeight);
    }
}