package seq.sequencermod.morph.server;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MorphServerState {
    private MorphServerState() {}

    private static final ConcurrentHashMap<UUID, Identifier> STATE = new ConcurrentHashMap<>();

    public static Identifier get(PlayerEntity p) { return p == null ? null : get(p.getUuid()); }
    public static Identifier get(UUID uuid) { return uuid == null ? null : STATE.get(uuid); }

    public static void set(PlayerEntity p, Identifier morphId) { if (p != null) set(p.getUuid(), morphId); }
    public static void set(UUID uuid, Identifier morphId) {
        if (uuid == null) return;
        if (morphId == null) STATE.remove(uuid);
        else STATE.put(uuid, morphId);
    }

    public static void clear(PlayerEntity p) { if (p != null) STATE.remove(p.getUuid()); }
    public static Map<UUID, Identifier> snapshot() { return Map.copyOf(STATE); }
    public static void clearAll() { STATE.clear(); }
}