package seq.sequencermod.client.ui.preview;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RotationAxis;

/**
 * Универсальный рендер сущности в GUI (экран/меню), без добавления в мир.
 * Работает и для не-Living сущностей (включая AreaEffectCloud).
 */
public final class GuiEntityRenderer {
    private GuiEntityRenderer() {}

    public static void renderEntityInGui(MatrixStack matrices,
                                         int centerX, int centerY,
                                         float scale,
                                         float yawDeg,
                                         float pitchDeg,
                                         Entity entity,
                                         float tickDelta) {
        if (entity == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();

        matrices.push();
        RenderSystem.enableDepthTest();

        // Центруем и масштабируем
        matrices.translate(centerX, centerY, 50.0);
        matrices.scale(scale, scale, scale);

        // Поворачиваем, чтобы было видно сверху/сбоку
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitchDeg));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawDeg));

        // Отключаем тени, чтобы не было артефактов в GUI
        dispatcher.setRenderShadows(false);

        // Рендерим сущность на (0,0,0) в локальном пространстве матрицы
        try {
            dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0f, tickDelta, matrices, immediate, OverlayTexture.DEFAULT_UV);
        } catch (Throwable ignored) {
            // На всякий случай молча проглатываем, чтобы не ломать экран
        }
        immediate.draw();
        dispatcher.setRenderShadows(true);

        RenderSystem.disableDepthTest();
        matrices.pop();
    }
}