package seq.sequencermod.net.client.morphs.render.boat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import seq.sequencermod.mixin.accessor.EntityRenderDispatcherAccessor;
import seq.sequencermod.net.client.morphs.runtime.BoatMorphRuntime;

import java.util.EnumMap;
import java.util.Map;

/**
 * Кастомный рендер лодочного морфа.
 * Изменения:
 *  - НЕ рендерим water_patch вместе с корпусом (как vanilla).
 *  - Сглаживание переходов анимации весёл (через runtime smooth).
 */
public final class BoatMorphRenderer {

    private final Map<BoatEntity.Type, ModelPart> BOAT_MODELS = new EnumMap<>(BoatEntity.Type.class);
    private final Map<BoatEntity.Type, ModelPart> CHEST_BOAT_MODELS = new EnumMap<>(BoatEntity.Type.class);

    private static Identifier boatTex(BoatEntity.Type t) {
        return new Identifier("minecraft", "textures/entity/boat/" + t.getName() + ".png");
    }
    private static Identifier chestBoatTex(BoatEntity.Type t) {
        return new Identifier("minecraft", "textures/entity/chest_boat/" + t.getName() + ".png");
    }

    private ModelPart load(EntityModelLayer layer) {
        return MinecraftClient.getInstance().getEntityModelLoader().getModelPart(layer);
    }

    private ModelPart getRoot(BoatEntity.Type type, boolean chest) {
        Map<BoatEntity.Type, ModelPart> map = chest ? CHEST_BOAT_MODELS : BOAT_MODELS;
        return map.computeIfAbsent(type, t -> {
            EntityModelLayer layer = chest
                    ? EntityModelLayers.createChestBoat(t == BoatEntity.Type.BAMBOO ? BoatEntity.Type.OAK : t)
                    : EntityModelLayers.createBoat(t == BoatEntity.Type.BAMBOO ? BoatEntity.Type.OAK : t);
            return load(layer);
        });
    }

    public void render(AbstractClientPlayerEntity player,
                       BoatMorphRuntime rt,
                       BoatEntity.Type type,
                       boolean chest,
                       float tickDelta,
                       MatrixStack matrices,
                       VertexConsumerProvider buffers,
                       int light) {

        ModelPart root = getRoot(type, chest);
        if (root == null) return;

        // Подсчёт целевых углов и сглаживание
        applyPaddleAngles(root, rt, tickDelta);

        Identifier tex = chest ? chestBoatTex(type) : boatTex(type);
        VertexConsumer vc = buffers.getBuffer(RenderLayer.getEntityTranslucent(tex));

        float renderYaw = rt.getRenderYaw(tickDelta);

        matrices.push();
        matrices.translate(0.0f, 0.375f, 0.0f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - renderYaw));
        matrices.scale(-1.0f, -1.0f, 1.0f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0f));

        // Рендер только нужных частей (без water_patch)
        renderPart(root, "bottom", matrices, vc, light);
        renderPart(root, "back", matrices, vc, light);
        renderPart(root, "front", matrices, vc, light);
        renderPart(root, "right", matrices, vc, light);
        renderPart(root, "left", matrices, vc, light);
        renderPart(root, "left_paddle", matrices, vc, light);
        renderPart(root, "right_paddle", matrices, vc, light);
        if (chest) {
            renderPart(root, "chest_bottom", matrices, vc, light);
            renderPart(root, "chest_lid", matrices, vc, light);
            renderPart(root, "chest_lock", matrices, vc, light);
        }

        // Отдельно water_patch если игрок не полностью под водой
        if (!player.isSubmergedInWater()) {
            ModelPart water = safeChild(root, "water_patch");
            if (water != null) {
                VertexConsumer waterVC = buffers.getBuffer(RenderLayer.getWaterMask());
                water.render(matrices, waterVC, light, OverlayTexture.DEFAULT_UV);
            }
        }

        matrices.pop();

        // Тень
        try {
            var disp = (EntityRenderDispatcherAccessor) MinecraftClient.getInstance().getEntityRenderDispatcher();
            disp.sequencer$invokeRenderShadow(matrices, buffers, player, 0.8f, tickDelta, player.getWorld(), 1.0f);
        } catch (Throwable ignored) {}
    }

    private void renderPart(ModelPart root, String name, MatrixStack matrices, VertexConsumer vc, int light) {
        ModelPart p = safeChild(root, name);
        if (p != null) {
            p.render(matrices, vc, light, OverlayTexture.DEFAULT_UV);
        }
    }

    private void applyPaddleAngles(ModelPart root, BoatMorphRuntime rt, float tickDelta) {
        ModelPart left = safeChild(root, "left_paddle");
        ModelPart right = safeChild(root, "right_paddle");
        if (left == null || right == null) return;

        // Фазы
        float fL = rt.interpolatedPhase(0, tickDelta);
        float fR = rt.interpolatedPhase(1, tickDelta);

        // Целевые углы (как vanilla BoatEntityModel.setPaddleAngle):
        float leftPitchTarget = MathHelper.lerp((MathHelper.sin(-fL) + 1.0f) / 2.0f,
                -1.0471976f, -0.2617994f);
        float leftYawTarget = MathHelper.lerp((MathHelper.sin(-fL + 1.0f) + 1.0f) / 2.0f,
                -0.7853982f, 0.7853982f);

        float rightPitchTarget = MathHelper.lerp((MathHelper.sin(-fR) + 1.0f) / 2.0f,
                -1.0471976f, -0.2617994f);
        float rightYawRaw = MathHelper.lerp((MathHelper.sin(-fR + 1.0f) + 1.0f) / 2.0f,
                -0.7853982f, 0.7853982f);
        float rightYawTarget = (float)Math.PI - rightYawRaw;

        // Сглаживание (runtime хранит промежуточные smoothed значения)
        rt.smoothPaddle(0, leftPitchTarget, leftYawTarget);
        rt.smoothPaddle(1, rightPitchTarget, rightYawTarget);
        // После первого кадра фиксируем инициализацию
        rt.commitPaddlesInit();

        left.pitch = rt.getSmoothedPitch(0);
        left.yaw   = rt.getSmoothedYaw(0);
        left.roll  = 0.19634955f; // исходный roll

        right.pitch = rt.getSmoothedPitch(1);
        right.yaw   = rt.getSmoothedYaw(1);
        right.roll  = 0.19634955f;
    }

    private ModelPart safeChild(ModelPart root, String name) {
        try {
            return root.getChild(name);
        } catch (Throwable ignored) {
            return null;
        }
    }
}