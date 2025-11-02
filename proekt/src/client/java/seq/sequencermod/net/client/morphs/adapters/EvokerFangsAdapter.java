package seq.sequencermod.net.client.morphs.adapters;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.mob.EvokerFangsEntity;
import net.minecraft.util.math.MathHelper;
import seq.sequencermod.net.client.SimpleMorphsSpecial;
import seq.sequencermod.net.client.morphs.BaseAdapter;

import java.util.Map;
import java.util.WeakHashMap;

public class EvokerFangsAdapter extends BaseAdapter<EvokerFangsEntity> {

    /**
     * Храним сглаженный yaw отдельно, чтобы не зависеть от внутренних полей сущности.
     * WeakHashMap — память очистится, когда сущность будет собрана GC.
     */
    private static final Map<EvokerFangsEntity, Float> SMOOTH_YAW = new WeakHashMap<>();

    // Базовый коэффициент сглаживания (EMA). Фактический alpha будет адаптивно увеличиваться при больших поворотах.
    private static final float BASE_ALPHA = 0.28f;
    // Максимальный шаг за кадр (градусов) при обычном повороте.
    private static final float MAX_STEP_PER_SECOND = 240f; // градусов в секунду
    // Жёсткий лимит даже при “рывке”.
    private static final float HARD_CAP_PER_SECOND = 720f;
    // Dead zone чтобы убрать дрожание на микродиффах.
    private static final float DEAD_ZONE_DEG = 0.15f;
    // Порог “быстрого поворота”.
    private static final float FAST_DIFF_TRIGGER = 140f;

    @Override
    public boolean shouldClientTick(EvokerFangsEntity e) {
        // Нужно для корректного уменьшения ticksLeft и спавна частиц.
        return true;
    }

    @Override
    public void syncFromPlayer(AbstractClientPlayerEntity p, EvokerFangsEntity e) {
        // БЕРЕМ только позицию (углы не копируем, copyRotation=false) — сглаживание делаем сами.
        syncCommon(p, e, false, false);

        // При первом появлении инициализируем сглаженный yaw текущим целевым значением.
        float target = p.bodyYaw;
        SMOOTH_YAW.computeIfAbsent(e, ent -> target);

        // Отключаем физику — клыки “прибиты” к игроку.
        try { e.setNoGravity(true); } catch (Throwable ignored) {}
        try { e.setOnGround(false); } catch (Throwable ignored) {}
        try { e.setVelocity(0, 0, 0); } catch (Throwable ignored) {}

        // pitch не нужен
        try { e.setPitch(0f); } catch (Throwable ignored) {}
        trySetField(e, "prevPitch", 0f);

        // Сбрасываем prevXYZ чтобы исключить растяжки
        trySetField(e, "prevX", e.getX());
        trySetField(e, "prevY", e.getY());
        trySetField(e, "prevZ", e.getZ());

        try { e.setInvisible(false); } catch (Throwable ignored) {}
    }

    @Override
    public void beforeRender(AbstractClientPlayerEntity p, EvokerFangsEntity e, float tickDelta) {
        if (e.isRemoved()) {
            SMOOTH_YAW.remove(e);
            return;
        }

        // Обновляем prevXYZ
        trySetField(e, "prevX", e.getX());
        trySetField(e, "prevY", e.getY());
        trySetField(e, "prevZ", e.getZ());

        // Плавный поворот
        updateSmoothYaw(p, e, tickDelta);

        // pitch = 0
        try { e.setPitch(0f); } catch (Throwable ignored) {}
        trySetField(e, "prevPitch", 0f);

        // Без физики
        try { e.setNoGravity(true); } catch (Throwable ignored) {}
        try { e.setOnGround(false); } catch (Throwable ignored) {}
        try { e.setVelocity(0, 0, 0); } catch (Throwable ignored) {}
        try { e.setInvisible(false); } catch (Throwable ignored) {}

        // Цикл анимации
        SimpleMorphsSpecial.keepFangsVisible(e);
    }

    private void updateSmoothYaw(AbstractClientPlayerEntity p, EvokerFangsEntity e, float tickDelta) {
        // Целевой yaw — можно заменить на p.getYaw() если хочешь ориентироваться по голове
        float target = p.bodyYaw;

        float currentSmooth = SMOOTH_YAW.getOrDefault(e, target);

        // Разница по кратчайшей дуге
        float diff = MathHelper.wrapDegrees(target - currentSmooth);

        // Dead zone
        if (Math.abs(diff) < DEAD_ZONE_DEG) {
            diff = 0f;
        }

        // Адаптивное альфа: при больших диффах ускоряем реакцию
        float absDiff = Math.abs(diff);
        float alpha = BASE_ALPHA;

        if (absDiff > 45f) {
            // плавно растём до 0.6
            alpha = MathHelper.clamp(BASE_ALPHA + (absDiff - 45f) / 135f * 0.32f, BASE_ALPHA, 0.6f);
        }

        if (absDiff > FAST_DIFF_TRIGGER) {
            // Почти моментально догоним, но всё же через ограниченный шаг
            alpha = 0.85f;
        }

        // Преобразуем “в секунду” в “за этот кадр”
        float maxStepThisFrame = MAX_STEP_PER_SECOND * tickDelta;
        float hardCapThisFrame = HARD_CAP_PER_SECOND * tickDelta;

        if (absDiff > FAST_DIFF_TRIGGER) {
            maxStepThisFrame = hardCapThisFrame; // разрешаем ускоренный догон
        }

        float step = diff * alpha;

        // Ограничиваем шаг
        if (Math.abs(step) > maxStepThisFrame) {
            step = Math.copySign(maxStepThisFrame, step);
        }

        float newSmooth = currentSmooth + step;

        // Финальный снэп если почти пришли
        float remaining = MathHelper.wrapDegrees(target - newSmooth);
        if (Math.abs(remaining) < DEAD_ZONE_DEG) {
            newSmooth = target;
        }

        // Пишем prevYaw до установки нового yaw (если где-то есть интерполяция — она будет плавной)
        trySetField(e, "prevYaw", currentSmooth);
        try { e.setYaw(newSmooth); } catch (Throwable ignored) {}

        SMOOTH_YAW.put(e, newSmooth);
    }
}