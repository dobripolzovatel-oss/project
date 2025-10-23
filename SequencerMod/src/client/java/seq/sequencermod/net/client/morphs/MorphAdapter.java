package seq.sequencermod.net.client.morphs;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;

import java.util.UUID;

public interface MorphAdapter<E extends Entity> {
    // Создание зеркала для игрока
    default void onCreate(E e, UUID playerId) {}

    // Нужно ли тикать mirror.tick() (по умолчанию — да)
    default boolean shouldClientTick(E e) { return true; }

    // Кастомный тик до/после стандартного (опционально)
    default void onClientTick(E e) {}
    default void afterClientTick(E e) {}

    // Синхронизация состояния зеркала от игрока
    void syncFromPlayer(AbstractClientPlayerEntity player, E e);

    // Кастом до/после рендера (опционально)
    default void beforeRender(AbstractClientPlayerEntity player, E e, float tickDelta) {}
    default void afterRender(AbstractClientPlayerEntity player, E e, float tickDelta) {}
}