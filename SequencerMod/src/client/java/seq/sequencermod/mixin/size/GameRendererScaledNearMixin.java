package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.size.config.MicroRenderConfig;

/**
 * Динамический near/far в первом лице для tiny/микро‑размеров с усиленными страховками,
 * чтобы исключить нативные зависания при экстремальных значениях.
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

        // Безопасные размеры буфера (на ресайзе/минимизации)
        int fbw = Math.max(1, mc.getWindow().getFramebufferWidth());
        int fbh = Math.max(1, mc.getWindow().getFramebufferHeight());
        float aspect = (float) fbw / (float) fbh;
        if (!(aspect > 0.0f) || Float.isInfinite(aspect) || Float.isNaN(aspect)) return; // fallback к ванилле

        // Текущий AABB игрока
        Box bb = p.getBoundingBox();
        double w = Math.max(1.0e-12, bb.getXLength());
        double h = Math.max(1.0e-12, bb.getYLength());
        double d = Math.max(1.0e-12, bb.getZLength());
        double minHalfExtent = 0.5 * Math.min(w, Math.min(h, d));
        float sizeH = (float) h;

        // near: ещё чуть консервативнее
        float nearMin = 1.0e-4f;       // было 5e-5
        float nearMax = 0.0050f;
        float nearDesired = (float) (minHalfExtent * 0.50);
        float near = Math.max(nearMin, Math.min(nearMax, nearDesired));

        // far: базовый + ограничение отношения + жесткий минимум
        float farBase =
                (sizeH < 0.005f) ? MicroRenderConfig.FAR_CLIP_MICRO :
                        (sizeH < MicroRenderConfig.TINY_THRESHOLD) ? MicroRenderConfig.FAR_CLIP_TINY :
                                MicroRenderConfig.FAR_CLIP;

        float far = Math.max(64.0f, Math.max(near * 4.0f, farBase));

        // Ограничение отношения far/near
        float maxRatio = Math.max(10_000f, MicroRenderConfig.FAR_NEAR_MAX_RATIO); // на всякий случай не ниже 10k
        if (far / near > maxRatio) {
            far = Math.max(near * maxRatio, 64.0f);
        }
        // Строгое неравенство
        if (!(far > near)) {
            far = near + Math.max(0.01f, near);
        }

        float fovRad = (float) Math.toRadians(fovDegOriginal);
        if (!(fovRad > 0f) || fovRad >= Math.PI) {
            // Неправдоподобный FOV — оставляем ваниллу
            return;
        }

        try {
            Matrix4f proj = new Matrix4f().setPerspective(fovRad, aspect, near, far);
            cir.setReturnValue(proj);
        } catch (Throwable t) {
            // Фолбэк на вполне «ванильные» значения, исключающий зависания драйвера
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