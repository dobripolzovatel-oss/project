package seq.sequencermod.net.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.passive.BeeEntity;
import seq.sequencermod.net.client.morph.MorphEngine;

/**
 * Минимальная стабилизация живых сущностей без вмешательства в анимацию крыльев.
 * - Bee/Bat: держим в полёте (onGround=false), для Bat выключаем "roosting".
 * - Allay/Vex: сохраняем существующую плавную фазу крыльев (как и было).
 * - Остальные — без изменений.
 */
final class SimpleMorphsLiving {
    private SimpleMorphsLiving() {}

    static void stabilizeLiving(AbstractClientPlayerEntity p, LivingEntity le, ClientWorld world) {
        // Если это аксолотль — ничего не делаем: у него свой адаптер
        if (le instanceof AxolotlEntity) {
            return;
        }

        // Bee / Bat — только окружение для полёта, без подмены крыльев
        if (le instanceof BeeEntity || le instanceof BatEntity) {
            try { le.setOnGround(false); } catch (Throwable ignored) {}
            if (le instanceof BatEntity) {
                // Выключаем "подвешенное" состояние (roosting), чтобы мышь считалась летящей
                MorphEngine.tryInvokeMethod(le, "setRoosting", new Class<?>[]{boolean.class}, false);
            }
            return;
        }

        // Allay / Vex — оставляем существующую плавную фазу (как раньше)
        EntityType<?> t = le.getType();
        String id = String.valueOf(net.minecraft.registry.Registries.ENTITY_TYPE.getId(t));
        if (id.endsWith(":allay") || id.endsWith(":vex")) {
            try { le.setOnGround(false); } catch (Throwable ignored) {}
            applyWingPhase(world, le, p);
            return;
        }
        // Остальных не трогаем
    }

    // Плавная фаза крыльев (только для allay/vex)
    private static void applyWingPhase(ClientWorld world, LivingEntity e, AbstractClientPlayerEntity p) {
        long t = world.getTime();
        double spd = Math.hypot(p.getX() - p.prevX, p.getZ() - p.prevZ);
        float baseHz = 0.25f;
        float speedBoost = (float)Math.min(0.6, spd * 3.0);
        float phase = (float)((t % 100000L) * (baseHz + speedBoost) * 0.25f);

        float flap = phase;
        float prev = flap - 0.25f;

        String[] prevNames = {"prevWingFlapProgress","prevWingProgress","prevFlapProgress","prevMaxWingDeviation","prevWingRotation","field_28639"};
        String[] nowNames  = {"wingFlapProgress","wingProgress","flapProgress","maxWingDeviation","wingRotation","field_28638"};

        for (String n : prevNames) MorphEngine.trySetFieldFloat(e, n, prev);
        for (String n : nowNames)  MorphEngine.trySetFieldFloat(e, n, flap);
    }
}