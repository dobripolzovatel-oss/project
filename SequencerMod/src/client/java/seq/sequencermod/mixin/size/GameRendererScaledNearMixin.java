package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.size.config.MicroRenderConfig;

/**
 * Динамический near/far в первом лице с учётом минимума глубины по 9 лучам (центр + 4 угла + 4 середины рёбер).
 * - Рэйкаст стартует на небольшом смещении позади камеры, чтобы гарантировать пересечение ближайшей грани.
 * - Ограничение far/near корректно соблюдается: при необходимости понижаем far и/или повышаем near.
 */
@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererScaledNearMixin {

    @Inject(method = "getBasicProjectionMatrix(D)Lorg/joml/Matrix4f;", at = @At("HEAD"), cancellable = true)
    private void sequencer$customProjection(double fovDegOriginal, CallbackInfoReturnable<Matrix4f> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null || mc.getWindow() == null) return;

        // Только 1-е лицо
        Perspective persp = mc.options != null ? mc.options.getPerspective() : Perspective.FIRST_PERSON;
        if (persp == null || !persp.isFirstPerson()) return;

        PlayerEntity p = mc.player;
        if (p == null) return;

        // Буфер
        int fbw = Math.max(1, mc.getWindow().getFramebufferWidth());
        int fbh = Math.max(1, mc.getWindow().getFramebufferHeight());
        float aspect = (float) fbw / (float) fbh;
        if (!(aspect > 0.0f) || Float.isInfinite(aspect) || Float.isNaN(aspect)) return;

        // Текущий AABB игрока
        Box bb = p.getBoundingBox();
        double w = Math.max(1.0e-12, bb.getXLength());
        double h = Math.max(1.0e-12, bb.getYLength());
        double d = Math.max(1.0e-12, bb.getZLength());
        double minHalfExtent = 0.5 * Math.min(w, Math.min(h, d));
        float sizeH = (float) h;

        // Базовые клампы
        final float NEAR_ABS_MIN  = 1.0e-6f;
        final float NEAR_SOFT_MIN = 5.0e-5f;
        final float NEAR_MAX      = 0.0050f;

        // База от размера (если ничего близко, держим мягкий минимум)
        float nearDesired = (float) (minHalfExtent * 0.50);
        float near = Math.max(NEAR_SOFT_MIN, Math.min(NEAR_MAX, nearDesired));

        // Коррекция near по минимальной дистанции в пределах текущего FOV
        if (sizeH < 0.06f) {
            try {
                Camera cam = ((GameRenderer) (Object) this).getCamera();
                if (cam != null) {
                    final double fovRad = Math.toRadians(fovDegOriginal);
                    final double halfVFov = 0.5 * fovRad;
                    final double halfHFov = Math.atan(Math.tan(halfVFov) * aspect);

                    // 9 направлений: центр, 4 угла, 4 ребра
                    double[] pitchOff = new double[] {
                            0.0,
                            +halfVFov, +halfVFov, -halfVFov, -halfVFov, // углы
                            +halfVFov, -halfVFov, 0.0,        0.0       // рёбра: вверх/вниз/лево/право
                    };
                    double[] yawOff = new double[] {
                            0.0,
                            -halfHFov, +halfHFov, -halfHFov, +halfHFov, // углы
                            0.0,        0.0,        -halfHFov, +halfHFov // рёбра
                    };

                    final Vec3d eye0 = cam.getPos();
                    final double basePitch = cam.getPitch();
                    final double baseYaw   = cam.getYaw();
                    final double maxProbe  = 1.0;     // до 1 блока вперёд
                    final double backEps   = 1.0e-3;  // отступ назад, чтобы не начинать "в плоскости"

                    double minHit = Double.POSITIVE_INFINITY;

                    for (int i = 0; i < pitchOff.length; i++) {
                        final double pitchDeg = basePitch + Math.toDegrees(pitchOff[i]);
                        final double yawDeg   = baseYaw   + Math.toDegrees(yawOff[i]);

                        Vec3d dir = Vec3d.fromPolar((float) pitchDeg, (float) yawDeg).normalize();
                        Vec3d start = eye0.subtract(dir.multiply(backEps)); // сместили старт назад
                        Vec3d end   = start.add(dir.multiply(maxProbe + backEps));

                        HitResult hit = mc.world.raycast(new RaycastContext(
                                start, end,
                                RaycastContext.ShapeType.COLLIDER,
                                RaycastContext.FluidHandling.NONE,
                                p
                        ));
                        if (hit != null && hit.getType() != HitResult.Type.MISS) {
                            double dist = Math.max(0.0, hit.getPos().distanceTo(eye0)); // измеряем от реального глаза
                            if (dist > 0.0 && dist < minHit) minHit = dist;
                        }
                    }

                    if (minHit != Double.POSITIVE_INFINITY) {
                        // Берём 60% от минимальной глубины в фрустуме — более безопасно у самых малых масштабов
                        float nearFromFrustum = (float) Math.max(NEAR_ABS_MIN, minHit * 0.60);
                        near = Math.max(NEAR_ABS_MIN, Math.min(NEAR_MAX, Math.min(near, nearFromFrustum)));
                    }
                }
            } catch (Throwable ignored) {
                // Оставляем базовый near
            }
        }

        // База far
        float farBase =
                (sizeH < 0.005f) ? MicroRenderConfig.FAR_CLIP_MICRO :
                        (sizeH < MicroRenderConfig.TINY_THRESHOLD) ? MicroRenderConfig.FAR_CLIP_TINY :
                                MicroRenderConfig.FAR_CLIP;

        // Минимальный допустимый far (для стабильности глубины)
        final float FAR_MIN = 32.0f;

        float far = Math.max(FAR_MIN, Math.max(near * 4.0f, farBase));

        // Корректное соблюдение соотношения far/near
        float maxRatio = Math.max(10_000f, MicroRenderConfig.FAR_NEAR_MAX_RATIO);

        // Сначала пробуем понизить far, чтобы уложиться в отношение
        float farByRatio = near * maxRatio;
        if (far > farByRatio) {
            far = Math.max(FAR_MIN, farByRatio);
        }
        // Если всё ещё нарушено (например, упёрлись в FAR_MIN), поднимем near
        if (far / near > maxRatio) {
            near = Math.max(near, far / maxRatio);
            // И пересчитаем страхующие клампы
            near = Math.max(NEAR_ABS_MIN, Math.min(NEAR_MAX, near));
        }

        // Гарантия строгого неравенства
        if (!(far > near)) {
            far = near + Math.max(0.01f, near);
        }

        float fovRad = (float) Math.toRadians(fovDegOriginal);
        if (!(fovRad > 0f) || fovRad >= Math.PI) return;

        try {
            Matrix4f proj = new Matrix4f().setPerspective(fovRad, aspect, near, far);
            cir.setReturnValue(proj);
        } catch (Throwable t) {
            // Надёжный фолбэк
            float fbNear = 0.05f;
            float fbFar =
                    (sizeH < 0.005f) ? Math.max(256f, MicroRenderConfig.FAR_CLIP_MICRO) :
                            (sizeH < MicroRenderConfig.TINY_THRESHOLD) ? Math.max(512f, MicroRenderConfig.FAR_CLIP_TINY) :
                                    Math.max(1024f, MicroRenderConfig.FAR_CLIP);
            if (fbFar <= fbNear) fbFar = fbNear + 1.0f;
            Matrix4f proj = new Matrix4f().setPerspective(fovRad, aspect, fbNear, fbFar);
            cir.setReturnValue(proj);
        }
    }
}