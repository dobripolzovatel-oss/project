package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import seq.sequencermod.render.RenderPassFlags;

/**
 * Помечаем участок рендера руки (renderHand) — чтобы FOV‑компенсация
 * не применялась к рукам/предметам.
 */
@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererHandPassHookMixin {

    @Inject(method = "renderHand(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/Camera;F)V",
            at = @At("HEAD"), require = 0)
    private void sequencer$enterHand(MatrixStack matrices, Camera camera, float tickDelta, CallbackInfo ci) {
        RenderPassFlags.enterHand();
    }

    @Inject(method = "renderHand(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/Camera;F)V",
            at = @At("TAIL"), require = 0)
    private void sequencer$exitHand(MatrixStack matrices, Camera camera, float tickDelta, CallbackInfo ci) {
        RenderPassFlags.exitHand();
    }
}