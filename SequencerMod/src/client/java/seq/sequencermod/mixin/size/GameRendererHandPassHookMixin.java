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
import seq.sequencermod.render.RenderPassFlags;
import seq.sequencermod.size.PlayerClientSizes;
import seq.sequencermod.size.PlayerSizeData;
import seq.sequencermod.size.config.MicroRenderConfig;

@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererHandPassHookMixin {

    @Shadow public abstract void loadProjectionMatrix(Matrix4f projection);
    @Shadow public abstract float getViewDistance();
    @Shadow public abstract Camera getCamera();

    @Invoker("getFov")
    protected abstract double sequencer$invokeGetFov(Camera camera, float tickDelta, boolean changingFov);

    private Matrix4f sequencer$prevProj = null;

    @Inject(method = "renderHand", at = @At("HEAD"))
    private void sequencer$enterHand(net.minecraft.client.util.math.MatrixStack matrices, Camera camera, float tickDelta, CallbackInfo ci) {
        // Явно помечаем hand‑pass (даже если конфиг tiny-NEAR сейчас выключен)
        RenderPassFlags.enterHand();
        // Ниже — существующая логика (если включите APPLY_CUSTOM_NEAR_IN_HAND)
        if (!MicroRenderConfig.APPLY_CUSTOM_NEAR_IN_HAND) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null || mc.player == null) return;

        Perspective persp = mc.options != null ? mc.options.getPerspective() : Perspective.FIRST_PERSON;
        if (persp == null || !persp.isFirstPerson()) return;

        PlayerEntity p = mc.player;
        PlayerSizeData data = PlayerClientSizes.get(p.getUuid());
        if (data == null || !(data.height > 0f)) return;
        if (data.height >= MicroRenderConfig.MICRO_HEIGHT_THRESHOLD) return;

        int fbw = Math.max(1, mc.getWindow().getFramebufferWidth());
        int fbh = Math.max(1, mc.getWindow().getFramebufferHeight());
        float aspect = (float) fbw / (float) fbh;
        double fovRad = Math.toRadians(this.sequencer$invokeGetFov(camera, tickDelta, false));
        if (!(aspect > 0f) || !(fovRad > 0d)) return;

        final boolean ultraMicro = data.height < 0.01f;
        final float NEAR_MIN = ultraMicro ? MicroRenderConfig.HAND_NEAR_MIN : MicroRenderConfig.NEAR_MIN_TINY;
        final float NEAR_MAX = ultraMicro ? MicroRenderConfig.HAND_NEAR_MAX : MicroRenderConfig.NEAR_MAX_TINY;

        float eye = Math.max(0.001f, p.getStandingEyeHeight());
        float desiredNear = Math.max(NEAR_MIN, Math.min(NEAR_MAX, eye * MicroRenderConfig.EYE_NEAR_FRACTION));

        float far = Math.max(512f, getViewDistance() + 64f);
        far = Math.min(far, MicroRenderConfig.FAR_CLIP_HARD);

        float near = desiredNear;
        if (far / near > MicroRenderConfig.FAR_NEAR_MAX_RATIO) {
            near = far / MicroRenderConfig.FAR_NEAR_MAX_RATIO;
        }

        sequencer$prevProj = new Matrix4f(); // при необходимости — сохранить
        Matrix4f proj = new Matrix4f().setPerspective((float) fovRad, aspect, near, far);
        loadProjectionMatrix(proj);
    }

    @Inject(method = "renderHand", at = @At("TAIL"))
    private void sequencer$exitHand(net.minecraft.client.util.math.MatrixStack matrices, Camera camera, float tickDelta, CallbackInfo ci) {
        // Гарантированно снимаем флаг hand‑pass
        RenderPassFlags.exitHand();
        // Если хотели возвращать матрицу — делайте это здесь (по текущему коду не требуется).
        sequencer$prevProj = null;
    }
}