package seq.sequencermod.net.client.morphs.render.blaze;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.model.BlazeEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import seq.sequencermod.mixin.accessor.EntityRenderDispatcherAccessor;

/**
 * Рендер морфа Blaze (render-only), 1:1 с ванилью:
 *  - Модель: BlazeEntityModel(EntityModelLayers.BLAZE).
 *  - Текстура: textures/entity/blaze.png.
 *  - Яркость: fullbright (эквивалент getBlockLight=15).
 *  - Анимации стержней — строго из модели по h = age + tickDelta.
 *  - Повороты головы/тела — как в LivingEntityRenderer.
 */
public class BlazeMorphRenderer {

    private static final Identifier TEXTURE = new Identifier("minecraft", "textures/entity/blaze.png");
    private static final float VERTICAL_OFFSET = 1.6f; // подгон высоты под ванильный рост
    private static final float GLOBAL_SCALE = 1.0f;
    private static final int FULL_BRIGHT = 0x00F000F0;

    private final BlazeEntityModel<net.minecraft.entity.Entity> model;
    private final BlazeMorphState state = new BlazeMorphState();

    public BlazeMorphRenderer(EntityModelLoader loader) {
        this.model = new BlazeEntityModel<>(loader.getModelPart(EntityModelLayers.BLAZE));
    }

    public BlazeMorphState getState() { return state; }

    public void reset(AbstractClientPlayerEntity player) { state.reset(player); }

    public void tick(AbstractClientPlayerEntity player) { state.tick(player); }

    public void render(AbstractClientPlayerEntity player,
                       float tickDelta,
                       MatrixStack matrices,
                       VertexConsumerProvider providers,
                       int ignoredLight) {

        matrices.push();

        // Ставим в позицию игрока (получаемся «в нём»)
        matrices.translate(0.0, VERTICAL_OFFSET, 0.0);

        // Интерполяции углов как в LivingEntityRenderer
        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw);
        float headYaw = MathHelper.lerpAngleDegrees(tickDelta, player.prevHeadYaw, player.headYaw);
        float relHeadYawDeg = MathHelper.wrapDegrees(headYaw - bodyYaw);
        float headPitch = MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());

        // Поворот корня
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - bodyYaw));

        // Инверсия и масштаб
        matrices.scale(-1.0f, -1.0f, 1.0f);
        matrices.scale(GLOBAL_SCALE, GLOBAL_SCALE, GLOBAL_SCALE);

        // Время модели: age + tickDelta
        float animationProgress = state.age + tickDelta;

        // setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch)
        // limbAngle/limbDistance для Blaze не используются — подаём 0
        this.model.setAngles(
                player,
                0.0f, 0.0f,
                animationProgress,
                relHeadYawDeg,
                headPitch
        );

        VertexConsumer vc = providers.getBuffer(this.model.getLayer(TEXTURE));
        this.model.render(matrices, vc, FULL_BRIGHT, OverlayTexture.DEFAULT_UV, 1f, 1f, 1f, 1f);

        matrices.pop();
    }

    // Тень (опционально)
    public void renderShadow(AbstractClientPlayerEntity player,
                             MatrixStack matrices,
                             VertexConsumerProvider providers,
                             float tickDelta) {
        if (!MinecraftClient.getInstance().options.getEntityShadows().getValue()) return;
        EntityRenderDispatcher dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        float radius = 0.5f; // как в BlazeEntityRenderer (shadow size в super(..., 0.5f))
        float opacity = 1.0f;
        try {
            ((EntityRenderDispatcherAccessor) dispatcher)
                    .sequencer$invokeRenderShadow(matrices, providers, player, radius, tickDelta, player.getWorld(), opacity);
        } catch (Throwable ignored) {}
    }
}