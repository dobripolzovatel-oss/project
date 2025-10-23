package seq.sequencermod.morph.runtime;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.Identifier;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MorphDimensionsRegistry {
    private static final Map<Identifier, EnumMap<EntityPose, EntityDimensions>> POSE_MAP = new ConcurrentHashMap<>();
    private static final Map<Identifier, EntityDimensions> DEFAULT_MAP = new ConcurrentHashMap<>();

    private MorphDimensionsRegistry() {}

    public static void clear() {
        POSE_MAP.clear();
        DEFAULT_MAP.clear();
    }

    public static void putDefault(Identifier typeId, EntityDimensions dims) {
        if (typeId != null && dims != null) {
            DEFAULT_MAP.put(typeId, dims);
        }
    }

    public static void putPose(Identifier typeId, EntityPose pose, EntityDimensions dims) {
        if (typeId == null || pose == null || dims == null) return;
        POSE_MAP.computeIfAbsent(typeId, k -> new EnumMap<>(EntityPose.class)).put(pose, dims);
    }

    public static EntityDimensions get(Identifier typeId, EntityPose pose) {
        if (typeId == null) return null;
        EnumMap<EntityPose, EntityDimensions> map = POSE_MAP.get(typeId);
        if (map != null && pose != null) {
            EntityDimensions dims = map.get(pose);
            if (dims != null) return dims;
        }
        return DEFAULT_MAP.get(typeId);
    }
}