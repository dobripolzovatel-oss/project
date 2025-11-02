package seq.sequencermod.net.client.morphs.render.blaze;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Состояние морфа Blaze (клиент).
 * - Источник времени для модели: age (тикается строго в ClientTickEvents).
 * - Косметика 1:1 с ванилью: 2x LARGE_SMOKE/тик вокруг, случайный звук BURN 1/24.
 */
public class BlazeMorphState {
    public int age;

    public void reset(AbstractClientPlayerEntity player) {
        age = 0;
    }

    public void tick(AbstractClientPlayerEntity player) {
        age++;

        World w = player.getWorld();
        if (w == null || !w.isClient) return;

        // Редкий "burn" звук, если не молчим
        if (player.getRandom().nextInt(24) == 0 && !player.isSilent()) {
            w.playSound(
                    player.getX() + 0.5, player.getY() + 0.5, player.getZ() + 0.5,
                    SoundEvents.ENTITY_BLAZE_BURN,
                    player.getSoundCategory(),
                    1.0f + player.getRandom().nextFloat(),
                    player.getRandom().nextFloat() * 0.7f + 0.3f,
                    false
            );
        }

        // Дым, как в BlazeEntity.tickMovement()
        for (int i = 0; i < 2; i++) {
            double offX = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.6;
            double offZ = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.6;
            double y = player.getY() + player.getRandom().nextDouble() * 1.0 + 0.2;
            w.addParticle(
                    ParticleTypes.LARGE_SMOKE,
                    player.getX() + offX,
                    y,
                    player.getZ() + offZ,
                    0.0, 0.0, 0.0
            );
        }
    }
}