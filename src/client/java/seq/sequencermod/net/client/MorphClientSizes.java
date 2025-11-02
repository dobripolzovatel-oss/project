package seq.sequencermod.net.client;

import net.minecraft.util.Identifier;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Клиентское хранилище размеров, пришедших с сервера в S2C_MORPH_SYNC (вариант B).
 * Привязано к конкретному игроку и morphId.
 */
public final class MorphClientSizes {
    public static final class Entry {
        public final Identifier morphId;
        public final float width;
        public final float height;
        public final Float eyeHeight;

        public Entry(Identifier morphId, float width, float height, Float eyeHeight) {
            this.morphId = morphId;
            this.width = width;
            this.height = height;
            this.eyeHeight = eyeHeight;
        }
    }

    private static final ConcurrentHashMap<UUID, Entry> BY_PLAYER = new ConcurrentHashMap<>();

    private MorphClientSizes() {}

    public static void set(UUID player, Identifier morphId, float w, float h, Float eye) {
        if (player == null || morphId == null) return;
        BY_PLAYER.put(player, new Entry(morphId, w, h, eye));
    }

    public static void clear(UUID player) {
        if (player == null) return;
        BY_PLAYER.remove(player);
    }

    public static Entry get(UUID player) {
        return BY_PLAYER.get(player);
    }
}