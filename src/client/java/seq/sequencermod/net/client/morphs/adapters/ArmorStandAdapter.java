package seq.sequencermod.net.client.morphs.adapters;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.EulerAngle;
import seq.sequencermod.net.client.morphs.BaseAdapter;

public class ArmorStandAdapter extends BaseAdapter<ArmorStandEntity> {

    @Override
    public boolean shouldClientTick(ArmorStandEntity e) {
        // Не тикаем — исключаем любую «свою» физику/логику
        return false;
    }

    @Override
    public void syncFromPlayer(AbstractClientPlayerEntity p, ArmorStandEntity e) {
        // Базовая синхронизация без копирования углов — углы задаст SimpleMorphs перед рендером
        syncCommon(p, e, false, false);

        // Физика/скорость — в ноль
        try { e.setNoGravity(true); } catch (Throwable ignored) {}
        try { e.setOnGround(false); } catch (Throwable ignored) {}
        try { e.setVelocity(0, 0, 0); } catch (Throwable ignored) {}
    }

    @Override
    public void beforeRender(AbstractClientPlayerEntity p, ArmorStandEntity e, float tickDelta) {
        // Заморозить позиционную интерполяцию
        trySetField(e, "prevX", e.getX());
        trySetField(e, "prevY", e.getY());
        trySetField(e, "prevZ", e.getZ());

        // Гасим любые наклоны и резкие анимации
        try { e.setPitch(0f); } catch (Throwable ignored) {}
        trySetField(e, "prevPitch", 0f);

        zeroLivingAnimation(e);

        // Свести позы частей к нулю, чтобы стойка была «манекеном»
        EulerAngle zero = new EulerAngle(0f, 0f, 0f);
        try { e.setHeadRotation(zero); } catch (Throwable ignored) {}
        try { e.setBodyRotation(zero); } catch (Throwable ignored) {}
        try { e.setLeftArmRotation(zero); } catch (Throwable ignored) {}
        try { e.setRightArmRotation(zero); } catch (Throwable ignored) {}
        try { e.setLeftLegRotation(zero); } catch (Throwable ignored) {}
        try { e.setRightLegRotation(zero); } catch (Throwable ignored) {}

        // На всякий — физика off на кадр
        try { e.setNoGravity(true); } catch (Throwable ignored) {}
        try { e.setOnGround(false); } catch (Throwable ignored) {}
        try { e.setVelocity(0, 0, 0); } catch (Throwable ignored) {}
    }

    private void zeroLivingAnimation(LivingEntity le) {
        trySetFieldFloat(le, "limbDistance", 0f);
        trySetFieldFloat(le, "lastLimbDistance", 0f);
        trySetFieldFloat(le, "limbAngle", 0f);
        trySetFieldFloat(le, "strideDistance", 0f);
        trySetFieldFloat(le, "prevStrideDistance", 0f);

        trySetFieldFloat(le, "handSwingProgress", 0f);
        trySetFieldFloat(le, "lastHandSwingProgress", 0f);
        trySetField(le, "handSwinging", false);
        trySetField(le, "handSwingTicks", 0);

        trySetField(le, "hurtTime", 0);
        trySetFieldFloat(le, "hurtAnimationProgress", 0f);
        trySetFieldFloat(le, "prevHurtAnimationProgress", 0f);
    }
}