package seq.sequencermod.net.client.morphs.animation;

import net.minecraft.util.math.MathHelper;

/**
 * Адаптация идей LivingEntity.updateLimbs и LimbAnimator:
 * - limbSwing растёт пропорционально скорости
 * - limbSwingAmount плавно приближается к целевой амплитуде
 */
public final class LimbAnimatorCompat {
    private float limbSwing;
    private float limbSwingAmount;
    private float limbSwingAmountPrev;

    public void tick(float horizontalSpeed, boolean inFluidOrAir) {
        // horizontalSpeed = длина горизонтальной скорости (м/тик)
        // Масштабируем под анимацию (ваниль использует 4.0F и сглаживание)
        float speedForAnim = Math.min(horizontalSpeed * 4.0f, 1.0f);

        // Плавное приближение амплитуды (как у LivingEntity: дельта 0.4 -> 0.3)
        limbSwingAmountPrev = limbSwingAmount;
        limbSwingAmount += (speedForAnim - limbSwingAmount) * 0.3f;

        // Фаза шага; если в воздухе/жидкости, можно снижать вклад
        float phaseMul = inFluidOrAir ? 0.6f : 1.0f;
        limbSwing += speedForAnim * phaseMul;
    }

    public float getLimbSwing() {
        return limbSwing;
    }

    public float getLimbSwingAmount(float tickDelta) {
        return MathHelper.lerp(tickDelta, limbSwingAmountPrev, limbSwingAmount);
    }
}