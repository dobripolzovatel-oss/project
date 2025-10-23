package seq.sequencermod.net.client.morphs.render.camel;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.animation.CamelAnimations;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class CamelMorphModel extends SinglePartEntityModel<LivingEntity> {

    private final ModelPart root;
    private final ModelPart head;

    // vanilla extras
    private final ModelPart[] saddleAndBridle;
    private final ModelPart[] reins;

    private CamelMorphState state;

    public CamelMorphModel(ModelPart bakedRoot) {
        super(RenderLayer::getEntityCutoutNoCull);
        this.root = bakedRoot;

        ModelPart body = safeGet(root, EntityModelPartNames.BODY);
        this.head = body != null ? safeGet(body, EntityModelPartNames.HEAD) : null;

        // найти доп. части
        ModelPart saddle = null, bridle = null, reinsPart = null;
        if (body != null) {
            try { saddle = body.getChild("saddle"); } catch (Throwable ignored) {}
            if (head != null) {
                try { bridle = head.getChild("bridle"); } catch (Throwable ignored) {}
                try { reinsPart = head.getChild("reins"); } catch (Throwable ignored) {}
            }
        }
        this.saddleAndBridle = new ModelPart[] { saddle, bridle };
        this.reins = new ModelPart[] { reinsPart };

        // по умолчанию скрыть
        setVisibleArray(this.saddleAndBridle, false);
        setVisibleArray(this.reins, false);
    }

    private static ModelPart safeGet(ModelPart parent, String name) {
        if (parent == null) return null;
        try { return parent.getChild(name); } catch (Throwable ignored) { return null; }
    }

    public void applyState(CamelMorphState s) { this.state = s; }

    @Override
    public void setAngles(LivingEntity entity,
                          float limbAngle,
                          float limbDistance,
                          float animationProgress,
                          float relHeadYawDeg,
                          float headPitchDeg) {
        if (state == null) return;

        this.getPart().traverse().forEach(ModelPart::resetTransform);

        // Углы головы как в CamelEntityModel.setHeadAngles(...)
        float headYaw = MathHelper.clamp(relHeadYawDeg, -30.0f, 30.0f);
        float headPitch = MathHelper.clamp(headPitchDeg, -25.0f, 45.0f);

        int jumpCooldown = state.getJumpCooldown();
        if (jumpCooldown > 0) {
            float f = animationProgress - state.age();
            float g = 45.0f * ((float)jumpCooldown - f) / 55.0f;
            headPitch = MathHelper.clamp(headPitch + g, -25.0f, 70.0f);
        }
        if (head != null) {
            head.yaw = headYaw * ((float)Math.PI / 180f);
            head.pitch = headPitch * ((float)Math.PI / 180f);
        }

        // Движение (1:1 ваниль): WALKING
        this.animateMovement(CamelAnimations.WALKING, limbAngle, limbDistance, 2.0f, 2.5f);

        // Сидение/переходы/idle/dash — через AnimationState из CamelMorphStateImpl
        if (state instanceof CamelMorphStateImpl s) {
            this.updateAnimation(s.sittingTransitionAnimationState, CamelAnimations.SITTING_TRANSITION, animationProgress, 1.0f);
            this.updateAnimation(s.sittingAnimationState,           CamelAnimations.SITTING,            animationProgress, 1.0f);
            this.updateAnimation(s.standingTransitionAnimationState, CamelAnimations.STANDING_TRANSITION, animationProgress, 1.0f);
            this.updateAnimation(s.idlingAnimationState,            CamelAnimations.IDLING,             animationProgress, 1.0f);
            this.updateAnimation(s.dashingAnimationState,           CamelAnimations.DASHING,            animationProgress, 1.0f);
        }

        // Обновить видимость доп. частей (седло/поводья), как в ваниле
        updateVisibleParts();
    }

    private void updateVisibleParts() {
        boolean saddleVisible = state != null && state.isSaddled();
        boolean reinsVisible  = state != null && state.showReins() && saddleVisible;
        setVisibleArray(this.saddleAndBridle, saddleVisible);
        setVisibleArray(this.reins, reinsVisible);
    }

    private static void setVisibleArray(ModelPart[] arr, boolean vis) {
        if (arr == null) return;
        for (ModelPart p : arr) if (p != null) p.visible = vis;
    }

    public ModelPart getRoot() { return root; }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        this.root.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}