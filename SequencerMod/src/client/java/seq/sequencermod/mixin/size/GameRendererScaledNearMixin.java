package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.size.config.MicroRenderConfig;

/**
 * ВАЖНО: мы больше НЕ меняем проекцию для рендера мира.
 * Любые попытки уменьшать near здесь часто приводят к рассинхрону
 * фруствума и глубины -> исчезающие чанк-грязи/пустое небо.
 * Малый near применяется только в пассе руки (см. GameRendererHandPassHookMixin).
 */
@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererScaledNearMixin {

    @Inject(method = "getBasicProjectionMatrix(D)Lorg/joml/Matrix4f;", at = @At("HEAD"), cancellable = true)
    private void sequencer$customProjection(double fovDegOriginal, CallbackInfoReturnable<Matrix4f> cir) {
        // Полностью оставляем ванильную матрицу для мира.
        // Если когда-нибудь понадобится вернуть — включим флагом.
        if (!MicroRenderConfig.APPLY_CUSTOM_NEAR_IN_WORLD) return;

        // Если кто-то включит флаг обратно — тут можно вернуть защищённую реализацию,
        // но по умолчанию — не трогаем.
    }
}