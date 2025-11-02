package seq.sequencermod.mixin.size;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Делает getEyeHeight(pose, dims) пропорциональным высоте хитбокса для игрока.
 * Это "целевая" высота глаз без нижних порогов.
 */
@Mixin(LivingEntity.class)
public abstract class PlayerEyeHeightProportionalMixin {

    @Inject(
            method = "getEyeHeight(Lnet/minecraft/entity/EntityPose;Lnet/minecraft/entity/EntityDimensions;)F",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void sequencer$proportionalEye(EntityPose pose, EntityDimensions dims, CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof PlayerEntity)) return;

        float h = Math.max(1.0e-9f, dims.height);
        float ratio = ratioForPose(pose);
        float eye = h * ratio;

        // Анти-граница: держим глаз строго ниже потолка хитбокса
        float clearance = Math.max(1.0e-6f, h * 1.0e-3f);
        eye = Math.min(eye, Math.max(1.0e-9f, h - clearance));
        eye = Math.max(1.0e-9f, eye);

        cir.setReturnValue(eye);
    }

    private static float ratioForPose(EntityPose pose) {
        // Безопасные пропорции, совпадающие по “ощущениям” с ванилью
        return switch (pose) {
            case SWIMMING, FALL_FLYING, SPIN_ATTACK -> 0.67f;
            case SLEEPING -> 0.20f;
            case CROUCHING -> 0.90f; // ванильная 1.54/1.5 >1, но мы не даём выйти за верх
            default -> 0.90f;        // standing и прочие
        };
    }
}