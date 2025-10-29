package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerEntity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.gen.Invoker;
import seq.sequencermod.size.PlayerClientSizes;
import seq.sequencermod.size.PlayerSizeData;
import seq.sequencermod.size.config.MicroRenderConfig;

/**
 * Малый near применяем только вокруг рендера руки/предмета в 1-м лице.
 * Это не влияет на фрустум рендера мира и не ломает видимость чанков.
 */
@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererHandPassHookMixin {

    @Shadow public abstract void loadProjectionMatrix(Matrix4f projection);
    @Shadow public abstract float getViewDistance();
    @Shadow public abstract Camera getCamera();

    // Доступ к приватному GameRenderer#getFov(Camera, float, boolean)
    @Invoker("getFov")
    protected abstract double sequencer$invokeGetFov(Camera camera, float tickDelta, boolean changingFov);

    private Matrix4f sequencer$prevProj = null;
    private boolean  sequencer$handPushed = false;

    @Inject(method = "renderHand", at = @At("HEAD"))
    private void sequencer$enterHand(net.minecraft.client.util.math.MatrixStack matrices, Camera camera, float tickDelta, CallbackInfo ci) {
        if (!MicroRenderConfig.APPLY_CUSTOM_NEAR_IN_HAND) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null || mc.player == null) return;

        // Только 1-е лицо
        Perspective persp = mc.options != null ? mc.options.getPerspective() : Perspective.FIRST_PERSON;
        if (persp == null || !persp.isFirstPerson()) return;

        // Проверяем рост
        PlayerEntity p = mc.player;
        PlayerSizeData data = PlayerClientSizes.get(p.getUuid());
        if (data == null || !(data.height > 0f)) return;
        if (data.height >= MicroRenderConfig.MICRO_HEIGHT_THRESHOLD) return;

        int fbw = Math.max(1, mc.getWindow().getFramebufferWidth());
        int fbh = Math.max(1, mc.getWindow().getFramebufferHeight());
        float aspect = (float) fbw / (float) fbh;

        double fovRad = Math.toRadians(this.sequencer$invokeGetFov(camera, tickDelta, false));
        if (!(aspect > 0f) || !(fovRad > 0d)) return;

        // Режимы tiny/micro
        final boolean ultraMicro = data.height < 0.01f;
        final float NEAR_MIN = ultraMicro ? MicroRenderConfig.HAND_NEAR_MIN : MicroRenderConfig.NEAR_MIN_TINY;
        final float NEAR_MAX = ultraMicro ? MicroRenderConfig.HAND_NEAR_MAX : MicroRenderConfig.NEAR_MAX_TINY;

        // Берём near от высоты глаз
        float eye = Math.max(0.001f, p.getStandingEyeHeight());
        float desiredNear = Math.max(NEAR_MIN, Math.min(NEAR_MAX, eye * MicroRenderConfig.EYE_NEAR_FRACTION));

        // Far для руки из видимой дистанции (в блоках) + запас
        float far = Math.max(512f, getViewDistance() + 64f);
        far = Math.min(far, MicroRenderConfig.FAR_CLIP_HARD);

        float near = desiredNear;
        if (far / near > MicroRenderConfig.FAR_NEAR_MAX_RATIO) {
            near = Math.max(NEAR_MIN, far / MicroRenderConfig.FAR_NEAR_MAX_RATIO);
        }
        if (!(far > near)) return;

        try {
            // Сохраняем "ванильную" матрицу для восстановления
            float vanillaNear = 0.05f;
            float vanillaFar  = Math.max(512f, getViewDistance() + 64f);
            Matrix4f vanilla = new Matrix4f().setPerspective((float)fovRad, aspect, vanillaNear, vanillaFar);
            sequencer$prevProj = new Matrix4f(vanilla);

            Matrix4f handProj = new Matrix4f().setPerspective((float)fovRad, aspect, near, far);
            loadProjectionMatrix(handProj);
            sequencer$handPushed = true;
        } catch (Throwable ignored) {
            sequencer$handPushed = false;
            sequencer$prevProj = null;
        }
    }

    @Inject(method = "renderHand", at = @At("RETURN"))
    private void sequencer$exitHand(net.minecraft.client.util.math.MatrixStack matrices, Camera camera, float tickDelta, CallbackInfo ci) {
        if (!sequencer$handPushed) return;
        try {
            if (sequencer$prevProj != null) {
                loadProjectionMatrix(sequencer$prevProj);
            }
        } catch (Throwable ignored) {
        } finally {
            sequencer$handPushed = false;
            sequencer$prevProj = null;
        }
    }
}