package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.size.PlayerClientSizes;
import seq.sequencermod.size.PlayerSizeData;
import seq.sequencermod.size.config.MicroRenderConfig;

/**
 * Проекцию под tiny/micro включаем ТОЛЬКО при валидных данных размера от сервера
 * и при обнаружении реально близкой геометрии трассировкой лучей.
 * Иначе — ванильная матрица.
 */
@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererScaledNearMixin {

    @Inject(method = "getBasicProjectionMatrix(D)Lorg/joml/Matrix4f;", at = @At("HEAD"), cancellable = true)
    private void sequencer$customProjection(double fovDegOriginal, CallbackInfoReturnable<Matrix4f> cir) {
        if (!MicroRenderConfig.ENABLE_CUSTOM_NEAR) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null || mc.getWindow() == null) return;

        Perspective persp = mc.options != null ? mc.options.getPerspective() : Perspective.FIRST_PERSON;
        if (persp == null || !persp.isFirstPerson()) return;

        PlayerEntity p = mc.player;
        if (p == null) return;

        // ЕДИНЫЙ стор
        PlayerSizeData data = PlayerClientSizes.get(p.getUuid());
        if (data == null || !(data.height > 0.0f)) return;

        float sizeH = data.height;
        if (sizeH >= MicroRenderConfig.TINY_NEAR_THRESHOLD) {
            return; // обычный рост — ваниль
        }

        int fbw = Math.max(1, mc.getWindow().getFramebufferWidth());
        int fbh = Math.max(1, mc.getWindow().getFramebufferHeight());
        float aspect = (float) fbw / (float) fbh;
        if (!(aspect > 0.0f) || Float.isInfinite(aspect) || Float.isNaN(aspect)) return;

        final boolean micro = sizeH < 0.005f;
        final float NEAR_MIN = micro ? 5.0e-5f : MicroRenderConfig.NEAR_MIN_SAFE_TINY;
        final float NEAR_MAX = micro ? 0.0035f : MicroRenderConfig.NEAR_MAX_SAFE_TINY;

        // Лучевая оценка: если ничего близко не нашли — НЕ подменяем
        Float nearOpt = null;
        try {
            Camera cam = ((GameRenderer) (Object) this).getCamera();
            if (cam != null) {
                final double fovRad = Math.toRadians(fovDegOriginal);
                final double halfVFov = 0.5 * fovRad;
                final double halfHFov = Math.atan(Math.tan(halfVFov) * aspect);
                double[] grid = new double[]{-1, -0.5, 0, 0.5, 1};

                Vec3d eye = cam.getPos();
                double basePitch = cam.getPitch();
                double baseYaw   = cam.getYaw();

                final double maxProbe = 1.0;
                final double backEps  = 1.0e-3;

                double minHit = Double.POSITIVE_INFINITY;
                for (double py : grid) for (double px : grid) {
                    double pitchDeg = basePitch + Math.toDegrees(halfVFov * py);
                    double yawDeg   = baseYaw   + Math.toDegrees(halfHFov * px);
                    Vec3d dir = Vec3d.fromPolar((float) pitchDeg, (float) yawDeg).normalize();
                    Vec3d start = eye.subtract(dir.multiply(backEps));
                    Vec3d end   = start.add(dir.multiply(maxProbe + backEps));
                    HitResult hit = mc.world.raycast(new RaycastContext(
                            start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
                    if (hit != null && hit.getType() != HitResult.Type.MISS) {
                        double dist = Math.max(0.0, hit.getPos().distanceTo(eye));
                        if (dist > 0.0 && dist < minHit) minHit = dist;
                    }
                }

                if (minHit != Double.POSITIVE_INFINITY) {
                    float candidate = (float) (minHit * 0.75);
                    nearOpt = Math.max(NEAR_MIN, Math.min(NEAR_MAX, candidate));
                }
            }
        } catch (Throwable ignored) {}

        if (nearOpt == null || !(nearOpt > 0.0f)) return;

        float farBase =
                (sizeH < 0.005f) ? MicroRenderConfig.FAR_CLIP_MICRO :
                        (sizeH < MicroRenderConfig.TINY_THRESHOLD) ? MicroRenderConfig.FAR_CLIP_TINY :
                                MicroRenderConfig.FAR_CLIP;

        // Жёсткие страховки, чтобы мир точно попадал в frustum
        float maxRatio = Math.min(MicroRenderConfig.FAR_NEAR_MAX_RATIO, 500_000f);
        float near = nearOpt;
        float far = Math.max(256.0f, Math.min(farBase, near * maxRatio));
        if (!(far > near)) far = near + Math.max(0.01f, near);

        float fovRad = (float) Math.toRadians(fovDegOriginal);
        if (!(fovRad > 0f) || fovRad >= Math.PI) return;

        try {
            Matrix4f proj = new Matrix4f().setPerspective(fovRad, aspect, near, far);
            cir.setReturnValue(proj);
        } catch (Throwable t) {
            // На всякий случай — ванильный безопасный фолбэк
            float fbNear = 0.05f;
            float fbFar  = Math.max(2048f, farBase);
            if (fbFar <= fbNear) fbFar = fbNear + 1.0f;
            Matrix4f proj = new Matrix4f().setPerspective(fovRad, aspect, fbNear, fbFar);
            cir.setReturnValue(proj);
        }
    }
}