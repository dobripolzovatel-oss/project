package seq.sequencermod.net.client.morphs.render.camel;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public final class CamelMorphRenderer {

    private final CamelMorphModel model;
    private final CamelMorphStateImpl state;

    private static final float VERTICAL_OFFSET = 1.35f;
    private static final float GLOBAL_SCALE    = 1.0f;

    public CamelMorphRenderer(EntityModelLoader loader) {
        ModelPart baked = loader.getModelPart(EntityModelLayers.CAMEL);
        this.model = new CamelMorphModel(baked);
        this.state = new CamelMorphStateImpl();
    }

    public CamelMorphState getState() { return state; }

    public void tick(AbstractClientPlayerEntity player) {
        state.tick(player);
    }

    public void render(AbstractClientPlayerEntity player,
                       float tickDelta,
                       MatrixStack matrices,
                       VertexConsumerProvider providers,
                       int light) {
        state.setPartialTicks(MathHelper.clamp(tickDelta, 0f, 1f));
        model.applyState(state);

        matrices.push();
        matrices.translate(0.0, VERTICAL_OFFSET, 0.0);

        float bodyYawInterp = MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw);
        float headYawInterp = MathHelper.lerpAngleDegrees(tickDelta, player.prevHeadYaw, player.headYaw);
        float relHeadYawDeg = MathHelper.wrapDegrees(headYawInterp - bodyYawInterp);
        float headPitchInterp = MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - bodyYawInterp));
        matrices.scale(-1.0f, -1.0f, 1.0f);
        matrices.scale(GLOBAL_SCALE, GLOBAL_SCALE, GLOBAL_SCALE);

        float animationProgress = (state.age() + tickDelta) * state.globalSpeedMul();
        float limbDistance = state.getLimbSpeed(tickDelta);
        float limbAngle = state.getLimbPos(tickDelta);

        model.setAngles(
                player,
                limbAngle,
                limbDistance,
                animationProgress,
                relHeadYawDeg,
                headPitchInterp
        );

        // берем текстуру из state
        VertexConsumer vc = providers.getBuffer(model.getLayer(state.getSkinTexture()));
        model.render(matrices, vc, light, OverlayTexture.DEFAULT_UV, 1f,1f,1f,1f);

        matrices.pop();
    }

    public void renderShadow(AbstractClientPlayerEntity player,
                             MatrixStack matrices,
                             VertexConsumerProvider providers,
                             float tickDelta) {
        if (!MinecraftClient.getInstance().options.getEntityShadows().getValue()) return;
        EntityRenderDispatcher dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        float radius = 0.7f;
        float opacity = 1.0f;
        try {
            ((seq.sequencermod.mixin.accessor.EntityRenderDispatcherAccessor) dispatcher)
                    .sequencer$invokeRenderShadow(matrices, providers, player, radius, tickDelta, player.getWorld(), opacity);
        } catch (Throwable ignored) {}
    }
}