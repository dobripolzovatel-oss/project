package seq.sequencermod.net.client;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerClientSizes {
    public static final class Entry {
        public final float width;
        public final float height;
        public final Float eyeHeight;

        public Entry(float width, float height, Float eyeHeight) {
            this.width = width;
            this.height = height;
            this.eyeHeight = eyeHeight;
        }
    }

    private static final ConcurrentHashMap<UUID, Entry> MAP = new ConcurrentHashMap<>();

    private PlayerClientSizes() {}

    public static void set(UUID who, float w, float h, Float eye) {
        MAP.put(who, new Entry(w, h, eye));
    }

    public static void clear(UUID who) {
        if (who == null) return;
        MAP.remove(who);
    }

    public static Entry get(UUID who) {
        return MAP.get(who);
    }

    public static void clearAll() {
        MAP.clear();
    }
}