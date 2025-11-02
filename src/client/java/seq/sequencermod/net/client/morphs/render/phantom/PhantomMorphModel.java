package seq.sequencermod.net.client.morphs.render.phantom;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * Модель морфа фантома.
 * Полная анимация как у ванили (PhantomEntityModel):
 * - Крылья: roll = cos(k) * 16°
 * - Хвост:  pitch = -(5° + cos(2k) * 5°)
 * где k = (wingFlapOffsetTicks + animationProgress) * 7.448451° (переводим в радианы).
 *
 * Замены:
 * - getWingFlapTickOffset() -> PhantomMorphState.wingFlapOffsetTicks
 * - entity == LivingEntity (модель морфа) и animationProgress передаётся снаружи.
 */
public class PhantomMorphModel extends SinglePartEntityModel<LivingEntity> {

    private static final String TAIL_BASE = "tail_base";
    private static final String TAIL_TIP  = "tail_tip";

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart leftWingBase;
    private final ModelPart leftWingTip;
    private final ModelPart rightWingBase;
    private final ModelPart rightWingTip;
    private final ModelPart tailBase;
    private final ModelPart tailTip;

    private PhantomMorphState state;

    // Текстуры — как у ванили
    private static final Identifier TEXTURE = new Identifier("minecraft", "textures/entity/phantom.png");

    public PhantomMorphModel(ModelPart root) {
        // У фантома — непрозрачный cutout слой
        super(RenderLayer::getEntityCutoutNoCull);
        this.root = root;
        this.body = root.getChild(EntityModelPartNames.BODY);
        this.tailBase = this.body.getChild(TAIL_BASE);
        this.tailTip = this.tailBase.getChild(TAIL_TIP);
        this.leftWingBase = this.body.getChild(EntityModelPartNames.LEFT_WING_BASE);
        this.leftWingTip = this.leftWingBase.getChild(EntityModelPartNames.LEFT_WING_TIP);
        this.rightWingBase = this.body.getChild(EntityModelPartNames.RIGHT_WING_BASE);
        this.rightWingTip = this.rightWingBase.getChild(EntityModelPartNames.RIGHT_WING_TIP);
    }

    public void applyState(PhantomMorphState state) {
        this.state = state;
    }

    public Identifier getTexture() {
        return TEXTURE;
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }

    /**
     * Полная анимация как у ванили (формулы выше).
     * limbAngle/limbDistance не используются фантомом.
     */
    @Override
    public void setAngles(LivingEntity entity,
                          float limbAngle,
                          float limbDistance,
                          float animationProgress,
                          float headYawDeg,
                          float headPitchDeg) {
        if (state == null) return;

        // Ваниль: (offset + time) * 7.448451° -> радианы
        float kDeg = (state.wingFlapOffsetTicks + animationProgress) * 7.448451f;
        float k = kDeg * ((float)Math.PI / 180f);

        // Крылья: +/- 16°
        float roll = MathHelper.cos(k) * 16.0f * ((float)Math.PI / 180f);
        this.leftWingBase.roll  = roll;
        this.leftWingTip.roll   = roll;
        this.rightWingBase.roll = -roll;
        this.rightWingTip.roll  = -roll;

        // Хвост: -(5° + cos(2k) * 5°)
        float tailPitch = -(5.0f + MathHelper.cos(k * 2.0f) * 5.0f) * ((float)Math.PI / 180f);
        this.tailBase.pitch = tailPitch;
        this.tailTip.pitch  = tailPitch;
    }

    @Override
    public void render(MatrixStack matrices,
                       VertexConsumer vertices,
                       int light,
                       int overlay,
                       float red, float green, float blue, float alpha) {
        this.root.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}