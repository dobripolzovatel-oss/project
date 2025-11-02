package seq.sequencermod.mixin.morph;

import net.minecraft.entity.mob.EvokerFangsEntity;

/**
 * Вынесено из миксина, чтобы удовлетворить требование Mixin AP:
 * во внутренних классах миксина допускаются только @Mixin-классы.
 */
final class FangsOneShotState {
    EvokerFangsEntity fangs;
    long spawnWorldTick;
    long lastWorldTick = Long.MIN_VALUE;
    boolean finished;
}