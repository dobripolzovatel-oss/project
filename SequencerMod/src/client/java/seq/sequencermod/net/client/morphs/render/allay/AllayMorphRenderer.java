package seq.sequencermod.net.client.morphs.render.allay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * Рендер морфа Аллея.
 *  - Повороты тела/головы = как у LivingEntityRenderer.
 *  - limbAngle/limbDistance = ванильный LimbAnimator (pos/speed).
 *  - animationProgress = age + tickDelta.
 *  - Предметы в руках — как в HeldItemFeatureRenderer.
 *
 * Дополнено:
 *  - item_thrown: проигрывается при нажатии клавиши выброса (Q) у локального игрока,
 *    если в главной руке есть предмет (с небольшим локальным кулдауном).
 */
public class AllayMorphRenderer {

    private final AllayMorphModel model;
    private final AllayMorphState state;

    private static final float VERTICAL_OFFSET = 1.35f;
    private static final float GLOBAL_SCALE    = 1.0f;

    // Локальный кулдаун для звука броска
    private int throwSoundCooldown = 0;

    public AllayMorphRenderer(EntityModelLoader loader) {
        this.model = new AllayMorphModel(loader.getModelPart(EntityModelLayers.ALLAY));
        this.state = new AllayMorphState();
    }

    public AllayMorphState getState() {
        return state;
    }

    public void reset(AbstractClientPlayerEntity player) {
        state.reset(player);
        throwSoundCooldown = 0;
    }

    public void tick(AbstractClientPlayerEntity player) {
        // item_thrown — только для локального игрока
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean isLocal = mc != null && mc.player == player;
        if (isLocal && mc.options != null && throwSoundCooldown == 0) {
            if (mc.options.dropKey.wasPressed() && !player.getMainHandStack().isEmpty()) {
                playAt(player, SoundEvents.ENTITY_ALLAY_ITEM_THROWN, 1.0f, 1.0f);
                throwSoundCooldown = 4; // ~200мс при 20 TPS
            }
        }
        if (throwSoundCooldown > 0) throwSoundCooldown--;

        state.autoDetectDance(player);
        state.tick(player);
    }

    public void render(AbstractClientPlayerEntity player,
                       float tickDelta,
                       MatrixStack matrices,
                       VertexConsumerProvider providers,
                       int light) {

        // Прокинем partialTicks в состояние для корректной интерполяции
        state.partialTicks = MathHelper.clamp(tickDelta, 0.0f, 1.0f);

        model.applyState(state);
        matrices.push();

        matrices.translate(0.0, VERTICAL_OFFSET, 0.0);

        // Интерполяция углов (как LivingEntityRenderer)
        float bodyYawInterp = MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw);
        float headYawInterp = MathHelper.lerpAngleDegrees(tickDelta, player.prevHeadYaw, player.headYaw);
        float relHeadYawDeg = MathHelper.wrapDegrees(headYawInterp - bodyYawInterp);
        float headPitchInterp = MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());

        // Поворот корня по yaw
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - bodyYawInterp));

        // Инверсия + масштаб
        matrices.scale(-1.0f, -1.0f, 1.0f);
        matrices.scale(GLOBAL_SCALE, GLOBAL_SCALE, GLOBAL_SCALE);

        // Время анимации (h)
        float animationProgress = (state.age + tickDelta) * state.globalSpeedMul;

        // Ванильные limb-параметры
        float g = state.getLimbSpeed(tickDelta); // limbDistance
        float limbAngle = state.getLimbPos(tickDelta); // limbAngle

        model.setAngles(
                player,
                limbAngle,
                g,
                animationProgress,
                relHeadYawDeg,
                headPitchInterp
        );

        VertexConsumer vc = providers.getBuffer(model.getLayer(model.getTexture()));
        model.render(matrices, vc, light, OverlayTexture.DEFAULT_UV, 1f,1f,1f,1f);

        // Предметы в руках — 1:1 как HeldItemFeatureRenderer (1.20.1)
        renderHeldItemsVanillaFeature(player, matrices, providers, light);

        matrices.pop();
    }

    private void renderHeldItemsVanillaFeature(AbstractClientPlayerEntity player,
                                               MatrixStack matrices,
                                               VertexConsumerProvider providers,
                                               int light) {
        boolean mainRight = (player.getMainArm() == Arm.RIGHT);

        ItemStack leftStack  = mainRight ? player.getOffHandStack() : player.getMainHandStack();
        ItemStack rightStack = mainRight ? player.getMainHandStack() : player.getOffHandStack();

        if (leftStack.isEmpty() && rightStack.isEmpty()) return;

        matrices.push();

        // Правая рука
        renderOneHandVanilla(player, rightStack, ModelTransformationMode.THIRD_PERSON_RIGHT_HAND, Arm.RIGHT, matrices, providers, light);
        // Левая рука
        renderOneHandVanilla(player, leftStack, ModelTransformationMode.THIRD_PERSON_LEFT_HAND, Arm.LEFT, matrices, providers, light);

        matrices.pop();
    }

    private void renderOneHandVanilla(AbstractClientPlayerEntity player,
                                      ItemStack stack,
                                      ModelTransformationMode mode,
                                      Arm arm,
                                      MatrixStack matrices,
                                      VertexConsumerProvider providers,
                                      int light) {
        if (stack.isEmpty()) return;

        matrices.push();

        // 1) Матрица ладони (AllayEntityModel.setArmAngle)
        model.setArmAngle(arm, matrices);

        // 2) Доп. повороты
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));

        // 3) Сдвиг относительно ладони
        boolean left = (arm == Arm.LEFT);
        matrices.translate((left ? -1.0 : 1.0) / 16.0, 0.125, -0.625);

        // 4) Рендер предмета
        MinecraftClient.getInstance()
                .getItemRenderer()
                .renderItem(
                        player,
                        stack,
                        mode,
                        left,
                        matrices,
                        providers,
                        player.getWorld(),
                        light,
                        OverlayTexture.DEFAULT_UV,
                        0
                );

        matrices.pop();
    }

    // Тень (как раньше)
    public void renderShadow(AbstractClientPlayerEntity player,
                             MatrixStack matrices,
                             VertexConsumerProvider providers,
                             float tickDelta) {
        if (!MinecraftClient.getInstance().options.getEntityShadows().getValue()) return;
        EntityRenderDispatcher dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        float radius = 0.4f;
        float opacity = 1.0f;
        try {
            ((seq.sequencermod.mixin.accessor.EntityRenderDispatcherAccessor) dispatcher)
                    .sequencer$invokeRenderShadow(matrices, providers, player, radius, tickDelta, player.getWorld(), opacity);
        } catch (Throwable ignored) {}
    }

    private static void playAt(AbstractClientPlayerEntity p, SoundEvent snd, float vol, float pitch) {
        if (p == null || p.getWorld() == null) return;
        p.getWorld().playSound(p.getX(), p.getY(), p.getZ(), snd, SoundCategory.NEUTRAL, vol, pitch, false);
    }
}