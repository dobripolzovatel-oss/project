package seq.sequencermod.net.client.morphs;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Унифицированный интерфейс клиентского рендерера морфа.
 * Реализация должна быть "пер-игрок" (содержать своё состояние).
 */
public interface ClientMorphRenderer {

    /**
     * Тик состояния рендерера (клиентский тик игрока).
     */
    default void tick(AbstractClientPlayerEntity player) {}

    /**
     * Основной рендер морфа вместо модели игрока.
     */
    void render(AbstractClientPlayerEntity player,
                float tickDelta,
                MatrixStack matrices,
                VertexConsumerProvider buffers,
                int light);

    /**
     * Доп. тень (опционально).
     */
    default void renderShadow(AbstractClientPlayerEntity player,
                              MatrixStack matrices,
                              VertexConsumerProvider buffers,
                              float tickDelta) {}
}