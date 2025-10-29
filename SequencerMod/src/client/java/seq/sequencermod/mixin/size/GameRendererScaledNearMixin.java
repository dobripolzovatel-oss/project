package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
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

@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererScaledNearMixin {

    @Inject(method = "getBasicProjectionMatrix(D)Lorg/joml/Matrix4f;", at = @At("HEAD"), cancellable = true)
    private void sequencer$customProjection(double fovDegOriginal, CallbackInfoReturnable<Matrix4f> cir) {
        if (!MicroRenderConfig.ENABLE_CUSTOM_NEAR) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null || mc.getWindow() == null) return;

        // Только 1-е лицо
        Perspective persp = mc.options != null ? mc.options.getPerspective() : Perspective.FIRST_PERSON;
        if (persp == null || !persp.isFirstPerson()) return;

        PlayerEntity p = mc.player;
        if (p == null) return;

        // ЕДИНЫЙ стор размеров
        PlayerSizeData data = PlayerClientSizes.get(p.getUuid());
        if (data == null || !(data.height > 0.0f)) return;

        float sizeH = data.height;
        if (sizeH >= MicroRenderConfig.TINY_NEAR_THRESHOLD) {
            // обычный рост — ваниль
            return;
        }

        int fbw = Math.max(1, mc.getWindow().getFramebufferWidth());
        int fbh = Math.max(1, mc.getWindow().getFramebufferHeight());
        float aspect = (float) fbw / (float) fbh;
        if (!(aspect > 0.0f) || Float.isInfinite(aspect) || Float.isNaN(aspect)) return;

        final boolean micro = sizeH < 0.005f;
        final float NEAR_MIN = micro ? 5.0e-5f : MicroRenderConfig.NEAR_MIN_SAFE_TINY;
        final float NEAR_MAX = micro ? 0.0035f : MicroRenderConfig.NEAR_MAX_SAFE_TINY;

        // Оценка минимальной дистанции до геометрии
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

                final double maxProbe = 1.0;    // ищем только ближайшую геометрию
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

        // Если оценка не получилась — ваниль
        if (nearOpt == null || !(nearOpt > 0.0f)) return;

        // Базы и «страховки»
        float farBase =
                (sizeH < 0.005f) ? MicroRenderConfig.FAR_CLIP_MICRO :
                        (sizeH < MicroRenderConfig.TINY_THRESHOLD) ? MicroRenderConfig.FAR_CLIP_TINY :
                                MicroRenderConfig.FAR_CLIP;

        // Минимум от реальной видимой дистанции рендера + запас
        float viewDist = 1024f; // запас по умолчанию
        try {
            viewDist = Math.max(viewDist, ((GameRenderer)(Object)this).getViewDistance());
        } catch (Throwable ignored) {}
        final float FAR_MIN_FROM_VIEW = Math.max(512f, viewDist + 64f);

        float maxRatio = Math.max(10_000f, MicroRenderConfig.FAR_NEAR_MAX_RATIO);

        float near = nearOpt;
        // ВАЖНО: far не может быть меньше видимой дистанции
        float farCandidate = Math.min(farBase, near * maxRatio);
        float far = Math.max(FAR_MIN_FROM_VIEW, farCandidate);
        if (!(far > near)) far = Math.max(FAR_MIN_FROM_VIEW, near + Math.max(0.01f, near));

        float fovRad = (float) Math.toRadians(fovDegOriginal);
        if (!(fovRad > 0f) || fovRad >= (float)Math.PI) return;

        try {
            Matrix4f proj = new Matrix4f().setPerspective(fovRad, aspect, near, far);
            cir.setReturnValue(proj);
        } catch (Throwable t) {
            // Безопасный фолбэк: далеко и без сюрпризов
            float fbNear = 0.05f;
            float fbFar  = Math.max(FAR_MIN_FROM_VIEW, Math.max(2048f, farBase));
            if (fbFar <= fbNear) fbFar = fbNear + 1.0f;
            Matrix4f proj = new Matrix4f().setPerspective(fovRad, aspect, fbNear, fbFar);
            cir.setReturnValue(proj);
        }
    }
}