package seq.sequencermod.net.client.morphs.render.phantom;

import net.minecraft.client.network.AbstractClientPlayerEntity;

/**
 * Состояние морфа Фантома.
 * Здесь только смещение взмаха крыльев (как getWingFlapTickOffset у фантома).
 */
public class PhantomMorphState {

    // Аналог PhantomEntity#getWingFlapTickOffset()
    public int wingFlapOffsetTicks = 0;

    public void reset(AbstractClientPlayerEntity player) {
        // Детерминированный оффсет по игроку, чтобы "разнофазность" была стабильной
        // Берём несколько младших битов из id/uuid
        int seed = player.getUuid().hashCode();
        // Ванильный множитель 7.448451°/тик — оффсет можно держать в 0..19 тиков
        this.wingFlapOffsetTicks = Math.floorMod(seed, 20);
    }

    public void tick(AbstractClientPlayerEntity player) {
        // Ничего не надо — вся анимация в модели от age+tickDelta
    }
}