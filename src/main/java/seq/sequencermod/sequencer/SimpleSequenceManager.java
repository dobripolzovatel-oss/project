package seq.sequencermod.sequencer;

import seq.sequencermod.morph.runtime.MorphRuntime;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Простейший менеджер секвенций.
 * Если у тебя есть собственный SequenceRunnerManager — используй его,
 * а этот оставь как пример/заглушку.
 */
public final class SimpleSequenceManager {

    public interface Sequence {
        String id();
        void start(UUID playerUuid); // может дергать MorphRuntime.applyMorph(...)
        void stop(UUID playerUuid);  // может дергать MorphRuntime.clearMorph(...)
    }

    private static final Map<String, Sequence> REGISTRY = new ConcurrentHashMap<>();
    private static final Map<UUID, String> RUNNING = new ConcurrentHashMap<>();

    private SimpleSequenceManager() {}

    public static void register(Sequence seq) {
        REGISTRY.put(seq.id(), seq);
    }

    public static boolean start(UUID playerUuid, String sequenceId) {
        Sequence seq = REGISTRY.get(sequenceId);
        if (seq == null) return false;
        RUNNING.put(playerUuid, sequenceId);
        seq.start(playerUuid);
        return true;
    }

    public static void stop(UUID playerUuid) {
        String sequenceId = RUNNING.remove(playerUuid);
        if (sequenceId == null) return;
        Sequence seq = REGISTRY.get(sequenceId);
        if (seq != null) {
            seq.stop(playerUuid);
        } else {
            // безопасно снять морф, если секвенция исчезла
            MorphRuntime.clearMorph(playerUuid, u -> true);
        }
    }

    public static String runningOf(UUID playerUuid) {
        return RUNNING.get(playerUuid);
    }
}