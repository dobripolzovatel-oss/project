package seq.sequencermod.mixin.size;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.size.util.WhiteHitboxScale;

/**
 * Шаг 2: привязываем высоту глаз к "белому" хитбоксу (текущему AABB).
 * Пропорциональные глаза + усиленный "анти-пол" для tiny и отдельно для травы/растений.
 * Разрешаем глаз быть чуть выше хитбокса на ультра-малом росте — это нормально и убирает "подмир".
 */
@Mixin(Entity.class)
public abstract class PlayerStandingEyeHeightBypassMixin {

    @Inject(method = "getStandingEyeHeight()F", at = @At("HEAD"), cancellable = true, require = 0)
    private void sequencer$proportionalStandingEye(CallbackInfoReturnable<Float> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof PlayerEntity p)) return;

        // используем высоту "белого" хитбокса (AABB), а не nominal dimensions
        final float h = Math.max(WhiteHitboxScale.EPS_HEIGHT, WhiteHitboxScale.whiteHeight(p));
        final EntityPose pose = p.getPose();

        // Базовая пропорция — без switch, чтобы не создавать синтетический класс $1
        float ratio;
        if (pose == EntityPose.SWIMMING || pose == EntityPose.FALL_FLYING || pose == EntityPose.SPIN_ATTACK) {
            ratio = 0.67f;
        } else if (pose == EntityPose.SLEEPING) {
            ratio = 0.20f;
        } else if (pose == EntityPose.CROUCHING) {
            ratio = 0.90f;
        } else {
            ratio = 0.90f; // STANDING и прочие
        }
        float eyeByRatio = h * ratio;

        // Абсолютный минимум "над полом" (мм). Усиленные пороги для h<0.01
        float minAbsEye = baseMinAbsEye(h, pose);

        // Если стоим внутри травы/растения — добавим ещё +1.5 мм
        if (isInsidePlantish(p)) {
            minAbsEye += 0.0015f;
        }

        // Целевое: максимум из пропорции и абсолютного минимума
        float eye = Math.max(eyeByRatio, minAbsEye);

        // Анти-потолок: если глаз внутри AABB — опускаем на микрон ниже крыши.
        // Если уже выше top (minAbsEye > h) — НЕ опускаем (это и нужно для микроскопических размеров).
        float topClear = Math.max(1.0e-6f, h * 1.0e-3f);
        if (eye < h) {
            eye = Math.min(eye, Math.max(1.0e-9f, h - topClear));
        }

        eye = Math.max(1.0e-9f, eye);
        cir.setReturnValue(eye);
    }

    private static float baseMinAbsEye(float h, EntityPose pose) {
        // Под эти значения подгоняли h≈0.001: гарантированно выше верха блока
        float base =
                (h < 0.002f) ? 0.0045f :
                        (h < 0.010f) ? 0.0030f :
                                (h < 0.050f) ? 0.0020f :
                                        0.0000f;

        // В воде/элитрах можно на ~15% ниже, лёжа — ещё ниже
        if (pose == EntityPose.SWIMMING || pose == EntityPose.FALL_FLYING) base *= 0.85f;
        else if (pose == EntityPose.SLEEPING) base *= 0.5f;

        // Небольшой относительный минимум, чтобы не "прилипать" к 0 при росте
        float relativeMin = h * 0.15f;
        return Math.max(base, relativeMin);
    }

    /**
     * Эвристика "мы стоим в растении": блок не AIR и его коллизия пуста.
     * Проверяем текущий блок и блок над ним.
     */
    private static boolean isInsidePlantish(PlayerEntity p) {
        BlockPos pos = p.getBlockPos();
        BlockState here = p.getWorld().getBlockState(pos);
        if (!here.isAir() && here.getCollisionShape(p.getWorld(), pos).isEmpty()) return true;

        BlockPos up = pos.up();
        BlockState above = p.getWorld().getBlockState(up);
        return !above.isAir() && above.getCollisionShape(p.getWorld(), up).isEmpty();
    }
}