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
 * Динамический near/far в первом лице с учётом РЕАЛЬНОГО расстояния до стены по направлению взгляда.
 * Это устраняет «просвечивание» сквозь блок при упоре носом в стену на микро‑масштабах.
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

        // Безопасные размеры буфера
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

        // 1) Базовый near от размера
        final float NEAR_ABS_MIN = 1.0e-6f;   // допущение в экстремуме: достаточно мал, но надёжный с ограничением far/near
        final float NEAR_SOFT_MIN = 5.0e-5f;  // «мягкий» минимум, если впереди нет стены вплотную
        final float NEAR_MAX = 0.0050f;

        float nearDesired = (float) (minHalfExtent * 0.50);
        float near = Math.max(NEAR_SOFT_MIN, Math.min(NEAR_MAX, nearDesired));

        // 2) Коррекция near по расстоянию до стенки по взгляду (raycast до 0.25 блока)
        try {
            Camera cam = ((GameRenderer) (Object) this).getCamera();
            if (cam != null) {
                Vec3d start = cam.getPos();
                // Направление из углов камеры: fromPolar(pitch, yaw) — градусы
                Vec3d dir = Vec3d.fromPolar(cam.getPitch(), cam.getYaw());
                double maxProbe = 0.25; // достаточно для «упора носом»
                Vec3d end = start.add(dir.multiply(maxProbe));

                HitResult hit = mc.world.raycast(new RaycastContext(
                        start, end,
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE,
                        p
                ));

                if (hit != null && hit.getType() != HitResult.Type.MISS) {
                    double frontDist = Math.max(0.0, hit.getPos().distanceTo(start));
                    // Делать near меньше, чем стена впереди (с запасом 80%), но не ниже абсолютного минимума.
                    float nearFromWall = (float) Math.max(NEAR_ABS_MIN, frontDist * 0.80);
                    // Взять минимум из текущего near и «near от стенки», затем снова клампнуть в общий диапазон
                    near = Math.max(NEAR_ABS_MIN, Math.min(NEAR_MAX, Math.min(near, nearFromWall)));
                }
            }
        } catch (Throwable ignored) {
            // При любой аномалии остаёмся на базовом near
        }

        // 3) far: базовый + ограничение отношения + жёсткий минимум
        float farBase =
                (sizeH < 0.005f) ? MicroRenderConfig.FAR_CLIP_MICRO :
                        (sizeH < MicroRenderConfig.TINY_THRESHOLD) ? MicroRenderConfig.FAR_CLIP_TINY :
                                MicroRenderConfig.FAR_CLIP;

        float far = Math.max(64.0f, Math.max(near * 4.0f, farBase));

        // Ограничение отношения far/near для стабильности глубины/драйвера
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
            // Фолбэк на «ванильные» значения при любой проблеме на стороне драйвера/JOML
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