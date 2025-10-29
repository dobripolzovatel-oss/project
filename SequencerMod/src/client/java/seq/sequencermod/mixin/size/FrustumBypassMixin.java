package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.size.config.MicroRenderConfig;

/**
 * Диагностический байпас фрустум-куллинга. Когда включён
 * MicroRenderConfig.DEBUG_DISABLE_FRUSTUM_CULLING, все боксы считаются видимыми.
 * Это полностью исключает "исчезновение мира" из-за неверных плоскостей/позиции фруствума.
 */
@Environment(EnvType.CLIENT)
@Mixin(Frustum.class)
public class FrustumBypassMixin {

    @Inject(method = "isVisible(Lnet/minecraft/util/math/Box;)Z", at = @At("HEAD"), cancellable = true)
    private void sequencer$alwaysVisibleBox(Box box, CallbackInfoReturnable<Boolean> cir) {
        if (MicroRenderConfig.DEBUG_DISABLE_FRUSTUM_CULLING) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isVisible(DDDDDD)Z", at = @At("HEAD"), cancellable = true)
    private void sequencer$alwaysVisibleCoords(double minX, double minY, double minZ,
                                               double maxX, double maxY, double maxZ,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (MicroRenderConfig.DEBUG_DISABLE_FRUSTUM_CULLING) {
            cir.setReturnValue(true);
        }
    }
}