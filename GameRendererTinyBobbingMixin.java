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
 * Client-only: отключаем view bobbing у микро-игрока в 1-м лице,
 * чтобы убрать "микро- дрожь" у упора к блокам.
 */
@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererTinyBobbingMixin {

    // ниже этого масштаба отключаем bobbing (0.25 ~= рост ~0.45м)
    private static final float BOB_DISABLE_SCALE = 0.25f;

    @Inject(method = "bobView(Lnet/minecraft/client/util/math/MatrixStack;F)V",
            at = @At("HEAD"), cancellable = true)
    private void sequencer$disableBobbingForTiny(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        Perspective psp = mc.options != null ? mc.options.getPerspective() : Perspective.FIRST_PERSON;
        if (psp == null || !psp.isFirstPerson()) return;

        PlayerEntity p = mc.player;
        if (p == null) return;

        float h = Math.max(SizeCalc.EPS, p.getDimensions(p.getPose()).height);
        float s = Math.max(0.01f, h / 1.8f);

        if (s < BOB_DISABLE_SCALE) {
            // Полностью отменяем bobbing для tiny
            ci.cancel();
        }
    }
}