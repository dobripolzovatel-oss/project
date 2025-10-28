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
import seq.sequencermod.core.debug.DebugTaps;
import seq.sequencermod.size.util.WhiteHitboxScale;

// NEW: для расчёта верха коллизии блока под ногами
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

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

    /**
     * Минимальный клиренс для first-person камеры над поверхностью блока.
     * ~12 см (0.12 блока), что надёжно выше типичной near plane (~0.05 блока) с запасом.
     * Гарантирует, что камера не окажется внутри геометрии пола при экстремально малых размерах (1e-3..1e-5).
     */
    private static final float MIN_FP_CLEARANCE = 0.12f;

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
     * Мировой нижний порог глаза: не ниже верха коллизионной формы блока под игроком + clearance.
     * Возвращает требуемую высоту глаза относительно подошвы AABB (player.getY()).
     */
    private static float worldClearanceEye(PlayerEntity p, float clearance) {
        if (p == null) return clearance;
        var w = p.getWorld();
        if (w == null) return clearance;

        Vec3d pos = p.getPos();
        // Слегка смещаемся вниз, если стоим точно на грани
        BlockPos bp = BlockPos.ofFloored(pos.x, pos.y - 1.0e-6, pos.z);

        var state = w.getBlockState(bp);
        VoxelShape shape = state.getCollisionShape(w, bp);
        double topFrac = shape.isEmpty() ? 0.0 : shape.getMax(Direction.Axis.Y); // 0..1 в пределах блока
        double blockTopY = bp.getY() + topFrac;

        // Сколько нужно относительно "подошвы" AABB игрока
        double needed = blockTopY + clearance - p.getY();
        return (float) Math.max(clearance, needed);
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

        // Мировой нижний порог: верх коллизии под ногами + 12 см (или больше при необходимости)
        float worldLower = worldClearanceEye(player, MIN_FP_CLEARANCE);

        if (minEye > maxEye) {
            // Ультра-малый AABB: вместо центрирования используем абсолютный минимум.
            float absMin = clientMinAbsEye(h, pose);

            // Камера может быть выше белого AABB — это нормально и убирает "подземный" вид.
            eye = Math.max(Math.max(absMin, MIN_FP_CLEARANCE), worldLower);

            if (DebugTaps.active.get()) {
                DebugTaps.logf("[PlayerEyeHeightClient] ULTRA-TINY: h=%.6f, worldLower=%.6f, eye=%.6f",
                        h, worldLower, eye);
            }
        } else {
            // Нормальный случай: нижняя граница — максимум из локальных зазоров, MIN_FP_CLEARANCE и мирового порога.
            float lower = Math.max(Math.max(minEye, MIN_FP_CLEARANCE), worldLower);

            // Если требуется выйти выше maxEye, НЕ жмём сверху: даём камере выйти из белого бокса.
            if (lower <= maxEye) {
                eye = Math.max(lower, Math.min(maxEye, eye));
            } else {
                eye = Math.max(lower, eye); // выходим выше AABB при необходимости
            }

            if (DebugTaps.active.get()) {
                DebugTaps.logf("[PlayerEyeHeightClient] NORMAL: h=%.6f, lower=%.6f, maxEye=%.6f, worldLower=%.6f, eye=%.6f",
                        h, lower, maxEye, worldLower, eye);
            }
        }

        // Микро-антиграница
        if (eye < maxEye) {
            float bumped = Math.nextUp(eye);
            if (bumped < maxEye) eye = bumped;
        }

        cir.setReturnValue(eye);
    }
}