package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.size.util.WhiteHitboxScale;

/**
 * CLIENT: высота глаз локального игрока в ПЕРВОМ лице = пропорция от высоты БЕЛОГО хитбокса (текущего AABB).
 * Обычно зажимаем eye внутри AABB: [пол + eps; потолок - eps].
 * При экстремально малых размерах (h < 0.002f) разрешаем камере быть выше AABB,
 * используя абсолютный минимум (аналогично серверной логике PlayerStandingEyeHeightBypassMixin),
 * чтобы избежать "подземного" вида.
 */
@Environment(EnvType.CLIENT)
@Mixin(value = PlayerEntity.class, priority = 1000)
public abstract class PlayerEyeHeightClientMixin {

    private static float ratioForPose(EntityPose pose) {
        if (pose == null) return 0.90f;
        return switch (pose) {
            case SWIMMING, FALL_FLYING, SPIN_ATTACK -> 0.67f;
            case SLEEPING -> -1f;     // сон оставляем ванилле
            case CROUCHING -> 0.90f;
            default -> 0.90f;
        };
    }

    /**
     * Минимальная абсолютная высота глаз для клиента (в метрах/блоках).
     * Копирует логику baseMinAbsEye из PlayerStandingEyeHeightBypassMixin.
     * Для ультра-малых хитбоксов (h < 0.002f) возвращает достаточную высоту,
     * чтобы камера не оказалась внутри блока пола.
     * 
     * Значения согласованы с серверной логикой и подобраны эмпирически
     * для гарантии видимости при экстремально малых размерах.
     */
    private static float clientMinAbsEye(float h, EntityPose pose) {
        // Базовые абсолютные пороги (в метрах) для разных диапазонов высоты.
        // Значения совпадают с PlayerStandingEyeHeightBypassMixin#baseMinAbsEye.
        float base = (h < 0.002f) ? 0.0045f :  // 4.5 мм для микро-размеров
                     (h < 0.010f) ? 0.0030f :  // 3.0 мм для очень малых
                     (h < 0.050f) ? 0.0020f :  // 2.0 мм для малых
                                     0.0000f;   // 0 для нормальных (пропорция преобладает)

        // Корректировка для специальных поз (согласовано с сервером)
        if (pose == EntityPose.SWIMMING || pose == EntityPose.FALL_FLYING) base *= 0.85f;
        else if (pose == EntityPose.SLEEPING) base *= 0.5f;

        // Относительный минимум — 15% от высоты
        float relativeMin = h * 0.15f;
        return Math.max(base, relativeMin);
    }

    // getActiveEyeHeight(Lnet/minecraft/entity/EntityPose;Lnet/minecraft/entity/EntityDimensions;)F
    @Inject(
            method = "getActiveEyeHeight(Lnet/minecraft/entity/EntityPose;Lnet/minecraft/entity/EntityDimensions;)F",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sequencer$whiteAabbEye(EntityPose pose, EntityDimensions dims, CallbackInfoReturnable<Float> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        // Только локальный игрок и только 1-е лицо
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player != player || !mc.options.getPerspective().isFirstPerson()) return;

        float ratio = ratioForPose(pose);
        if (ratio < 0f) return; // sleeping -> ванилла

        // КЛЮЧ: берём высоту БЕЛОГО хитбокса (AABB), а не nominal dimensions
        float h = WhiteHitboxScale.whiteHeight(player);

        // Базовый eye по пропорции
        float eye = ratio * h;

        // Мягкие зазоры от пола/потолка: в долях от h + минимальный eps
        final float ABS_FLOOR_CLEARANCE = 0.02f;
        float floorEps = Math.max(ABS_FLOOR_CLEARANCE, Math.max(WhiteHitboxScale.EPS_HEIGHT, 0.005f * h));
        float topEps   = Math.max(WhiteHitboxScale.EPS_HEIGHT, 0.01f  * h); // ~1% h

        // Обычно зажимаем внутрь AABB
        float minEye = floorEps;
        float maxEye = Math.max(WhiteHitboxScale.EPS_HEIGHT, h - topEps);
        if (minEye > maxEye) {
            // Ультра-малый AABB: слои пересеклись.
            // Вместо центрирования используем абсолютный минимум,
            // который может быть выше h — это нормально и убирает "подземный" вид.
            eye = clientMinAbsEye(h, pose);
            // НЕ зажимаем до maxEye, чтобы камера могла быть выше белого бокса
        } else {
            eye = Math.max(minEye, Math.min(maxEye, eye));
        }

        // Микро-антиграница
        if (eye < maxEye) {
            float bumped = Math.nextUp(eye);
            if (bumped < maxEye) eye = bumped;
        }

        cir.setReturnValue(eye);
    }
}