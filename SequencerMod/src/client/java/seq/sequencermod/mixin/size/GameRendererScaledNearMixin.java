package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.size.config.MicroRenderConfig;

/**
 * ВАЖНО: по умолчанию мы не уменьшаем near в мире.
 * Добавлено: опционально насильно возвращаем ВСЕГДА ванильную матрицу в world‑pass,
 * чтобы исключить любые чужие модификации (диагностический «замок»).
 *
 * Малый near по‑прежнему может применяться в hand‑pass через другой миксин, если это включат флагом.
 */
@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererScaledNearMixin {

    @Shadow public abstract float getViewDistance();

    @Inject(method = "getBasicProjectionMatrix(D)Lorg/joml/Matrix4f;",
            at = @At("HEAD"),
            cancellable = true)
    private void sequencer$forceVanillaWorldProjection(double fovDegOriginal,
                                                       CallbackInfoReturnable<Matrix4f> cir) {
        if (!MicroRenderConfig.FORCE_VANILLA_WORLD_PROJECTION) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) return;

        // Ванильные параметры
        int fbw = Math.max(1, mc.getWindow().getFramebufferWidth());
        int fbh = Math.max(1, mc.getWindow().getFramebufferHeight());
        float aspect = (float) fbw / (float) fbh;

        float near = 0.05f; // жёстко ваниль
        float far  = Math.max(512f, this.getViewDistance() + 64f);

        double fovRad = Math.toRadians(fovDegOriginal);
        if (!(aspect > 0f) || !(fovRad > 0d) || !(far > near)) return;

        Matrix4f vanilla = new Matrix4f().setPerspective((float) fovRad, aspect, near, far);
        cir.setReturnValue(vanilla);
    }
}