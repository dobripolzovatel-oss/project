package seq.sequencermod.mixin.size;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import seq.sequencermod.size.util.SizeCalc;

/**
 * Масштабируем шаг через порог (stepHeight) пропорционально текущей высоте хитбокса игрока.
 * Это убирает "залипание" на кромках при низком росте и не влияет на камеру.
 */
@Mixin(Entity.class)
public abstract class PlayerStepHeightScaleMixin {

    @Shadow public float stepHeight;

    @Inject(method = "calculateDimensions", at = @At("TAIL"))
    private void sequencer$scaleStepHeight(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof PlayerEntity p)) return;

        float h = Math.max(SizeCalc.EPS, p.getDimensions(p.getPose()).height);

        // stepHeight ≈ 35% от высоты + небольшой базовый запас
        float step = 0.35f * h + 0.02f;
        if (step < 0.05f) step = 0.05f;  // не ниже 5 см
        if (step > 0.60f) step = 0.60f;  // ванильный максимум

        if (Math.abs(this.stepHeight - step) > 1e-5f) {
            this.stepHeight = step;
        }
    }
}