package seq.sequencermod.size.util;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import seq.sequencermod.size.PlayerSizeData;

public final class SizeCalc {
    private SizeCalc(){}

    public static final float EPS = 0.00001f;
    private static final float TOP_MARGIN = EPS;

    // Авто eye с микро-диапазоном (55..95% высоты) для h<0.05
    public static float resolveEye(PlayerSizeData d, EntityPose pose, EntityDimensions dims) {
        float h = Math.max(EPS, dims.height);

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
                if (minClear > h - TOP_MARGIN) minClear = Math.max(0.02f, h * 0.5f);
                if (eye < minClear) eye = minClear;
                if (eye > h - TOP_MARGIN) eye = h - TOP_MARGIN;
            }
        }
        if (eye > h) eye = h - TOP_MARGIN;
        if (eye < EPS) eye = EPS;

        // Анти‑граница: отводим глаз на 1 ULP вверх, чтобы не лежал ровно на плоскости блока
        // (пример: eye=0.050000f → isInsideWall мог срабатывать). Это микроскопический шаг для float.
        eye = Math.min(h - TOP_MARGIN, Math.nextUp(eye));

        return eye;
    }

    private static float clampEyeManual(float eye, float h) {
        if (h < 0.05f) {
            float minFrac = 0.55f, maxFrac = 0.95f;
            float min = h * minFrac, max = h * maxFrac;
            if (eye < min) eye = min;
            if (eye > max) eye = max;
        } else {
            float minClear = Math.max(0.02f, h * 0.45f);
            if (minClear > h - TOP_MARGIN) minClear = Math.max(0.02f, h * 0.5f);
            if (eye < minClear) eye = minClear;
            if (eye > h - TOP_MARGIN) eye = h - TOP_MARGIN;
        }

        // Тот же анти‑граница для ручного глаза
        eye = Math.min(h - TOP_MARGIN, Math.nextUp(Math.max(EPS, eye)));
        return eye;
    }

    public static float resolveScale(PlayerSizeData d) {
        if (d.modelScale != null && d.manualScale) return Math.max(EPS, d.modelScale);
        return Math.max(EPS, d.height / 1.8f);
    }
}