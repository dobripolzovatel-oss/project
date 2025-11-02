package seq.sequencermod.server.overlay;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Хранит активные оверлеи игроков между перезапусками сервера.
 */
public final class OverlayPersistentState extends PersistentState {
    public static final String PERSISTENT_ID = "sequencer_overlays";

    private final Map<UUID, Identifier> overlays = new HashMap<>();

    public OverlayPersistentState() {}

    public static OverlayPersistentState fromNbt(NbtCompound nbt) {
        OverlayPersistentState state = new OverlayPersistentState();
        NbtCompound map = nbt.getCompound("overlays");
        for (String key : map.getKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                Identifier id = new Identifier(map.getString(key));
                state.overlays.put(uuid, id);
            } catch (Exception ignored) {}
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound map = new NbtCompound();
        for (Map.Entry<UUID, Identifier> e : overlays.entrySet()) {
            map.putString(e.getKey().toString(), e.getValue().toString());
        }
        nbt.put("overlays", map);
        return nbt;
    }

    public Map<UUID, Identifier> view() {
        return Collections.unmodifiableMap(overlays);
    }

    public Identifier get(UUID uuid) {
        return overlays.get(uuid);
    }

    public void set(UUID uuid, Identifier id) {
        if (id == null) {
            overlays.remove(uuid);
        } else {
            overlays.put(uuid, id);
        }
        markDirty();
    }

    public void remove(UUID uuid) {
        overlays.remove(uuid);
        markDirty();
    }
}