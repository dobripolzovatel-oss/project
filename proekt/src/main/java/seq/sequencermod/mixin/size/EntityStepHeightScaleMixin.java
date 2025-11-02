package seq.sequencermod.mixin.size;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.size.util.ReachCalc;

/**
 * Масштабируем шаг (step height) только для игроков. Работает и на клиенте, и на сервере.
 */
@Mixin(Entity.class)
public abstract class EntityStepHeightScaleMixin {

    @Inject(method = "getStepHeight()F", at = @At("HEAD"), cancellable = true, require = 0)
    private void sequencer$scaledStepHeight(CallbackInfoReturnable<Float> cir) {
        Entity self = (Entity)(Object)this;
        if (self instanceof PlayerEntity p) {
            cir.setReturnValue(ReachCalc.stepHeightFor(p));
        }
    }
}