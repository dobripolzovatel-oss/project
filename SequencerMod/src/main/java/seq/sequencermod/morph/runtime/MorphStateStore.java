package seq.sequencermod.morph.runtime;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Серверное хранилище текущих морфов игроков.
 * Ключ — UUID игрока, значение — строковый id сущности (например, "minecraft:evoker_fangs").
 */
public final class MorphStateStore {
    private static final Map<UUID, String> ACTIVE = new ConcurrentHashMap<>();

    private MorphStateStore() {}

    public static void set(UUID playerUuid, String entityId) {
        if (entityId == null || entityId.isEmpty()) {
            ACTIVE.remove(playerUuid);
        } else {
            ACTIVE.put(playerUuid, entityId);
        }
    }

    public static String get(UUID playerUuid) {
        return ACTIVE.get(playerUuid);
    }

    public static void clear(UUID playerUuid) {
        ACTIVE.remove(playerUuid);
    }

    public static boolean has(UUID playerUuid) {
        return ACTIVE.containsKey(playerUuid);
    }
}