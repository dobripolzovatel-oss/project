package seq.sequencermod.server.morph;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import seq.sequencermod.morph.runtime.MorphSizeLookupServer;
import seq.sequencermod.morph.server.MorphServerState;
import seq.sequencermod.network.MorphPackets;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MorphServer {
    private MorphServer() {}

    private static volatile boolean inited = false;

    // Variant B: принимать размеры от клиента (НЕБЕЗОПАСНО — держите true только на доверенных серверах)
    private static final boolean UNSAFE_ALLOW_CLIENT_SIZES = true;

    // Необязательный whitelist тип-идов, для которых разрешена передача размеров клиентом
    private static final Set<Identifier> SIZE_WHITELIST = new HashSet<>();
    static {
        // Пример:
        // SIZE_WHITELIST.add(new Identifier("minecraft", "allay"));
    }

    public static void bootstrap() {
        if (inited) return;
        synchronized (MorphServer.class) {
            if (inited) return;
            inited = true;
        }

        // C2S: клиент просит применить морф (только id)
        ServerPlayNetworking.registerGlobalReceiver(MorphPackets.C2S_REQUEST_MORPH, (server, player, handler, buf, rs) -> {
            Identifier req = buf.readIdentifier();
            server.execute(() -> applyOrClear(server, player, req, null));
        });

        // C2S: клиент просит применить морф с размерами (Variant B)
        ServerPlayNetworking.registerGlobalReceiver(MorphPackets.C2S_REQUEST_MORPH_WITH_SIZE, (server, player, handler, buf, rs) -> {
            Identifier id = buf.readIdentifier();
            float w = buf.readFloat();
            float h = buf.readFloat();
            boolean hasEye = buf.readBoolean();
            Float eye = hasEye ? buf.readFloat() : null;
            server.execute(() -> handleMorphWithSize(server, player, id, w, h, eye));
        });

        // При входе — разослать активные морфы всем, плюс новичку — состояние всех (с размерами, если есть)
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity joined = handler.player;
            for (Map.Entry<UUID, Identifier> e : MorphServerState.snapshot().entrySet()) {
                var runtime = MorphSizeLookupServer.get(e.getKey());
                boolean hasSize = runtime != null && e.getValue().equals(runtime.morphId);
                sendSyncTo(joined, e.getKey(), true, e.getValue(),
                        hasSize ? runtime.width : 0f,
                        hasSize ? runtime.height : 0f,
                        hasSize ? runtime.eyeHeight : null);
            }
            Identifier mine = MorphServerState.get(joined);
            if (mine != null) {
                var runtime = MorphSizeLookupServer.get(joined.getUuid());
                boolean hasSize = runtime != null && mine.equals(runtime.morphId);
                broadcastSync(server, joined.getUuid(), true, mine,
                        hasSize ? runtime.width : 0f,
                        hasSize ? runtime.height : 0f,
                        hasSize ? runtime.eyeHeight : null);
            }
        });

        // По желанию: очищать персональные размеры при выходе
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            MorphSizeLookupServer.clear(handler.player.getUuid());
        });
    }

    // Публичный API, на который ссылаются ServerLifecycleHooks / MorphCommands / Sequence*:
    // syncToAll(p, null) — снять морф; syncToAll(p, id) — применить и разослать.
    public static void syncToAll(ServerPlayerEntity player, Identifier typeOrNull) {
        if (player == null) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;

        if (typeOrNull == null) {
            // Снять морф + очистить runtime-размеры, пересчитать габариты и разослать
            MorphServerState.clear(player);
            MorphSizeLookupServer.clear(player.getUuid());
            safeRecalc(player);
            broadcastSync(server, player.getUuid(), false, null, 0f, 0f, null);
            return;
        }

        // Установить морф
        MorphServerState.set(player, typeOrNull);
        safeRecalc(player);

        // Если есть runtime‑размеры для этого игрока и этого типа — разошлём их
        var runtime = MorphSizeLookupServer.get(player.getUuid());
        boolean hasSize = runtime != null && typeOrNull.equals(runtime.morphId);
        float w = hasSize ? runtime.width : 0f;
        float h = hasSize ? runtime.height : 0f;
        Float eye = hasSize ? runtime.eyeHeight : null;

        broadcastSync(server, player.getUuid(), true, typeOrNull, w, h, eye);
    }

    // ================== Внутренняя логика ==================

    private static void handleMorphWithSize(MinecraftServer server, ServerPlayerEntity player, Identifier id, float w, float h, Float eye) {
        if (!UNSAFE_ALLOW_CLIENT_SIZES) {
            System.out.println("[SequencerMod] C2S morph_with_size rejected (unsafe disabled)");
            return;
        }

        // Сброс
        if (id == null || ("minecraft".equals(id.getNamespace()) && "air".equals(id.getPath()))) {
            MorphSizeLookupServer.clear(player.getUuid());
            applyOrClear(server, player, id, null);
            return;
        }

        // Whitelist (если непустой)
        if (!SIZE_WHITELIST.isEmpty() && !SIZE_WHITELIST.contains(id)) {
            System.out.println("[SequencerMod] morph_with_size rejected (not whitelisted): " + id);
            return;
        }

        // Клампы
        float cw = clamp(w, 0.2f, 4.0f);
        float ch = clamp(h, 0.2f, 6.0f);
        Float ce = eye != null ? clamp(eye, 0.1f, 6.0f) : null;

        MorphSizeLookupServer.set(player.getUuid(), id, cw, ch, ce);
        applyOrClear(server, player, id, new float[]{cw, ch, ce != null ? ce : -1f});
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    // typeOnlySizes=null -> обычный путь; иначе — включаем hasSize=true при синхронизации
    private static void applyOrClear(MinecraftServer server, ServerPlayerEntity player, Identifier req, float[] typeOnlySizes) {
        boolean clear = req == null || ("minecraft".equals(req.getNamespace()) && "air".equals(req.getPath()));
        System.out.println("[SequencerMod] C2S morph request from " + player.getEntityName() + ": " + (clear ? "CLEAR" : req));

        if (clear) {
            MorphServerState.clear(player);
            MorphSizeLookupServer.clear(player.getUuid());
            safeRecalc(player);
            broadcastSync(server, player.getUuid(), false, null, 0f, 0f, null);
            return;
        }

        MorphServerState.set(player, req);
        safeRecalc(player);

        Float eye = null; float w = 0f, h = 0f;
        boolean hasSize = false;
        if (typeOnlySizes != null) {
            hasSize = true;
            w = typeOnlySizes[0];
            h = typeOnlySizes[1];
            eye = (typeOnlySizes[2] >= 0f) ? typeOnlySizes[2] : null;
        } else {
            var runtime = MorphSizeLookupServer.get(player.getUuid());
            if (runtime != null && req.equals(runtime.morphId)) {
                hasSize = true;
                w = runtime.width;
                h = runtime.height;
                eye = runtime.eyeHeight;
            }
        }

        broadcastSync(server, player.getUuid(), true, req, w, h, eye);
    }

    private static void safeRecalc(ServerPlayerEntity p) {
        try { p.calculateDimensions(); } catch (Throwable ignored) {}
    }

    // ================== S2C Broadcast ==================

    private static void broadcastSync(MinecraftServer server, UUID who, boolean active, Identifier type, float w, float h, Float eye) {
        for (ServerPlayerEntity sp : server.getPlayerManager().getPlayerList()) {
            sendSyncTo(sp, who, active, type, w, h, eye);
        }
    }

    private static void sendSyncTo(ServerPlayerEntity target, UUID who, boolean active, Identifier type, float w, float h, Float eye) {
        PacketByteBuf out = PacketByteBufs.create();
        out.writeUuid(who);
        out.writeBoolean(active);
        if (active) {
            out.writeIdentifier(type);
            boolean hasSize = (w > 0f && h > 0f) || eye != null;
            out.writeBoolean(hasSize);
            if (hasSize) {
                out.writeFloat(w);
                out.writeFloat(h);
                out.writeBoolean(eye != null);
                if (eye != null) out.writeFloat(eye);
            }
        }
        ServerPlayNetworking.send(target, MorphPackets.S2C_MORPH_SYNC, out);
    }
}