package seq.sequencermod.size.util;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import seq.sequencermod.size.PlayerSizeData;

public final class SizeCalc {
    private SizeCalc(){}

    public static final float EPS = 0.00001f;
    // Минимальное положительное значение для высоты глаз (во избежание нуля/отрицательного)
    private static final float MIN_POS = 1.0e-9f;

    // Абсолютный TOP_MARGIN заменяем на относительный, чтобы при h≈EPS не получать h - margin <= 0
    private static float topMargin(float h) {
        // 5% от высоты, но не меньше 0.1*EPS и не больше половины высоты
        float m = Math.max(EPS * 0.1f, h * 0.05f);
        if (m >= h) m = Math.nextAfter(h * 0.5f, 0.0f);
        return m;
    }

    // Авто eye с микро-диапазоном (55..95% высоты) для h<0.05
    public static float resolveEye(PlayerSizeData d, EntityPose pose, EntityDimensions dims) {
        float h = Math.max(EPS, dims.height);
        float margin = topMargin(h);

        float eye;
        if (d.eyeHeight != null && d.manualEye) {
            eye = clampEyeManual(d.eyeHeight, h);
        } else {
            float baseRatio = switch (pose) {
                case CROUCHING -> 1.27f / 1.5f;
                case SWIMMING, FALL_FLYING, SPIN_ATTACK -> 0.4f / 0.6f;
                case SLEEPING -> 0.5f;
                default -> 1.62f / 1.8f; // 0.9 стоя
            };
            eye = h * baseRatio;
            if (h < 0.05f) {
                float minFrac = 0.55f, maxFrac = 0.95f;
                float min = h * minFrac, max = h * maxFrac;
                if (eye < min) eye = min;
                if (eye > max) eye = max;
            } else {
                float minClear = Math.max(0.02f, h * 0.45f);
                if (minClear > h - margin) minClear = Math.max(0.02f, h * 0.5f);
                if (eye < minClear) eye = minClear;
                if (eye > h - margin) eye = h - margin;
            }
        }
        // Верхняя граница — не топ хитбокса, а (h - margin)
        if (eye > h - margin) eye = h - margin;
        if (eye < MIN_POS) eye = MIN_POS;

        // Анти‑граница: двигаем глаз на 1 ULP вверх, но не уменьшаем его и не выходим за (h - margin)
        float nudged = Math.max(eye, Math.nextUp(eye));
        eye = Math.min(h - margin, nudged);
        if (eye < MIN_POS) eye = MIN_POS;

        return eye;
    }

    private static float clampEyeManual(float eye, float h) {
        float margin = topMargin(h);
        if (h < 0.05f) {
            float minFrac = 0.55f, maxFrac = 0.95f;
            float min = h * minFrac, max = h * maxFrac;
            if (eye < min) eye = min;
            if (eye > max) eye = max;
        } else {
            float minClear = Math.max(0.02f, h * 0.45f);
            if (minClear > h - margin) minClear = Math.max(0.02f, h * 0.5f);
            if (eye < minClear) eye = minClear;
            if (eye > h - margin) eye = h - margin;
        }

        // Тот же анти‑граница для ручного глаза
        float base = Math.max(MIN_POS, eye);
        float nudged = Math.max(base, Math.nextUp(base));
        eye = Math.min(h - margin, nudged);
        if (eye < MIN_POS) eye = MIN_POS;
        return eye;
    }

    public static float resolveScale(PlayerSizeData d) {
        if (d.modelScale != null && d.manualScale) return Math.max(EPS, d.modelScale);
        return Math.max(EPS, d.height / 1.8f);
    }
}