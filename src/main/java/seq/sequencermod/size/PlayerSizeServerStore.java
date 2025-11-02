package seq.sequencermod.size;

import java.util.*;

public final class PlayerSizeServerStore {
    private static final Map<UUID, PlayerSizeData> MAP = new HashMap<>();

    private PlayerSizeServerStore(){}

    public static void set(UUID id, PlayerSizeData data) {
        MAP.put(id, data);
    }

    public static PlayerSizeData get(UUID id) {
        return MAP.get(id);
    }

    public static void clear(UUID id) {
        MAP.remove(id);
    }

    public static Collection<Map.Entry<UUID,PlayerSizeData>> all() {
        return MAP.entrySet();
    }

    public static boolean has(UUID id) {
        return MAP.containsKey(id);
    }
}