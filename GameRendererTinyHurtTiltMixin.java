package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import seq.sequencermod.size.util.SizeCalc;

/**
 * Client-only: отключаем наклон камеры от урона у микро-игрока (может давать микродрожь у упора).
 */
@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererTinyHurtTiltMixin {

    private static final float HURT_TILT_DISABLE_SCALE = 0.25f;

    @Inject(method = "tiltViewWhenHurt(Lnet/minecraft/client/util/math/MatrixStack;F)V",
            at = @At("HEAD"), cancellable = true)
    private void sequencer$disableHurtTiltForTiny(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        Perspective psp = mc.options != null ? mc.options.getPerspective() : Perspective.FIRST_PERSON;
        if (psp == null || !psp.isFirstPerson()) return;

        PlayerEntity p = mc.player;
        if (p == null) return;

        float h = Math.max(SizeCalc.EPS, p.getDimensions(p.getPose()).height);
        float s = Math.max(0.01f, h / 1.8f);

        if (s < HURT_TILT_DISABLE_SCALE) {
            ci.cancel();
        }
    }
}