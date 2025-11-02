package seq.sequencermod.morph;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import seq.sequencermod.morph.runtime.MorphSizeLookupServer;

public final class MorphResolver {
    private MorphResolver() {}

    public static final Identifier AIR = new Identifier("minecraft", "air");
    private static final float DEFAULT_EYE_HEIGHT_RATIO = 0.9f;

    // Вычисление размеров хитбокса и высоты глаз для текущего морфа
    public static @Nullable Resolved resolve(PlayerEntity player, EntityPose pose, @Nullable Identifier morphId) {
        if (morphId == null || AIR.equals(morphId)) return null;

        // 1) Вариант B: серверный runtime override (пришёл от клиента; небезопасно)
        if (player != null) {
            var entry = MorphSizeLookupServer.get(player.getUuid());
            if (entry != null && morphId.equals(entry.morphId)) {
                EntityDimensions dims = EntityDimensions.changing(entry.width, entry.height);
                Float eye = entry.eyeHeight != null ? entry.eyeHeight : dims.height * DEFAULT_EYE_HEIGHT_RATIO;
                return new Resolved(dims, eye);
            }
        }

        // 2) Иначе — ванильные размеры
        EntityDimensions base = dimsOf(morphId);
        if (base == null) return null;

        float eye = base.height * DEFAULT_EYE_HEIGHT_RATIO;
        return new Resolved(base, eye);
    }

    private static @Nullable EntityDimensions dimsOf(Identifier entityId) {
        try {
            EntityType<?> type = Registries.ENTITY_TYPE.get(entityId);
            return type == null ? null : type.getDimensions(); // стандартные размеры "стоя"
        } catch (Exception e) {
            System.err.println("[SequencerMod] Unknown entity id for morph: " + entityId + " (" + e + ")");
            return null;
        }
    }

    public static final class Resolved {
        public final EntityDimensions dimensions;
        public final @Nullable Float eyeHeightOverride;

        public Resolved(EntityDimensions dimensions, @Nullable Float eyeHeightOverride) {
            this.dimensions = dimensions;
            this.eyeHeightOverride = eyeHeightOverride;
        }
    }
}