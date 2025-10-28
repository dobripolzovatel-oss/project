package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.core.debug.DebugTaps;
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

    /**
     * Вычисляет минимальную высоту глаз относительно позиции игрока (player.getY()),
     * основанную на реальной геометрии блока коллизии под ногами игрока.
     * 
     * Это гарантирует, что камера находится выше верхней границы блока коллизии
     * как минимум на clearance блоков, независимо от размера белого AABB.
     * 
     * @param p PlayerEntity
     * @param clearance Минимальный зазор над верхней поверхностью блока (в блоках)
     * @return Минимальная высота глаз относительно player.getY()
     */
    private static float worldClearanceEye(PlayerEntity p, float clearance) {
        if (p == null || p.getWorld() == null) return 0f;
        
        // Позиция ног игрока (нижняя граница AABB)
        double playerY = p.getY();
        
        // Блок, в котором находятся ноги игрока
        BlockPos feetPos = BlockPos.ofFloored(p.getX(), playerY, p.getZ());
        
        // Получаем форму коллизии блока под ногами (кешируем BlockState)
        BlockState feetState = p.getWorld().getBlockState(feetPos);
        VoxelShape collisionShape = feetState.getCollisionShape(p.getWorld(), feetPos);
        
        // Если блок имеет коллизию, берём верхнюю границу
        double blockTopY;
        if (!collisionShape.isEmpty()) {
            // Верхняя граница коллизионной формы в мировых координатах
            blockTopY = feetPos.getY() + collisionShape.getMax(Direction.Axis.Y);
        } else {
            // Нет коллизии в текущем блоке - проверяем блок ниже
            BlockPos belowPos = feetPos.down();
            BlockState belowState = p.getWorld().getBlockState(belowPos);
            VoxelShape belowShape = belowState.getCollisionShape(p.getWorld(), belowPos);
            
            if (!belowShape.isEmpty()) {
                blockTopY = belowPos.getY() + belowShape.getMax(Direction.Axis.Y);
            } else {
                // Нет твёрдой поверхности - возвращаем 0 (не применяем ограничение)
                return 0f;
            }
        }
        
        // Минимальная мировая Y для глаз = верх блока + клиренс
        double minWorldEyeY = blockTopY + clearance;
        
        // Преобразуем в относительную высоту от playerY
        float worldLower = (float) (minWorldEyeY - playerY);
        
        return Math.max(0f, worldLower);
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
        
        // Вычисляем мировой нижний предел на основе реальной геометрии блока
        float worldLower = worldClearanceEye(player, MIN_FP_CLEARANCE);
        
        if (minEye > maxEye) {
            // Ультра-малый AABB: слои пересеклись.
            // Вместо центрирования используем максимум из абсолютного минимума,
            // MIN_FP_CLEARANCE и worldLower.
            // НЕ зажимаем до maxEye: при экстремально малых размерах камера может быть выше белого бокса.
            float absMin = clientMinAbsEye(h, pose);
            float minRequired = Math.max(absMin, MIN_FP_CLEARANCE);
            eye = Math.max(minRequired, worldLower);
            
            if (DebugTaps.active.get()) {
                DebugTaps.logf("[PlayerEyeHeightClient] ULTRA-TINY: h=%.6f, pose=%s, absMin=%.6f, worldLower=%.6f, eye=%.6f",
                        h, pose, absMin, worldLower, eye);
            }
        } else {
            // Нормальный случай: зажимаем внутрь AABB, но с учётом MIN_FP_CLEARANCE и worldLower.
            // Если worldLower требует eye > maxEye, разрешаем камере выйти выше AABB.
            float aabbLower = Math.max(minEye, MIN_FP_CLEARANCE);
            float lower = Math.max(aabbLower, worldLower);
            
            if (lower <= maxEye) {
                // Можем вместить в AABB
                eye = Math.max(lower, Math.min(maxEye, eye));
            } else {
                // worldLower требует выйти выше AABB - разрешаем это
                eye = Math.max(lower, eye);
            }
            
            if (DebugTaps.active.get()) {
                DebugTaps.logf("[PlayerEyeHeightClient] NORMAL: h=%.6f, pose=%s, lower=%.6f, maxEye=%.6f, worldLower=%.6f, eye=%.6f",
                        h, pose, lower, maxEye, worldLower, eye);
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