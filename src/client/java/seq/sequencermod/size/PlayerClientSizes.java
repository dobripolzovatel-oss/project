package seq.sequencermod.size;

import java.util.*;

public final class PlayerClientSizes {
    private static final Map<UUID, PlayerSizeData> MAP = new HashMap<>();

    private PlayerClientSizes(){}

    public static void put(UUID id, PlayerSizeData d) { MAP.put(id, d); }
    public static PlayerSizeData get(UUID id) { return MAP.get(id); }
    public static void remove(UUID id) { MAP.remove(id); }
    public static void clear() { MAP.clear(); }
}