package seq.sequencermod.morph.runtime;

import net.minecraft.util.Identifier;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Серверное хранилище размеров хитбокса, пришедших от клиента (Вариант B, небезопасно).
 * Привязано к конкретному игроку (UUID) и morphId — чтобы не было рассинхрона при смене морфа.
 */
public final class MorphSizeLookupServer {
    public static final class Entry {
        public final Identifier morphId;
        public final float width;
        public final float height;
        public final Float eyeHeight; // может быть null

        public Entry(Identifier morphId, float width, float height, Float eyeHeight) {
            this.morphId = morphId;
            this.width = width;
            this.height = height;
            this.eyeHeight = eyeHeight;
        }
    }

    private static final ConcurrentHashMap<UUID, Entry> BY_PLAYER = new ConcurrentHashMap<>();

    private MorphSizeLookupServer() {}

    public static void set(UUID player, Identifier morphId, float w, float h, Float eye) {
        if (player == null || morphId == null) return;
        BY_PLAYER.put(player, new Entry(morphId, w, h, eye));
    }

    public static void clear(UUID player) {
        if (player == null) return;
        BY_PLAYER.remove(player);
    }

    public static Entry get(UUID player) {
        if (player == null) return null;
        return BY_PLAYER.get(player);
    }

    public static void invalidateCache() {
        BY_PLAYER.clear();
    }
}