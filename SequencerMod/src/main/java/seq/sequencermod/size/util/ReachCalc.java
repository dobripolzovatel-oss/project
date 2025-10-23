package seq.sequencermod.size.util;

import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;

public final class ReachCalc {

    private ReachCalc() {}

    public static float scale(PlayerEntity p) {
        if (p == null) return 1.0f;
        float h = Math.max(SizeCalc.EPS, p.getDimensions(p.getPose()).height);
        return Math.max(0.00001f, h / 1.8f);
    }

    // Микро-порог: всё ниже этого считаем "ультра-тине"
    private static boolean isUltraTiny(float s) { return s < 1.0e-4f; }
    private static boolean isMicro(float s)     { return s < 1.0e-3f; }

    // Шаг через препятствия: 0.12 + 0.48*s
    // Клампы: обычный минимум 0.06, но для микро опускаем до 0.02
    public static float stepHeightFor(PlayerEntity p) {
        float s = scale(p);
        float step = 0.12f + 0.48f * s;
        if (p.getPose() == EntityPose.SWIMMING || p.isFallFlying()) step = Math.min(step, 0.2f);
        float minStep = isMicro(s) ? 0.02f : 0.06f;
        return clamp(step, minStep, 0.60f);
    }

    // Дистанция до блоков: 1.5 + 3.0*s
    // Минимум для микро опускаем: 0.25 (micro) и 0.15 (ultra-tiny)
    public static float blockReachFor(PlayerEntity p, boolean creative, float vanillaReach) {
        float s = scale(p);
        float reach = 1.5f + 3.0f * s;
        float minReach = isUltraTiny(s) ? 0.15f : (isMicro(s) ? 0.25f : 0.80f);
        return clamp(reach, minReach, vanillaReach);
    }

    // Дистанция до сущностей: 1.0 + 2.0*s
    // Минимум для микро: 0.20 (micro) и 0.12 (ultra-tiny)
    public static float entityReachFor(PlayerEntity p) {
        float s = scale(p);
        float reach = 1.0f + 2.0f * s;
        float minReach = isUltraTiny(s) ? 0.12f : (isMicro(s) ? 0.20f : 0.60f);
        return clamp(reach, minReach, 3.0f);
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}