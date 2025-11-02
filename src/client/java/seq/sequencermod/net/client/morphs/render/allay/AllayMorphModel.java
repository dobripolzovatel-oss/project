package seq.sequencermod.net.client.morphs.render.allay;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.ModelWithArms;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * Модель морфа Аллея (реплика vanilla логики)
 */
public class AllayMorphModel extends EntityModel<LivingEntity> implements ModelWithArms {

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightWing;
    private final ModelPart leftWing;

    private AllayMorphState state;

    private static final Identifier TEXTURE =
            new Identifier("minecraft", "textures/entity/allay/allay.png");

    // Vanilla constants
    private static final float WING_PITCH_BASE = 0.43633232f;    // 25°
    private static final float WING_YAW_BASE   = 0.7853982f;     // 45°
    private static final float BODY_MAX_PITCH  = 0.7853982f;     // 45°
    private static final float ARM_PITCH_A     = -1.0471976f;    // -60°
    private static final float ARM_PITCH_B     = -1.134464f;     // -65°
    private static final float ARM_YAW_MAX     = 0.27925268f;    // ~16°
    private static final float COS_OFFSET      = 4.712389f;      // 3π/2
    private static final float MAX_G_FOR_ANIM  = 0.30f;

    public AllayMorphModel(ModelPart baked) {
        super(RenderLayer::getEntityTranslucent);

        ModelPart rootPart;
        try {
            rootPart = baked.getChild("root");
        } catch (Exception e) {
            rootPart = baked;
        }
        this.root = rootPart;
        this.head = safeGet(root, "head");
        this.body = safeGet(root, "body");
        this.rightArm = body != null ? safeGet(body, "right_arm") : null;
        this.leftArm  = body != null ? safeGet(body, "left_arm")  : null;
        this.rightWing = body != null ? safeGet(body, "right_wing") : null;
        this.leftWing  = body != null ? safeGet(body, "left_wing")  : null;
    }

    private static ModelPart safeGet(ModelPart parent, String name) {
        if (parent == null) return null;
        try { return parent.getChild(name); } catch (Exception ignored) { return null; }
    }

    public void applyState(AllayMorphState s) { this.state = s; }

    @Override
    public void setAngles(LivingEntity entity,
                          float limbAngle,
                          float limbDistance,
                          float animationProgress,
                          float relHeadYawDeg,
                          float headPitchDeg) {
        if (state == null) return;

        root.traverse().forEach(ModelPart::resetTransform);

        float g = Math.min(limbDistance, MAX_G_FOR_ANIM);
        float h = animationProgress;

        // Используем частичные тики из состояния (чтобы глобовый множитель не ломал интерполяцию q/spin)
        float fractional = MathHelper.clamp(state.partialTicks, 0.0f, 1.0f);

        float qArm   = MathHelper.lerp(fractional, state.armRaisePrev, state.armRaise);
        float tSpin  = MathHelper.lerp(fractional, state.spinProgressPrev, state.spinProgress);

        // vanilla: k = h * 20° + limbAngle
        float k = h * 20.0f * ((float)Math.PI / 180f) + limbAngle;

        float l = MathHelper.cos(k) * (float)Math.PI * 0.15f + g;
        float n = h * 9.0f * ((float)Math.PI / 180f);
        float o = Math.min(g / state.speedDivisor, 1.0f);
        float p = 1.0f - o;

        if (state.dancing) {
            float rDance = h * 8.0f * ((float)Math.PI / 180f) + g;
            float sRootRollBase = MathHelper.cos(rDance) * 16.0f * ((float)Math.PI / 180f);
            float headRollOsc   = MathHelper.cos(rDance) * 14.0f * ((float)Math.PI / 180f);
            float headYawOsc    = MathHelper.cos(rDance) * 30.0f * ((float)Math.PI / 180f);

            if (state.spinActive) root.yaw = (float)Math.PI * 4f * tSpin;
            root.roll = sRootRollBase * (1.0f - tSpin);

            if (head != null) {
                head.yaw  = headYawOsc * (1.0f - tSpin);
                head.roll = headRollOsc * (1.0f - tSpin);
            }
        } else {
            if (head != null) {
                head.pitch = headPitchDeg * ((float)Math.PI / 180f);
                head.yaw   = relHeadYawDeg * ((float)Math.PI / 180f);
            }
        }

        if (rightWing != null) { rightWing.pitch = WING_PITCH_BASE * (1.0f - o); rightWing.yaw = -WING_YAW_BASE + l; }
        if (leftWing  != null) { leftWing.pitch  = WING_PITCH_BASE * (1.0f - o); leftWing.yaw  =  WING_YAW_BASE - l; }
        if (body != null) body.pitch = o * BODY_MAX_PITCH;

        if (rightArm != null && leftArm != null) {
            float armPitchBlend = MathHelper.lerp(o, ARM_PITCH_A, ARM_PITCH_B);
            float rPitch = qArm * armPitchBlend;
            rightArm.pitch = rPitch;
            leftArm.pitch  = rPitch;

            float s = p * (1.0f - qArm);
            float t = 0.43633232f - MathHelper.cos(n + COS_OFFSET) * (float)Math.PI * 0.075f * s;
            leftArm.roll  = -t;
            rightArm.roll =  t;

            rightArm.yaw =  ARM_YAW_MAX * qArm;
            leftArm.yaw  = -ARM_YAW_MAX * qArm;
        }

        root.pivotY += MathHelper.cos(n) * 0.25f * p;
    }

    @Override
    public void setArmAngle(Arm arm, MatrixStack matrices) {
        // 1:1 с ваниллой — матрица «ладони», к которой привязывается предмет
        this.root.rotate(matrices);
        this.body.rotate(matrices);
        matrices.translate(0.0f, 0.0625f, 0.1875f);
        if (this.rightArm != null) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(this.rightArm.pitch));
        }
        matrices.scale(0.7f, 0.7f, 0.7f);
        matrices.translate(0.0625f, 0.0f, 0.0f);
    }

    public Identifier getTexture() { return TEXTURE; }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        root.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}