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
 * Проекция 1-го лица с анти‑клиппингом у стены:
 * - near вычисляем из минимума глубины по 13 лучам внутри текущего FOV (центр, углы, рёбра, полу‑углы),
 *   стартуя лучи с маленьким смещением назад от глаза, чтобы гарантировать пересечение грани.
 * - В микро‑режиме понижаем far до near*MAX_RATIO (вплоть до очень малого far), чтобы не приходилось
 *   поднимать near из‑за ограничения отношения — это устраняет «уголки» и «просветы».
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
        final float NEAR_ABS_MIN  = 1.0e-7f; // разрешим ещё ниже в экстремуме
        final float NEAR_SOFT_MIN = 5.0e-5f;
        final float NEAR_MAX      = 0.0050f;

        // База от размера
        float nearDesired = (float) (minHalfExtent * 0.50);
        float near = Math.max(NEAR_SOFT_MIN, Math.min(NEAR_MAX, nearDesired));

        // Плотная выборка внутри фрустума: 13 лучей
        if (sizeH < 0.10f) {
            try {
                Camera cam = ((GameRenderer) (Object) this).getCamera();
                if (cam != null) {
                    final double fovRad = Math.toRadians(fovDegOriginal);
                    final double halfVFov = 0.5 * fovRad;
                    final double halfHFov = Math.atan(Math.tan(halfVFov) * aspect);

                    // Масштабы смещений по углам/рёбрам/полу‑углам
                    // 0 — центр, ±1 — край FOV, ±0.5 — полу‑углы/полу‑рёбра
                    double[] pitchScale = new double[] {
                            0.0,  +1, +1, -1, -1,   +1, -1,   0,  0,    +0.5, +0.5, -0.5, -0.5
                    };
                    double[] yawScale   = new double[] {
                            0.0,  -1, +1, -1, +1,    0,  0,   -1, +1,   -0.5, +0.5, -0.5, +0.5
                    };

                    final Vec3d eye = cam.getPos();
                    final double basePitch = cam.getPitch();
                    final double baseYaw   = cam.getYaw();

                    final double maxProbe = 1.25;   // до ~1.25 блока
                    final double backEps  = 1.0e-3; // старт чуть позади глаза

                    double minHit = Double.POSITIVE_INFINITY;

                    for (int i = 0; i < pitchScale.length; i++) {
                        double pitchDeg = basePitch + Math.toDegrees(halfVFov * pitchScale[i]);
                        double yawDeg   = baseYaw   + Math.toDegrees(halfHFov * yawScale[i]);

                        Vec3d dir = Vec3d.fromPolar((float) pitchDeg, (float) yawDeg).normalize();
                        Vec3d start = eye.subtract(dir.multiply(backEps));
                        Vec3d end   = start.add(dir.multiply(maxProbe + backEps));

                        HitResult hit = mc.world.raycast(new RaycastContext(
                                start, end,
                                RaycastContext.ShapeType.COLLIDER,
                                RaycastContext.FluidHandling.NONE,
                                p
                        ));
                        if (hit != null && hit.getType() != HitResult.Type.MISS) {
                            double dist = Math.max(0.0, hit.getPos().distanceTo(eye));
                            if (dist > 0.0 && dist < minHit) minHit = dist;
                        }
                    }

                    if (minHit != Double.POSITIVE_INFINITY) {
                        // Более агрессивный запас: ~60% → 35% от найденной минимальной глубины
                        float nearFromFrustum = (float) Math.max(NEAR_ABS_MIN, minHit * 0.35);
                        near = Math.max(NEAR_ABS_MIN, Math.min(NEAR_MAX, Math.min(near, nearFromFrustum)));
                    }
                }
            } catch (Throwable ignored) {
                // остаёмся на базовом near
            }
        }

        // База far (как раньше)
        float farBase =
                (sizeH < 0.005f) ? MicroRenderConfig.FAR_CLIP_MICRO :
                        (sizeH < MicroRenderConfig.TINY_THRESHOLD) ? MicroRenderConfig.FAR_CLIP_TINY :
                                MicroRenderConfig.FAR_CLIP;

        // Отношение far/near: в микро‑режиме понижаем far, а не повышаем near
        float maxRatio = Math.max(10_000f, MicroRenderConfig.FAR_NEAR_MAX_RATIO);
        float far = farBase;

        // Если near совсем мал (кадры «упор в стену»), позволим far опуститься
        if (near < 1.0e-4f) {
            // Не больше, чем near*ratio; и не ниже 8 блоков — чтобы мир не рассыпался совсем
            far = Math.min(far, Math.max(8.0f, near * maxRatio));
        } else {
            // Обычно: режем far по отношению, но держим небольшой минимум
            far = Math.max(32.0f, Math.min(far, near * maxRatio));
        }

        // Дополнительная страховка на видимость: far должен быть хотя бы немного дальше near
        if (far <= near) {
            far = Math.max(near + Math.max(0.01f, near * 4.0f), 8.0f);
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