package seq.sequencermod.size.auto;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import seq.sequencermod.size.PlayerSizeData;
import seq.sequencermod.size.config.MicroRenderConfig;
import seq.sequencermod.size.util.SizeCalc;

/** Формулы: из размера -> производные настройки. Все “магические числа” сведены сюда. */
public final class SizeRules {
    private SizeRules(){}

    public static SizeDerived compute(PlayerEntity player, PlayerSizeData d) {
        SizeDerived out = new SizeDerived();

        EntityPose pose = player.getPose();
        EntityDimensions dims = player.getDimensions(pose);

        float scale = SizeCalc.resolveScale(d);
        float h = Math.max(SizeCalc.EPS, dims.height);
        float eye = Math.max(SizeCalc.EPS, SizeCalc.resolveEye(d, pose, dims));

        out.scale = scale;
        out.hitboxHeight = h;
        out.eyeHeight = eye;

        // 1) Динамический far: micro <0.005 → 512; tiny <0.06 → 1024; иначе 4096
        float far;
        if (h < 0.005f) {
            far = Math.min(MicroRenderConfig.FAR_CLIP_MICRO, MicroRenderConfig.FAR_CLIP);
        } else if (h < MicroRenderConfig.TINY_NEAR_THRESHOLD) {
            far = Math.min(MicroRenderConfig.FAR_CLIP_TINY, MicroRenderConfig.FAR_CLIP);
        } else {
            far = MicroRenderConfig.FAR_CLIP;
        }

        // 2) near: клампы зависят от размера (tiny-ветка для h < 0.06)
        float desiredNear = (float) (eye * MicroRenderConfig.EYE_NEAR_FRACTION);

        float nearMin, nearMax;
        if (h < MicroRenderConfig.TINY_NEAR_THRESHOLD) {
            nearMin = MicroRenderConfig.NEAR_MIN_SAFE_TINY;
            nearMax = MicroRenderConfig.NEAR_MAX_SAFE_TINY;
        } else {
            nearMin = MicroRenderConfig.NEAR_MIN_SAFE;
            nearMax = MicroRenderConfig.NEAR_MAX_SAFE;
        }

        float near = Math.max(nearMin, Math.min(nearMax, desiredNear));

        // Ограничение по соотношению (страховка точности depth)
        if (far / near > MicroRenderConfig.FAR_NEAR_MAX_RATIO) {
            near = far / MicroRenderConfig.FAR_NEAR_MAX_RATIO;
        }

        out.near = near;
        out.far  = far;

        // 3) FOV
        if (MicroRenderConfig.SCALE_FOV && scale < 1f) {
            float ratio = Math.max(0f, Math.min(1f, scale / MicroRenderConfig.TINY_THRESHOLD));
            double reduction = (1.0 - ratio) * MicroRenderConfig.MAX_FOV_REDUCTION;
            out.fovMultiplier = (float) (1.0 - reduction);
        } else {
            out.fovMultiplier = 1f;
        }

        // 4) Bobbing
        out.bobbingEnabled = (h >= MicroRenderConfig.BOB_TINY_DISABLE_HEIGHT);
        out.bobMaxHeightFraction = MicroRenderConfig.BOB_MAX_HEIGHT_FRACTION;

        // 5) 3-е лицо
        out.cameraExponent = MicroRenderConfig.CAMERA_EXPONENT;
        out.thirdPersonAbsMin = MicroRenderConfig.MIN_ABSOLUTE_THIRD_PERSON;
        out.thirdPersonMinDistance = Math.max(
                out.thirdPersonAbsMin,
                eye * MicroRenderConfig.THIRD_PERSON_MIN_EYE_FRACTION
        );
        out.thirdPersonClearanceFrac = MicroRenderConfig.THIRD_PERSON_CLEARANCE_FRACTION;
        out.thirdPersonClearanceMin  = MicroRenderConfig.THIRD_PERSON_CLEARANCE_ABS_MIN;
        out.thirdPersonClearanceMax  = MicroRenderConfig.THIRD_PERSON_CLEARANCE_ABS_MAX;

        // 6) 1-е лицо: безопасный зазор над верхом блока
        out.firstPersonClearanceEps = Math.max(SizeCalc.EPS, Math.max(eye * 0.05f, near * 1.5f));

        // 7) Анти-мерцание модели
        out.visualUpscaleEnabled = MicroRenderConfig.ANTIFLICKER_VISUAL_UPSCALE;
        out.minVisualModelScale  = MicroRenderConfig.MIN_VISUAL_MODEL_SCALE;
        out.maxVisualUpscaleFactor = MicroRenderConfig.MAX_UPSCALE_FACTOR;

        // 8) Поведение
        out.disableAutoSwim = (h < MicroRenderConfig.BEHAVIOR_TINY_HEIGHT);

        return out;
    }
}