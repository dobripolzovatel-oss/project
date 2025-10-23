package seq.sequencermod.size.util;

import net.minecraft.entity.Entity;

/**
 * Единый источник высоты "белого" хитбокса и масштаба.
 *
 * Под "белым" хитбоксом понимаем визуальный/фактический AABB сущности (тот, что рисуется белым в отладке),
 * т. е. берём именно текущий bounding box (Entity#getBoundingBox()).
 *
 * Масштаб s_white = whiteHeight / 1.8f, с нижним клампом на микро-значениях.
 */
public final class WhiteHitboxScale {

    // Базовая ванильная высота игрока стоя
    public static final float BASE_PLAYER_HEIGHT = 1.8f;

    // Защита от нулевых величин (в метрах/блоках)
    public static final float EPS_HEIGHT = 1.0e-9f;

    // Минимальный допустимый масштаб
    public static final float MIN_SCALE = 1.0e-5f;

    private WhiteHitboxScale() {}

    /**
     * Текущая высота "белого" хитбокса (AABB) сущности.
     */
    public static float whiteHeight(Entity e) {
        if (e == null) return EPS_HEIGHT;
        // Используем текущий AABB, а не nominal dimensions:
        double h = e.getBoundingBox().getYLength();
        return (float) Math.max(EPS_HEIGHT, h);
        // При желании можно добавить fallback на e.getDimensions(e.getPose()).height,
        // но AABB — это и есть "белый" ящик.
    }

    /**
     * Масштаб относительно базовой высоты игрока.
     * s_white = whiteHeight / 1.8f, с нижним клампом.
     */
    public static float whiteScale(Entity e) {
        float h = whiteHeight(e);
        return Math.max(MIN_SCALE, h / BASE_PLAYER_HEIGHT);
    }
}