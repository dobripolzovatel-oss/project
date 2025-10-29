package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.size.config.MicroRenderConfig;

/**
 * ВАЖНО: мы больше НЕ меняем проекцию для рендера мира.
 * Любые попытки уменьшать near здесь часто приводят к рассинхрону
 * фруствума и глубины -> исчезающие чанк-грязи/пустое небо.
 * Малый near применяется только в пассе руки (см. GameRendererHandPassHookMixin).
 *
 * Новое: FORCE_VANILLA_WORLD_PROJECTION включает принудительное применение
 * чистой ванильной проекции (near=0.05, far=viewDist+64), защищая от других модов.
 */
@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererScaledNearMixin {

    @Shadow public abstract float getViewDistance();

    @Inject(method = "getBasicProjectionMatrix(D)Lorg/joml/Matrix4f;", at = @At("HEAD"), cancellable = true)
    private void sequencer$customProjection(double fovDegOriginal, CallbackInfoReturnable<Matrix4f> cir) {
        // Если включён принудительный ванильный режим, создаём чистую проекцию
        if (MicroRenderConfig.FORCE_VANILLA_WORLD_PROJECTION) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null || mc.getWindow() == null) return;

            int fbw = Math.max(1, mc.getWindow().getFramebufferWidth());
            int fbh = Math.max(1, mc.getWindow().getFramebufferHeight());
            float aspect = (float) fbw / (float) fbh;

            // Чистые ванильные значения
            float near = 0.05f;
            float far = Math.max(512f, getViewDistance() + MicroRenderConfig.FAR_PLANE_MARGIN);

            double fovRad = Math.toRadians(fovDegOriginal);

            if (MicroRenderConfig.DEBUG_PROJECTION_LOGS) {
                System.out.printf("[WORLD] FOV=%.2f° aspect=%.3f near=%.4f far=%.1f viewDist=%.1f%n",
                    fovDegOriginal, aspect, near, far, getViewDistance());
            }

            Matrix4f vanillaMatrix = new Matrix4f().setPerspective((float) fovRad, aspect, near, far);
            cir.setReturnValue(vanillaMatrix);
            return;
        }

        // Полностью оставляем ванильную матрицу для мира.
        // Если когда-нибудь понадобится вернуть — включим флагом.
        if (!MicroRenderConfig.APPLY_CUSTOM_NEAR_IN_WORLD) return;

        // Если кто-то включит флаг обратно — тут можно вернуть защищённую реализацию,
        // но по умолчанию — не трогаем.
    }
}