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
 * Динамический near/far в первом лице с учётом минимума глубины по пирамиде из 5 лучей (центр + 4 угла FOV).
 * Это убирает срезы земли/блоков внизу и по краям кадра при экстремально малых размерах.
 */
@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererScaledNearMixin {

    @Inject(method = "getBasicProjectionMatrix(D)Lorg/joml/Matrix4f;", at = @At("HEAD"), cancellable = true)
    private void sequencer$customProjection(double fovDegOriginal, CallbackInfoReturnable<Matrix4f> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null || mc.getWindow() == null) return;

        Perspective persp = mc.options != null ? mc.options.getPerspective() : Perspective.FIRST_PERSON;
        if (persp == null || !persp.isFirstPerson()) return;

        PlayerEntity p = mc.player;
        if (p == null) return;

        int fbw = Math.max(1, mc.getWindow().getFramebufferWidth());
        int fbh = Math.max(1, mc.getWindow().getFramebufferHeight());
        float aspect = (float) fbw / (float) fbh;
        if (!(aspect > 0.0f) || Float.isInfinite(aspect) || Float.isNaN(aspect)) return;

        Box bb = p.getBoundingBox();
        double w = Math.max(1.0e-12, bb.getXLength());
        double h = Math.max(1.0e-12, bb.getYLength());
        double d = Math.max(1.0e-12, bb.getZLength());
        double minHalfExtent = 0.5 * Math.min(w, Math.min(h, d));
        float sizeH = (float) h;

        final float NEAR_ABS_MIN = 1.0e-6f;
        final float NEAR_SOFT_MIN = 5.0e-5f;
        final float NEAR_MAX = 0.0050f;

        float nearDesired = (float) (minHalfExtent * 0.50);
        float near = Math.max(NEAR_SOFT_MIN, Math.min(NEAR_MAX, nearDesired));

        // Коррекция near по минимальной глубине среди 5 лучей в пределах текущего FOV
        // Делаем это только для tiny/микро, чтобы не тратить лишние лучи на нормальных размерах
        if (sizeH < 0.06f) {
            try {
                Camera cam = ((GameRenderer) (Object) this).getCamera();
                if (cam != null) {
                    double fovRad = Math.toRadians(fovDegOriginal);
                    double halfVFov = 0.5 * fovRad;
                    double halfHFov = Math.atan(Math.tan(halfVFov) * aspect);

                    // Центр + 4 угла
                    double[] pitchOffsets = new double[] { 0.0,  +halfVFov, +halfVFov, -halfVFov, -halfVFov };
                    double[] yawOffsets   = new double[] { 0.0,  -halfHFov, +halfHFov, -halfHFov, +halfHFov };

                    Vec3d eye = cam.getPos();
                    double basePitchDeg = cam.getPitch();
                    double baseYawDeg   = cam.getYaw();
                    double maxProbe = 0.6; // до 0.6 блока — хватает для «упора носом» и низа экрана

                    double minHit = Double.POSITIVE_INFINITY;

                    for (int i = 0; i < pitchOffsets.length; i++) {
                        double pitchDeg = basePitchDeg + Math.toDegrees(pitchOffsets[i]);
                        double yawDeg   = baseYawDeg   + Math.toDegrees(yawOffsets[i]);

                        Vec3d dir = Vec3d.fromPolar((float) pitchDeg, (float) yawDeg);
                        Vec3d end = eye.add(dir.multiply(maxProbe));

                        HitResult hit = mc.world.raycast(new RaycastContext(
                                eye, end,
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
                        float nearFromFrustum = (float) Math.max(NEAR_ABS_MIN, minHit * 0.80);
                        near = Math.max(NEAR_ABS_MIN, Math.min(NEAR_MAX, Math.min(near, nearFromFrustum)));
                    }
                }
            } catch (Throwable ignored) {
                // При любой аномалии остаёмся на базовом near
            }
        }

        // far: базовый + ограничение отношения + жёсткий минимум
        float farBase =
                (sizeH < 0.005f) ? MicroRenderConfig.FAR_CLIP_MICRO :
                        (sizeH < MicroRenderConfig.TINY_THRESHOLD) ? MicroRenderConfig.FAR_CLIP_TINY :
                                MicroRenderConfig.FAR_CLIP;

        float far = Math.max(64.0f, Math.max(near * 4.0f, farBase));

        float maxRatio = Math.max(10_000f, MicroRenderConfig.FAR_NEAR_MAX_RATIO);
        if (far / near > maxRatio) {
            far = Math.max(near * maxRatio, 64.0f);
        }
        if (!(far > near)) {
            far = near + Math.max(0.01f, near);
        }

        float fovRad = (float) Math.toRadians(fovDegOriginal);
        if (!(fovRad > 0f) || fovRad >= Math.PI) return;

        try {
            Matrix4f proj = new Matrix4f().setPerspective(fovRad, aspect, near, far);
            cir.setReturnValue(proj);
        } catch (Throwable t) {
            // Фолбэк на «ванильные» значения
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