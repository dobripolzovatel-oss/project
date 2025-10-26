package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Проекцию больше не переопределяем — возвращаем ванильное поведение.
 */
@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererScaledNearMixin {

    @Inject(method = "getBasicProjectionMatrix(D)Lorg/joml/Matrix4f;", at = @At("HEAD"), cancellable = true)
    private void sequencer$keepVanillaProjection(double fovDegOriginal, CallbackInfoReturnable<Matrix4f> cir) {
        // Ничего не делаем: оставляем матрицу проекции как в ваниле.
    }
}