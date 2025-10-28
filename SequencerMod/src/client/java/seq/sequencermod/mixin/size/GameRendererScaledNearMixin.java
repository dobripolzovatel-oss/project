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
 * Динамический near/far в первом лице для tiny/микро‑размеров.
 * - near привязываем к минимальному полуразмеру текущего белого AABB (по сути — к «толщине» игрока).
 * - far поджимаем так, чтобы far/near <= FAR_NEAR_MAX_RATIO (сохраняем точность depth).
 *
 * Это устраняет «срезы» стены, когда камера в упор к блоку на 0.01..0.00001.
 */
@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererScaledNearMixin {

    @Inject(method = "getBasicProjectionMatrix(D)Lorg/joml/Matrix4f;", at = @At("HEAD"), cancellable = true)
    private void sequencer$customProjection(double fovDegOriginal, CallbackInfoReturnable<Matrix4f> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;

        // Только 1-е лицо — в остальных случаях оставляем ваниллу
        Perspective persp = mc.options != null ? mc.options.getPerspective() : Perspective.FIRST_PERSON;
        if (persp == null || !persp.isFirstPerson()) return;

        PlayerEntity p = mc.player;
        if (p == null) return;

        // Текущий белый AABB игрока (в клиенте он совпадает с реальным)
        Box bb = p.getBoundingBox();
        double w = Math.max(1.0e-12, bb.getXLength());
        double h = Math.max(1.0e-12, bb.getYLength());
        double d = Math.max(1.0e-12, bb.getZLength());
        double minHalfExtent = 0.5 * Math.min(w, Math.min(h, d));

        // near: чем меньше игрок, тем меньше near, чтобы не «срезать» стену у самого носа
        // Границы подобраны эмпирически: от 1e-5 до 0.005 блока
        float nearMin = (float) 1.0e-5;
        float nearMax = 0.0050f;
        // Базовый near — доля от минимального полуразмера (мягкий, но с клэмпами)
        float nearDesired = (float) Math.max(nearMin, Math.min(nearMax, minHalfExtent * 0.50));

        // far: меньше для микро, побольше для обычных; затем ограничиваем по отношению
        float sizeH = (float) h;
        float farBase =
                (sizeH < 0.005f) ? MicroRenderConfig.FAR_CLIP_MICRO :
                        (sizeH < MicroRenderConfig.TINY_THRESHOLD) ? MicroRenderConfig.FAR_CLIP_TINY :
                                MicroRenderConfig.FAR_CLIP;

        float near = nearDesired;
        float far = farBase;

        // Страховка точности depth: ограничиваем отношение far/near
        if (far / near > MicroRenderConfig.FAR_NEAR_MAX_RATIO) {
            far = Math.max(near * MicroRenderConfig.FAR_NEAR_MAX_RATIO, 16.0f);
        }

        // Построение перспективы с нашими near/far
        int fbw = Math.max(1, mc.getWindow().getFramebufferWidth());
        int fbh = Math.max(1, mc.getWindow().getFramebufferHeight());
        float aspect = (float) fbw / (float) fbh;
        float fovRad = (float) Math.toRadians(fovDegOriginal);

        Matrix4f proj = new Matrix4f().setPerspective(fovRad, aspect, near, far);
        cir.setReturnValue(proj);
    }
}