package seq.sequencermod.sequencer;

import net.minecraft.server.network.ServerPlayerEntity;
import seq.sequencermod.sequencer.json.SequenceJsonDef;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Регистр loaded-секвенций (из JSON) + управление запуском/остановкой.
 */
public final class SequenceRegistry {

    private static final Map<String, SequenceJsonDef> LOADED = new ConcurrentHashMap<>();
    private static final Map<UUID, String> RUNNING = new ConcurrentHashMap<>();
    private static volatile boolean INITIALIZED = false;

    private SequenceRegistry() {}

    public static void init() {
        if (INITIALIZED) return;
        INITIALIZED = true;
        SequenceScheduler.init();
    }

    public static void setLoaded(Map<String, SequenceJsonDef> map) {
        init();
        LOADED.clear();
        LOADED.putAll(map);
        System.out.println("[sequencermod] Loaded sequences: " + LOADED.keySet());
    }

    public static Set<String> listIds() {
        return new TreeSet<>(LOADED.keySet());
    }

    public static boolean start(UUID playerUuid, String sequenceId) {
        init();
        SequenceJsonDef def = LOADED.get(sequenceId);
        if (def == null) return false;
        boolean ok = SequenceEngine.run(playerUuid, sequenceId, def);
        if (ok) RUNNING.put(playerUuid, sequenceId);
        return ok;
    }

    public static void stop(UUID playerUuid) {
        init();
        RUNNING.remove(playerUuid);
        SequenceEngine.stop(playerUuid);
    }

    public static String runningOf(UUID playerUuid) {
        return RUNNING.get(playerUuid);
    }

    public static boolean isKnown(String id) {
        return LOADED.containsKey(id);
    }

    public static void onPlayerLeft(ServerPlayerEntity player) {
        if (player == null) return;
        stop(player.getUuid());
    }
}