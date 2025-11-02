package seq.sequencermod.size.auto;

import net.minecraft.entity.player.PlayerEntity;
import seq.sequencermod.size.PlayerSizeData;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Потокобезопасное хранилище производных параметров (Derived) по UUID игрока.
 * Временно используем стор, чтобы не лезть в PlayerSizeData.
 */
public final class SizeDerivedStore {
    private static final ConcurrentHashMap<UUID, SizeDerived> MAP = new ConcurrentHashMap<>();

    private SizeDerivedStore() {}

    public static SizeDerived get(UUID id) {
        return MAP.get(id);
    }

    public static void set(UUID id, SizeDerived derived) {
        if (derived == null) {
            MAP.remove(id);
        } else {
            MAP.put(id, derived);
        }
    }

    public static SizeDerived getOrCompute(PlayerEntity p, PlayerSizeData d) {
        if (p == null || d == null) return null;
        return MAP.computeIfAbsent(p.getUuid(), __ -> SizeRules.compute(p, d));
    }

    public static void clear(UUID id) {
        MAP.remove(id);
    }
}