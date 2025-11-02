package seq.sequencermod.net.client.morphs.render.axolotl;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.*;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import seq.sequencermod.mixin.accessor.EntityRenderDispatcherAccessor;
import seq.sequencermod.net.client.morphs.runtime.AxolotlMorphRuntime;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Кастомный рендерер аксолотля (морф).
 * ВКЛЮЧЕНО:
 *  - Исправленная ориентация (scale(-1,-1,1), поворот 180 - yaw).
 *  - Корректный вертикальный оффсет.
 *  - Разделение головы и корпуса (тело не повторяет pitch головы).
 *  - Плавные переходы между состояниями (idle / move / water idle / swim / play dead).
 *  - Кросс-версионное получение limbDistance.
 *
 * Для точной подстройки высоты правьте OFFSET_Y.
 */
@Environment(EnvType.CLIENT)
public final class AxolotlMorphRenderer {

    /* ---------------- Константы настройки ---------------- */

    // Model Layer
    public static final EntityModelLayer LAYER =
            new EntityModelLayer(new Identifier("sequencer", "axolotl_morph"), "main");

    // Вертикальный оффсет модели после масштабирования (подбирается опытно)
    private static final double OFFSET_Y = -1.38; // было -1.45 / -1.25 — этот ближе к поверхности

    // Сглаживание (0..1). Чем больше — тем быстрее догоняет целевую позу.
    private static final float SMOOTH_GENERAL = 0.25f;
    private static final float SMOOTH_WATER   = 0.23f;
    private static final float SMOOTH_PLAY_DEAD = 0.5f;
    private static final float SMOOTH_PIVOT  = 0.25f;

    // Ограничение угла головы по pitch (градусы)
    private static final float HEAD_PITCH_MIN = -60f;
    private static final float HEAD_PITCH_MAX =  60f;

    /* ---------------- Поля модели ---------------- */

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart topGills;
    private final ModelPart leftGills;
    private final ModelPart rightGills;
    private final ModelPart leftHindLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart tail;

    // Кэш (для возможного дебага)
    private final Map<String, PartAngles> cache = new HashMap<>();

    // Флаг первого кадра (чтобы не было плавного "вползания" из нулевой позы)
    private boolean firstFrame = true;

    // Текущая "состояние" (для возможного расширения)
    private MorphState lastState = MorphState.GROUND_IDLE;

    private enum MorphState {
        GROUND_IDLE, GROUND_MOVE, WATER_IDLE, WATER_MOVE, PLAY_DEAD
    }

    // Текстуры вариантов
    private static final Identifier[] VARIANT_TEXTURES = new Identifier[] {
            new Identifier("minecraft", "textures/entity/axolotl/axolotl_lucy.png"),
            new Identifier("minecraft", "textures/entity/axolotl/axolotl_wild.png"),
            new Identifier("minecraft", "textures/entity/axolotl/axolotl_gold.png"),
            new Identifier("minecraft", "textures/entity/axolotl/axolotl_cyan.png"),
            new Identifier("minecraft", "textures/entity/axolotl/axolotl_blue.png")
    };

    /* ---------------- Конструктор / bake ---------------- */

    public AxolotlMorphRenderer(ModelPart root) {
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.topGills = this.head.getChild("top_gills");
        this.leftGills = this.head.getChild("left_gills");
        this.rightGills = this.head.getChild("right_gills");
        this.leftHindLeg = this.body.getChild("left_hind_leg");
        this.rightHindLeg = this.body.getChild("right_hind_leg");
        this.leftFrontLeg = this.body.getChild("left_front_leg");
        this.rightFrontLeg = this.body.getChild("right_front_leg");
        this.tail = this.body.getChild("tail");
    }

    public static TexturedModelData createModelData() {
        ModelData md = new ModelData();
        ModelPartData root = md.getRoot();

        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create()
                        .uv(0, 11).cuboid(-4F, -2F, -9F, 8F, 4F, 10F)
                        .uv(2, 17).cuboid(0F, -3F, -8F, 0F, 5F, 9F),
                ModelTransform.pivot(0F, 20F, 5F));

        ModelPartData head = body.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-4F, -3F, -5F, 8F, 5F, 5F),
                ModelTransform.pivot(0F, 0F, -9F));

        head.addChild("top_gills",
                ModelPartBuilder.create().uv(3, 37).cuboid(-4F, -3F, 0F, 8F, 3F, 0F),
                ModelTransform.pivot(0F, -3F, -1F));
        head.addChild("left_gills",
                ModelPartBuilder.create().uv(0, 40).cuboid(-3F, -5F, 0F, 3F, 7F, 0F),
                ModelTransform.pivot(-4F, 0F, -1F));
        head.addChild("right_gills",
                ModelPartBuilder.create().uv(11, 40).cuboid(0F, -5F, 0F, 3F, 7F, 0F),
                ModelTransform.pivot(4F, 0F, -1F));

        body.addChild("right_hind_leg",
                ModelPartBuilder.create().uv(2, 13).cuboid(-2F, 0F, 0F, 3F, 5F, 0F),
                ModelTransform.pivot(-3.5F, 1F, -1F));
        body.addChild("left_hind_leg",
                ModelPartBuilder.create().uv(2, 13).cuboid(-1F, 0F, 0F, 3F, 5F, 0F),
                ModelTransform.pivot(3.5F, 1F, -1F));
        body.addChild("right_front_leg",
                ModelPartBuilder.create().uv(2, 13).cuboid(-2F, 0F, 0F, 3F, 5F, 0F),
                ModelTransform.pivot(-3.5F, 1F, -8F));
        body.addChild("left_front_leg",
                ModelPartBuilder.create().uv(2, 13).cuboid(-1F, 0F, 0F, 3F, 5F, 0F),
                ModelTransform.pivot(3.5F, 1F, -8F));

        body.addChild("tail",
                ModelPartBuilder.create().uv(2, 19).cuboid(0F, -3F, 0F, 0F, 5F, 12F),
                ModelTransform.pivot(0F, 0F, 1F));

        return TexturedModelData.of(md, 64, 64);
    }

    /* ---------------- Главный рендер ---------------- */

    public void render(AbstractClientPlayerEntity player,
                       float tickDelta,
                       MatrixStack matrices,
                       VertexConsumerProvider buffers,
                       int light) {

        AxolotlMorphRuntime rt = AxolotlMorphRuntime.get(player.getUuid(), true);
        int variant = Math.min(rt.getVariant(), VARIANT_TEXTURES.length - 1);
        Identifier tex = VARIANT_TEXTURES[variant];

        MinecraftClient mc = MinecraftClient.getInstance();
        VertexConsumer vc = buffers.getBuffer(RenderLayer.getEntityTranslucent(tex));

        matrices.push();

        float bodyYawNow = MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - bodyYawNow));
        matrices.scale(-1f, -1f, 1f);
        matrices.translate(0.0, -1.45, 0.0);

        float limb = computeLimbAmount(player, tickDelta);
        float animTime = player.age + tickDelta;

        float headYawAbs  = MathHelper.lerpAngleDegrees(tickDelta, player.prevHeadYaw, player.headYaw);
        float headYawRel  = MathHelper.wrapDegrees(headYawAbs - bodyYawNow);
        float headPitchDeg = MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());
        headPitchDeg = MathHelper.clamp(headPitchDeg, HEAD_PITCH_MIN, HEAD_PITCH_MAX);

        float headYaw    = headYawRel * ((float)Math.PI / 180f);
        float headPitch  = headPitchDeg * ((float)Math.PI / 180f);

        // Выявление состояния
        boolean wet      = player.isSubmergedInWater() || player.isTouchingWater();
        boolean moving   = limb > 0.01f;
        boolean deadPlay = rt.isPlayingDead();

        MorphState state;
        if (deadPlay) {
            state = MorphState.PLAY_DEAD;
        } else if (wet) {
            state = moving ? MorphState.WATER_MOVE : MorphState.WATER_IDLE;
        } else {
            state = moving ? MorphState.GROUND_MOVE : MorphState.GROUND_IDLE;
        }

        // Первый кадр — жестко установить целевые значения, чтобы не дергалось из нуля
        if (firstFrame) {
            hardResetPose(headYaw, headPitch);
            firstFrame = false;
            lastState = state;
        }

        // Плавно изменяем pivotY в некоторых анимациях — базовый 20
        body.pivotY = lerpScalar(body.pivotY, 20f, SMOOTH_PIVOT);

        switch (state) {
            case PLAY_DEAD      -> animatePlayDead(headYaw);
            case WATER_MOVE     -> animateSwimMoving(animTime, headYaw, headPitch);
            case WATER_IDLE     -> animateSwimIdle(animTime, headYaw, headPitch);
            case GROUND_MOVE    -> animateGroundMoving(animTime, headYaw, headPitch);
            case GROUND_IDLE    -> animateGroundIdle(animTime, headYaw, headPitch);
        }

        lastState = state;

        cacheAngles();

        body.render(matrices, vc, light, OverlayTexture.DEFAULT_UV);
        matrices.pop();

        // Тень
        try {
            EntityRenderDispatcherAccessor disp = (EntityRenderDispatcherAccessor) mc.getEntityRenderDispatcher();
            disp.sequencer$invokeRenderShadow(matrices, buffers, player, 0.35f, tickDelta, player.getWorld(), 1.0f);
        } catch (Throwable ignored) {}
    }

    /* ---------------- Анимации (сглаженные) ---------------- */

    private void animateGroundIdle(float t, float headYaw, float headPitch) {
        float f = t * 0.09f;
        float s = MathHelper.sin(f);
        float c = MathHelper.cos(f);
        float i = s * s - 2 * s;
        float j = c * c - 3 * s;

        // Тело в нуле (без следования pitch головы)
        smooth(body, 0f, 0f, 0f, SMOOTH_GENERAL);

        // Голова с легким «дыханием»
        smooth(head, headPitch + (-0.09f * i), headYaw, -0.2f, SMOOTH_GENERAL);

        smooth(tail, 0f, 0.05f * i + headYaw * 0.1f, 0f, SMOOTH_GENERAL);

        smooth(topGills, 0.6f + 0.05f * j, 0f, 0f, SMOOTH_GENERAL);
        float gYaw = -(0.6f + 0.05f * j);
        smooth(leftGills, 0f, gYaw, 0f, SMOOTH_GENERAL);
        smooth(rightGills, 0f, -gYaw, 0f, SMOOTH_GENERAL);

        poseLegsIdleGroundSmooth();
    }

    private void animateGroundMoving(float t, float headYaw, float headPitch) {
        float f = t * 0.11f;
        float cos = MathHelper.cos(f);
        float hVar = (cos * cos - 2 * cos) / 5f;
        float side = 0.7f * cos;

        smooth(body, 0f, 0f, 0f, SMOOTH_GENERAL);
        smooth(head, headPitch, headYaw + 0.09f * cos, 0f, SMOOTH_GENERAL);

        float gPitch = 0.6f - 0.08f * (cos * cos + 2 * MathHelper.sin(f));
        smooth(topGills, gPitch, 0f, 0f, SMOOTH_GENERAL);
        smooth(leftGills, 0f, -gPitch, 0f, SMOOTH_GENERAL);
        smooth(rightGills, 0f, gPitch, 0f, SMOOTH_GENERAL);

        smooth(leftHindLeg, 0.9424779f, 1.5f - hVar, -0.1f, SMOOTH_GENERAL);
        smooth(leftFrontLeg, 1.0995574f, 1.5707964f - side, 0f, SMOOTH_GENERAL);
        mirrorRightLegsSmooth(leftHindLeg, leftFrontLeg);

        smooth(tail, 0f, 0.2f * MathHelper.sin(f * 1.2f), 0f, SMOOTH_GENERAL);
    }

    private void animateSwimIdle(float t, float headYaw, float headPitch) {
        float f = t * 0.075f;
        float cos = MathHelper.cos(f);
        float sin = MathHelper.sin(f);

        float targetBodyPitch = -0.15f + 0.075f * cos;
        body.pitch = lerpScalar(body.pitch, targetBodyPitch, SMOOTH_WATER);
        float targetPivotY = 20f - sin * 0.15f;
        body.pivotY = lerpScalar(body.pivotY, targetPivotY, SMOOTH_WATER);

        smooth(head, -body.pitch + headPitch * 0.2f, headYaw, 0f, SMOOTH_WATER);

        float g = 0.2f * cos;
        smooth(topGills, g, 0f, 0f, SMOOTH_WATER);
        smooth(leftGills, 0f, -0.3f * cos - 0.19f, 0f, SMOOTH_WATER);
        smooth(rightGills, 0f, 0.3f * cos + 0.19f, 0f, SMOOTH_WATER);

        poseLegsFoldedWaterSmooth();
        smooth(tail, 0f, 0.5f * cos, 0f, SMOOTH_WATER);
    }

    private void animateSwimMoving(float t, float headYaw, float headPitch) {
        float f = t * 0.33f;
        float sin = MathHelper.sin(f);
        float cos = MathHelper.cos(f);

        float targetBodyPitch = 0.25f * sin + headPitch * 0.15f;
        body.pitch = lerpScalar(body.pitch, targetBodyPitch, SMOOTH_WATER);
        float targetPivotY = 20f - 0.45f * cos;
        body.pivotY = lerpScalar(body.pivotY, targetPivotY, SMOOTH_WATER);

        float targetHeadPitch = -0.6f * sin * 0.2f + headPitch * 0.8f;
        smooth(head, targetHeadPitch, headYaw, 0f, SMOOTH_WATER);

        float gillPitch = -0.5f * sin - 0.8f;
        smooth(topGills, gillPitch, 0f, 0f, SMOOTH_WATER);
        smooth(leftGills, 0f, 0.3f * sin + 0.9f, 0f, SMOOTH_WATER);
        smooth(rightGills, 0f, -0.3f * sin - 0.9f, 0f, SMOOTH_WATER);

        poseLegsSwimSmooth();
        smooth(tail, 0f, 0.35f * MathHelper.cos(f * 0.9f) + 0.1f * headYaw, 0f, SMOOTH_WATER);
    }

    private void animatePlayDead(float headYaw) {
        // Ноги
        smooth(leftHindLeg, 1.4137167f, 1.0995574f, 0.7853982f, SMOOTH_PLAY_DEAD);
        smooth(leftFrontLeg, 0.7853982f, 2.042035f, 0f, SMOOTH_PLAY_DEAD);
        // зеркальные
        smooth(rightHindLeg, 1.4137167f, -1.0995574f, -0.7853982f, SMOOTH_PLAY_DEAD);
        smooth(rightFrontLeg, 0.7853982f, -2.042035f, 0f, SMOOTH_PLAY_DEAD);

        // Корпус
        body.pitch = lerpScalar(body.pitch, -0.15f, SMOOTH_PLAY_DEAD);
        body.roll  = lerpScalar(body.roll, 0.35f, SMOOTH_PLAY_DEAD);

        // Голова
        smooth(head, 0f, headYaw, 0f, SMOOTH_PLAY_DEAD);

        // Хвост / жабры плавно замирают
        smooth(tail, 0f, 0f, 0f, SMOOTH_PLAY_DEAD);
        smooth(topGills, 0f, 0f, 0f, SMOOTH_PLAY_DEAD);
        smooth(leftGills, 0f, 0f, 0f, SMOOTH_PLAY_DEAD);
        smooth(rightGills, 0f, 0f, 0f, SMOOTH_PLAY_DEAD);
    }

    /* ---------------- Поза ног (сглаженно) ---------------- */

    private void poseLegsIdleGroundSmooth() {
        smooth(leftHindLeg, 1.1f, 1.0f, 0f, SMOOTH_GENERAL);
        smooth(leftFrontLeg, 0.8f, 2.3f, -0.5f, SMOOTH_GENERAL);
        mirrorRightLegsSmooth(leftHindLeg, leftFrontLeg);
    }

    private void poseLegsFoldedWaterSmooth() {
        smooth(leftHindLeg, 2.3561945f, 0.47123894f, 1.7278761f, SMOOTH_WATER);
        smooth(leftFrontLeg, 0.7853982f, 2.042035f, 0f, SMOOTH_WATER);
        mirrorRightLegsSmooth(leftHindLeg, leftFrontLeg);
    }

    private void poseLegsSwimSmooth() {
        smooth(leftHindLeg, 1.8849558f, 0f, 1.5707964f, SMOOTH_WATER);
        smooth(leftFrontLeg, 1.8849558f, -0.1f, 1.5707964f, SMOOTH_WATER);
        mirrorRightLegsSmooth(leftHindLeg, leftFrontLeg);
    }

    private void mirrorRightLegsSmooth(ModelPart leftHind, ModelPart leftFront) {
        smooth(rightHindLeg, leftHind.pitch, -leftHind.yaw, -leftHind.roll, SMOOTH_GENERAL);
        smooth(rightFrontLeg, leftFront.pitch, -leftFront.yaw, -leftFront.roll, SMOOTH_GENERAL);
    }

    /* ---------------- Сглаживание / утилиты ---------------- */

    private void hardResetPose(float headYaw, float headPitch) {
        body.pitch = body.yaw = body.roll = 0f;
        head.setAngles(headPitch, headYaw, 0f);
        tail.setAngles(0,0,0);
        topGills.setAngles(0,0,0);
        leftGills.setAngles(0,0,0);
        rightGills.setAngles(0,0,0);
        leftFrontLeg.setAngles(0,0,0);
        rightFrontLeg.setAngles(0,0,0);
        leftHindLeg.setAngles(0,0,0);
        rightHindLeg.setAngles(0,0,0);
        body.pivotY = 20f;
    }

    private void smooth(ModelPart part, float targetPitch, float targetYaw, float targetRoll, float speed) {
        part.pitch = part.pitch + (targetPitch - part.pitch) * speed;
        part.yaw   = part.yaw   + (targetYaw   - part.yaw)   * speed;
        part.roll  = part.roll  + (targetRoll  - part.roll)  * speed;
    }

    private float lerpScalar(float current, float target, float alpha) {
        return current + (target - current) * alpha;
    }

    private float computeLimbAmount(AbstractClientPlayerEntity p, float tickDelta) {
        // 1) Новые версии (limb animator)
        try {
            Method mGetLA = p.getClass().getMethod("getLimbAnimator");
            Object la = mGetLA.invoke(p);
            if (la != null) {
                try {
                    Method mGetSpeed = la.getClass().getMethod("getSpeed");
                    Object v = mGetSpeed.invoke(la);
                    if (v instanceof Number) return ((Number) v).floatValue();
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        // 2) Старые Yarn
        try {
            Class<?> c = p.getClass();
            while (c != null) {
                try {
                    Field f = c.getDeclaredField("limbDistance");
                    f.setAccessible(true);
                    return f.getFloat(p);
                } catch (NoSuchFieldException nf) {
                    c = c.getSuperclass();
                }
            }
        } catch (Throwable ignored) {}
        // 3) Фоллбэк — горизонтальная скорость
        var v = p.getVelocity();
        return (float) Math.hypot(v.x, v.z);
    }

    private record PartAngles(float pitch, float yaw, float roll) {}
    private void put(String k, ModelPart p) { cache.put(k, new PartAngles(p.pitch, p.yaw, p.roll)); }
    private void cacheAngles() {
        put("body", body); put("head", head); put("tail", tail);
        put("top_gills", topGills); put("left_gills", leftGills); put("right_gills", rightGills);
        put("left_hind_leg", leftHindLeg); put("right_hind_leg", rightHindLeg);
        put("left_front_leg", leftFrontLeg); put("right_front_leg", rightFrontLeg);
    }
}