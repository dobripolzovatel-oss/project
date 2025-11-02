package seq.sequencermod.morph;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Серверные (и общие) переопределения размеров для некоторых мобов.
 * Ключ — minecraft:<entity>.
 * Если записи нет — используем ванильные размеры EntityType.getDimensions().
 */
public final class MorphSizeOverrides {
    public static final class Override {
        public final float width;
        public final float height;
        public final Float eyeHeight; // можно null => считать по ratio

        public Override(float width, float height, Float eyeHeight) {
            this.width = width;
            this.height = height;
            this.eyeHeight = eyeHeight;
        }

        public EntityDimensions toDimensions() {
            return EntityDimensions.changing(width, height);
        }
    }

    private static final Map<Identifier, Override> MAP = new HashMap<>();

    static {
        // Пример: allay — более компактный хитбокс и фиксированная высота глаз
        MAP.put(new Identifier("minecraft", "allay"), new Override(0.35f, 0.60f, 0.40f));
        // Примеры на будущее:
        // MAP.put(new Identifier("minecraft", "pig"), new Override(0.9f, 0.9f, null));
        // MAP.put(new Identifier("minecraft", "chicken"), new Override(0.4f, 0.7f, 0.35f));
    }

    public static Override get(Identifier id) { return id == null ? null : MAP.get(id); }

    // По желанию: публичный API для регистрации в рантайме
    public static void register(Identifier id, float width, float height, Float eyeHeight) {
        MAP.put(id, new Override(width, height, eyeHeight));
    }

    private MorphSizeOverrides() {}
}