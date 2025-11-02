package seq.sequencermod.net.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.mob.EvokerFangsEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.EntityStatuses;
import seq.sequencermod.net.client.morph.MorphEngine;

public final class SimpleMorphsSpecial {
    private SimpleMorphsSpecial() {}

    public static void keepFangsVisible(EvokerFangsEntity f) {
        // Без звука при каждом перезапуске
        try { f.setSilent(true); } catch (Throwable ignored) {}

        // Ваниль: renderer вообще рендерит только если getAnimationProgress() > 0
        float prog = 0f;
        try {
            prog = f.getAnimationProgress(0f);
        } catch (Throwable ignored) {}

        // Если анимация не запущена — инициируем её как ваниль: статус PLAY_ATTACK_SOUND
        if (prog == 0f) {
            // ticksLeft по умолчанию 22 — на всякий случай выставим
            MorphEngine.trySetField(f, "ticksLeft", 22);
            try { f.handleStatus(EntityStatuses.PLAY_ATTACK_SOUND); } catch (Throwable ignored) {}
        } else {
            // В самом конце h→1, и рендерер схлопывает масштаб к 0 — перезапустим немного заранее
            if (prog >= 0.98f) {
                MorphEngine.trySetField(f, "ticksLeft", 22);
                try { f.handleStatus(EntityStatuses.PLAY_ATTACK_SOUND); } catch (Throwable ignored) {}
            }
        }

        // Твики безопасности
        // Снять прогрев (не обязателен на клиенте, но мешать не будет)
        if (!MorphEngine.tryInvokeMethod(f, "setWarmup", new Class<?>[]{int.class}, 0)) {
            MorphEngine.tryInvokeMethod(f, "setWarmupTicks", new Class<?>[]{int.class}, 0);
        }
        MorphEngine.trySetField(f, "warmup", 0);

        // Гарантируем видимость и отсутствие физики
        try { f.setInvisible(false); } catch (Throwable ignored) {}
        try { f.setNoGravity(true); } catch (Throwable ignored) {}
        try { f.setOnGround(false); } catch (Throwable ignored) {}
        try { f.setVelocity(0, 0, 0); } catch (Throwable ignored) {}
    }

    static void applyBoatInput(BoatEntity boat) {
        var mc = MinecraftClient.getInstance();
        if (mc == null || mc.options == null) return;

        boolean f = mc.options.forwardKey.isPressed();
        boolean b = mc.options.backKey.isPressed();
        boolean l = mc.options.leftKey.isPressed();
        boolean r = mc.options.rightKey.isPressed();

        boolean leftPaddle  = (f && l) || (b && r) || f || l;
        boolean rightPaddle = (f && r) || (b && l) || f || r;

        if (!MorphEngine.tryInvokeMethod(boat, "setPaddleMovings", new Class<?>[]{boolean.class, boolean.class}, leftPaddle, rightPaddle)) {
            MorphEngine.tryInvokeMethod(boat, "setPaddleState", new Class<?>[]{boolean.class, boolean.class}, leftPaddle, rightPaddle);
        }
    }

    static void trySetBoatLocation(BoatEntity boat, BoatEntity.Location loc) {
        if (MorphEngine.tryInvokeMethod(boat, "setLocation", new Class<?>[]{BoatEntity.Location.class}, loc)) return;
        MorphEngine.trySetField(boat, "location", loc);
        MorphEngine.trySetField(boat, "status", loc);
    }
}