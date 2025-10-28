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
 * Для нормальных размеров зажимаем eye внутри AABB: [пол + eps; потолок - eps].
 * Для ультра-малых размеров (h < 0.002f) применяем абсолютный минимум высоты глаз,
 * который может быть выше AABB, чтобы избежать камеры "под землёй".
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
     * Вычисляет минимальную абсолютную высоту глаз на клиенте для первого лица.
     * Используется для ультра-малых хитбоксов, чтобы камера не была "под землёй".
     * Логика согласована с серверной логикой в PlayerStandingEyeHeightBypassMixin.
     *
     * @param h    высота белого хитбокса
     * @param pose текущая поза игрока
     * @return минимальная абсолютная высота глаз (в метрах/блоках)
     */
    private static float clientMinAbsEye(float h, EntityPose pose) {
        // Базовый абсолютный минимум зависит от размера хитбокса
        float base = (h < 0.002f) ? 0.0045f :
                     (h < 0.010f) ? 0.0030f :
                     (h < 0.050f) ? 0.0020f :
                                     0.0000f;
        
        // Корректировка для специальных поз
        if (pose == EntityPose.SWIMMING || pose == EntityPose.FALL_FLYING) base *= 0.85f;
        else if (pose == EntityPose.SLEEPING) base *= 0.5f;
        
        // Относительный минимум (15% от высоты)
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

        // Жёстко зажимаем внутрь AABB
        float minEye = floorEps;
        float maxEye = Math.max(WhiteHitboxScale.EPS_HEIGHT, h - topEps);
        if (minEye > maxEye) {
            // Ультра-маленький AABB (h ~ 1e-5): слои пересеклись.
            // Вместо центрирования применяем абсолютный минимум высоты глаз,
            // чтобы камера не была "под землёй". Это может поднять eye выше AABB.
            eye = clientMinAbsEye(h, pose);
            // НЕ зажимаем в maxEye — разрешаем камере быть выше белого бокса
            // для экстремально малых размеров
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