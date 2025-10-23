package seq.sequencermod.net.client.morphs.render.phantom;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * Рендер морфа Фантома:
 * Включает ВСЁ, что делает ванильный PhantomEntityRenderer/Model:
 * - Yaw: поворот как у всех Living (180° - bodyYawInterp)
 * - Pitch: дополнительный наклон корпуса matrices.multiply(X, pitch), как у PhantomEntityRenderer.setupTransforms
 * - Масштаб: g = 1.0 + 0.15 * size (у морфа size=0 → g=1.0; формулу оставляем)
 * - Перенос: translate(0.0, 1.3125, 0.1875) после scale (ваниль)
 * - Крылья/хвост: формулы из PhantomEntityModel.setAngles (в модели)
 * - Светящиеся глаза: отдельный eyes-pass с полным освещением
 */
public class PhantomMorphRenderer {

    private final PhantomMorphModel model;
    private final PhantomMorphState state;

    // Подстройка посадки относительно роста игрока (морф рендерим в координатах игрока)
    private static final float VERTICAL_OFFSET = 1.1f; // поднять модель относительно ног игрока
    private static final float GLOBAL_SCALE    = 1.0f; // общий ручной масштаб морфа

    // Ванильный сдвиг фантома после scale
    private static final float VANILLA_TRANSLATE_Y = 1.3125f;
    private static final float VANILLA_TRANSLATE_Z = 0.1875f;

    // Глаза фантома (fullbright)
    private static final Identifier EYES_TEXTURE = new Identifier("minecraft", "textures/entity/phantom_eyes.png");

    public PhantomMorphRenderer(EntityModelLoader loader) {
        this.model = new PhantomMorphModel(loader.getModelPart(EntityModelLayers.PHANTOM));
        this.state = new PhantomMorphState();
    }

    public PhantomMorphState getState() {
        return state;
    }

    public void reset(AbstractClientPlayerEntity player) {
        state.reset(player);
    }

    public void tick(AbstractClientPlayerEntity player) {
        state.tick(player);
    }

    public void render(AbstractClientPlayerEntity player,
                       float tickDelta,
                       MatrixStack matrices,
                       VertexConsumerProvider providers,
                       int light) {

        model.applyState(state);

        matrices.push();

        // Базовый подъём под рост игрока
        matrices.translate(0.0, VERTICAL_OFFSET, 0.0);

        // Ванильный yaw: 180° - bodyYawInterp
        float bodyYawInterp = MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - bodyYawInterp));

        // Инверт сущностей (как рендер у всех Entity) + наш глобальный масштаб
        matrices.scale(-1.0f, -1.0f, 1.0f);
        matrices.scale(GLOBAL_SCALE, GLOBAL_SCALE, GLOBAL_SCALE);

        // Ванильный scale по "size" фантома (у морфа size = 0 → g=1.0)
        float phantomSize = 0.0f; // если захочешь — можно завести в состоянии
        float g = 1.0f + 0.15f * phantomSize;
        matrices.scale(g, g, g);

        // Ванильный сдвиг фантома (после scale)
        matrices.translate(0.0f, VANILLA_TRANSLATE_Y, VANILLA_TRANSLATE_Z);

        // Ванильный pitch: наклон корпуса по pitch сущности
        float headPitchInterp = MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(headPitchInterp));

        // Время анимации = age + tickDelta (как у всех моделей)
        float animationProgress = player.age + tickDelta;

        // У фантома limbAngle/Distance не используются
        model.setAngles(
                player,
                0.0f,
                0.0f,
                animationProgress,
                0.0f,
                headPitchInterp
        );

        // Базовый pass (тело)
        VertexConsumer bodyVc = providers.getBuffer(model.getLayer(model.getTexture()));
        model.render(matrices, bodyVc, light, OverlayTexture.DEFAULT_UV, 1f, 1f, 1f, 1f);

        // Eyes-pass (fullbright, как PhantomEyesFeatureRenderer)
        VertexConsumer eyesVc = providers.getBuffer(RenderLayer.getEyes(EYES_TEXTURE));
        // Полное освещение для "свечения" глаз
        int fullbright = 0x00F000F0;
        model.render(matrices, eyesVc, fullbright, OverlayTexture.DEFAULT_UV, 1f, 1f, 1f, 1f);

        matrices.pop();
    }
}