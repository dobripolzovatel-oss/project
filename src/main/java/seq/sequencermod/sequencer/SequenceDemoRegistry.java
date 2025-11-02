package seq.sequencermod.sequencer;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import seq.sequencermod.core.ServerRef;
import seq.sequencermod.morph.runtime.MorphRuntime;
import seq.sequencermod.morph.runtime.MorphSizeLookupServer;
import seq.sequencermod.server.morph.MorphServer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SequenceDemoRegistry {

    public interface Sequence {
        String id();
        void start(UUID playerUuid);
        void stop(UUID playerUuid);
    }

    private static final Map<String, Sequence> REGISTRY = new ConcurrentHashMap<>();
    private static final Map<UUID, String> RUNNING = new ConcurrentHashMap<>();

    private SequenceDemoRegistry() {}

    public static void registerBuiltins() {
        SequenceScheduler.init();

        register(new Sequence() {
            @Override public String id() { return "fangs"; }
            @Override public void start(UUID playerUuid) {
                RUNNING.put(playerUuid, id());
                var err = MorphRuntime.applyMorph(playerUuid, "minecraft:evoker_fangs", u -> true);
                ServerPlayerEntity p = currentPlayer(serverOf(), playerUuid);
                if (err == null && p != null) {
                    MorphServer.syncToAll(p, new Identifier("minecraft", "evoker_fangs"));
                    p.sendMessage(Text.literal("[seq] morphed: evoker_fangs"), false);
                } else if (p != null) {
                    p.sendMessage(Text.literal("[seq] failed: " + err), false);
                }
            }
            @Override public void stop(UUID playerUuid) {
                doClear(playerUuid);
            }
        });

        register(new Sequence() {
            @Override public String id() { return "zombie_then_spider"; }
            @Override public void start(UUID playerUuid) {
                RUNNING.put(playerUuid, id());
                var err = MorphRuntime.applyMorph(playerUuid, "minecraft:zombie", u -> true);
                ServerPlayerEntity p = currentPlayer(serverOf(), playerUuid);
                if (err == null && p != null) {
                    MorphServer.syncToAll(p, new Identifier("minecraft", "zombie"));
                    p.sendMessage(Text.literal("[seq] step 1: zombie"), false);
                }
                SequenceScheduler.schedule(playerUuid, 60, server -> {
                    if (!id().equals(RUNNING.get(playerUuid))) return;
                    ServerPlayerEntity p2 = currentPlayer(server, playerUuid);
                    if (p2 == null) return;
                    var err2 = MorphRuntime.applyMorph(playerUuid, "minecraft:spider", u -> true);
                    if (err2 == null) {
                        MorphServer.syncToAll(p2, new Identifier("minecraft", "spider"));
                        p2.sendMessage(Text.literal("[seq] step 2: spider"), false);
                    } else {
                        p2.sendMessage(Text.literal("[seq] failed on step 2: " + err2), false);
                    }
                });
            }
            @Override public void stop(UUID playerUuid) {
                doClear(playerUuid);
            }
        });

        register(new Sequence() {
            @Override public String id() { return "demo_basic"; }
            @Override public void start(UUID playerUuid) {
                RUNNING.put(playerUuid, id());
                var err = MorphRuntime.applyMorph(playerUuid, "minecraft:allay", u -> true);
                ServerPlayerEntity p = currentPlayer(serverOf(), playerUuid);
                if (err == null && p != null) {
                    // Вариант B: задаём компактные размеры для allay
                    MorphSizeLookupServer.set(playerUuid, new Identifier("minecraft", "allay"), 0.35f, 0.60f, 0.40f);
                    p.calculateDimensions();
                    MorphServer.syncToAll(p, new Identifier("minecraft", "allay"));
                    p.sendMessage(Text.literal("[seq] allay, will clear in 40 ticks"), false);
                }
                SequenceScheduler.schedule(playerUuid, 40, server -> {
                    if (!id().equals(RUNNING.get(playerUuid))) return;
                    ServerPlayerEntity p2 = currentPlayer(server, playerUuid);
                    if (p2 == null) return;
                    MorphRuntime.clearMorph(playerUuid, u -> true);
                    MorphSizeLookupServer.clear(playerUuid);
                    MorphServer.syncToAll(p2, null);
                    p2.sendMessage(Text.literal("[seq] cleared"), false);
                });
            }
            @Override public void stop(UUID playerUuid) {
                doClear(playerUuid);
            }
        });
    }

    public static void register(Sequence seq) {
        REGISTRY.put(seq.id(), seq);
    }

    public static boolean start(UUID playerUuid, String sequenceId) {
        Sequence seq = REGISTRY.get(sequenceId);
        if (seq == null) return false;
        seq.start(playerUuid);
        return true;
    }

    public static void stop(UUID playerUuid) {
        String id = RUNNING.remove(playerUuid);
        if (id == null) {
            doClear(playerUuid);
            return;
        }
        Sequence seq = REGISTRY.get(id);
        if (seq != null) seq.stop(playerUuid);
        else doClear(playerUuid);
    }

    public static String runningOf(UUID playerUuid) {
        return RUNNING.get(playerUuid);
    }

    private static void doClear(UUID playerUuid) {
        SequenceScheduler.cancelFor(playerUuid);
        MorphRuntime.clearMorph(playerUuid, u -> true);
        ServerPlayerEntity p = currentPlayer(serverOf(), playerUuid);
        if (p != null) {
            MorphSizeLookupServer.clear(playerUuid);
            MorphServer.syncToAll(p, null);
            p.sendMessage(Text.literal("[seq] stopped"), false);
        }
    }

    private static MinecraftServer serverOf() {
        return ServerRef.get();
    }

    private static ServerPlayerEntity currentPlayer(MinecraftServer server, UUID uuid) {
        if (server == null) return null;
        return server.getPlayerManager().getPlayer(uuid);
    }
}