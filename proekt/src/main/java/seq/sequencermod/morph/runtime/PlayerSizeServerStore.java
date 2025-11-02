package seq.sequencermod.morph.runtime;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerSizeServerStore {
    public static final class Entry {
        public final float width;
        public final float height;
        public final Float eyeHeight; // может быть null

        public Entry(float width, float height, Float eyeHeight) {
            this.width = width;
            this.height = height;
            this.eyeHeight = eyeHeight;
        }
    }

    private static final ConcurrentHashMap<UUID, Entry> MAP = new ConcurrentHashMap<>();

    private PlayerSizeServerStore() {}

    public static void set(UUID player, float w, float h, Float eye) {
        if (player == null) return;
        MAP.put(player, new Entry(w, h, eye));
    }

    public static void clear(UUID player) {
        if (player == null) return;
        MAP.remove(player);
    }

    public static Entry get(UUID player) {
        if (player == null) return null;
        return MAP.get(player);
    }

    public static void invalidate() {
        MAP.clear();
    }
}