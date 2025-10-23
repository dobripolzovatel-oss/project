package seq.sequencermod.disguise.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Минимальный клиентский интерфейс рендерера disguise.
 * В текущей сборке overlay может быть отключён — дефолтные реализации делают no-op.
 */
public interface DisguiseRenderable {
    /**
     * Рендер превью в GUI.
     */
    default void renderInGui(DrawContext ctx,
                             int centerX, int bottomY, int size,
                             float yawDeg, float pitchDeg, float tickDelta) {
        // no-op
    }

    /**
     * Рендер как overlay (поверх игрока). Возвращает true, если отрисовано.
     */
    default boolean renderAsOverlay(AbstractClientPlayerEntity player,
                                    float entityYaw, float headPitch, float tickDelta,
                                    MatrixStack matrices, VertexConsumerProvider buffers, int light) {
        return false;
    }
}