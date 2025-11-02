package seq.sequencermod.net.client.morphs.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Клиентский lookup размеров и высоты глаз для морфов.
 * - Использует базовые размеры EntityType, а при наличии мира создаёт временный инстанс для более точного значения.
 * - Кэширует результаты.
 */
@Environment(EnvType.CLIENT)
public final class MorphSizeLookup {
    private MorphSizeLookup() {}

    private static final Map<Identifier, EntityDimensions> DIMS_CACHE = new ConcurrentHashMap<>();
    private static final Map<Identifier, Float> EYE_CACHE = new ConcurrentHashMap<>();

    /**
     * Универсальные размеры для клиента (по умолчанию STANDING).
     * Примечание: ваш EntityDimensionsMixin использует это без учёта позы — ок для большинства мобов.
     */
    public static EntityDimensions getDimensions(Identifier typeId) {
        if (typeId == null) return null;
        EntityDimensions cached = DIMS_CACHE.get(typeId);
        if (cached != null) return cached;

        EntityType<?> et = Registries.ENTITY_TYPE.get(typeId);
        if (et == null) return null;

        EntityDimensions dims = null;

        // Попытаемся получить более точные размеры через временный объект в клиентском мире
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.world != null) {
            try {
                Entity e = et.create(mc.world);
                if (e != null) {
                    dims = e.getDimensions(EntityPose.STANDING);
                }
            } catch (Throwable ignored) {}
        }

        if (dims == null) {
            try { dims = et.getDimensions(); } catch (Throwable ignored) {}
        }

        if (dims != null) DIMS_CACHE.put(typeId, dims);
        return dims;
    }

    /**
     * Оценка высоты глаз для клиента. Для LivingEntity берём их standing eye height.
     * Для прочих — 85% от высоты хитбокса, как приемлемый визуальный компромисс.
     */
    public static float getEyeHeight(Identifier typeId) {
        if (typeId == null) return 1.62f; // дефолт игрока, как безопасный fallback
        Float cached = EYE_CACHE.get(typeId);
        if (cached != null) return cached;

        float eye = 1.62f; // fallback

        EntityType<?> et = Registries.ENTITY_TYPE.get(typeId);
        if (et == null) return eye;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.world != null) {
            try {
                Entity e = et.create(mc.world);
                if (e instanceof LivingEntity le) {
                    eye = Math.max(0.1f, le.getStandingEyeHeight());
                } else {
                    // не-Living — пропорция от высоты хитбокса
                    EntityDimensions dims = e != null ? e.getDimensions(EntityPose.STANDING) : et.getDimensions();
                    if (dims != null) eye = Math.max(0.1f, dims.height * 0.85f);
                }
            } catch (Throwable ignored) {
                // fallback ниже
            }
        }

        if (mc == null || mc.world == null) {
            try {
                EntityDimensions dims = et.getDimensions();
                if (dims != null) eye = Math.max(0.1f, dims.height * 0.85f);
            } catch (Throwable ignored) {}
        }

        EYE_CACHE.put(typeId, eye);
        return eye;
    }

    public static void clear() {
        DIMS_CACHE.clear();
        EYE_CACHE.clear();
    }
}