package seq.sequencermod.disguise.render.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import seq.sequencermod.disguise.render.DisguiseRenderable;

/**
 * Рендер предметной модели (включая блоки как Item форму) в GUI и как overlay.
 */
public final class BakedModelRenderable implements DisguiseRenderable {
    private final BakedModel model;
    private final ItemStack itemStack;
    private final float scale;

    private static final float OVERLAY_BASE_SCALE = 1.6f;

    private BakedModelRenderable(BakedModel model, ItemStack stack, float scale) {
        this.model = model;
        this.itemStack = stack;
        this.scale = scale;
    }

    public static BakedModelRenderable forItem(BakedModel model, ItemStack stack, float scale) {
        return new BakedModelRenderable(model, stack, scale);
    }

    @Override
    public void renderInGui(DrawContext ctx, int centerX, int bottomY, int size, float yawDeg, float pitchDeg, float tickDelta) {
        if (itemStack == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        MatrixStack matrices = ctx.getMatrices();
        matrices.push();

        matrices.translate(centerX, bottomY, 200);
        float s = (size / 24.0f) * scale;
        matrices.scale(s, -s, s);

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawDeg));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitchDeg));

        VertexConsumerProvider.Immediate buffers = mc.getBufferBuilders().getEntityVertexConsumers();

        mc.getItemRenderer().renderItem(
                itemStack,
                net.minecraft.client.render.model.json.ModelTransformationMode.FIXED,
                LightmapTextureManager.MAX_LIGHT_COORDINATE,
                OverlayTexture.DEFAULT_UV,
                matrices, buffers, mc.world, 0
        );

        buffers.draw();
        matrices.pop();
    }

    @Override
    public boolean renderAsOverlay(AbstractClientPlayerEntity player,
                                   float entityYaw, float headPitch, float tickDelta,
                                   MatrixStack matrices, VertexConsumerProvider buffers, int light) {
        if (itemStack == null) return false;

        MinecraftClient mc = MinecraftClient.getInstance();
        matrices.push();
        try {
            // Примерная позиция “на уровне груди/головы”
            matrices.translate(0.0, 1.2, 0.0);

            float clampedPitch = Math.max(-60f, Math.min(60f, headPitch));
            // Разворачиваем лицом вперёд
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-clampedPitch));

            float s = OVERLAY_BASE_SCALE * scale;
            matrices.scale(s, s, s);

            mc.getItemRenderer().renderItem(
                    itemStack,
                    net.minecraft.client.render.model.json.ModelTransformationMode.FIXED,
                    light,
                    OverlayTexture.DEFAULT_UV,
                    matrices, buffers, player.getWorld(), 0
            );

            return true;
        } finally {
            matrices.pop();
        }
    }
}