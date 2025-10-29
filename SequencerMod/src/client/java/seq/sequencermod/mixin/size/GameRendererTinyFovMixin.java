package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.size.util.SizeCalc;
import seq.sequencermod.size.config.MicroRenderConfig;
import seq.sequencermod.render.RenderPassFlags;

/**
 * Мягкая компенсация FOV для tiny: в первом лице немного уменьшаем FOV по мере уменьшения роста.
 *
 * Исправления:
 * - Применяем только при включённом флаге конфигурации.
 * - Не трогаем FOV в проходе рендера руки (чтобы избежать рассинхрона матриц между world/hand).
 * - Без меж-кадрового сглаживания (статeless), чтобы исключить накопление/артефакты в других проходах
 *   (напр., небо/солнечные тела).
 */
@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererTinyFovMixin {

    private static float scaleFor(PlayerEntity p) {
        if (p == null) return 1.0f;
        float h = Math.max(SizeCalc.EPS, p.getDimensions(p.getPose()).height);
        return Math.max(0.01f, h / 1.8f);
    }

    // getFov(Camera, float tickDelta, boolean changingFov) -> double
    @Inject(method = "getFov(Lnet/minecraft/client/render/Camera;FZ)D",
            at = @At("RETURN"), cancellable = true)
    private void sequencer$softFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        // DEBUG_VANILLA_CAMERA отключает все изменения камеры/FOV
        if (MicroRenderConfig.DEBUG_VANILLA_CAMERA) return;

        // Конфиг можно быстро выключить целиком
        if (!MicroRenderConfig.SCALE_FOV) return;

        // Рендер руки/предмета — оставляем ванильный FOV, чтобы не было расхождений
        if (RenderPassFlags.isHand()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;

        Perspective persp = mc.options != null ? mc.options.getPerspective() : Perspective.FIRST_PERSON;
        if (persp == null || !persp.isFirstPerson()) return;

        PlayerEntity p = mc.player;
        if (p == null) return;

        double base = cir.getReturnValueD();

        // Мягкий множитель FOV от масштаба (чем меньше — тем чуть уже)
        float s = scaleFor(p);
        // m(s) = 1 - 0.18*(1 - sqrt(s))  -> min около ~0.82 при очень маленьких
        double mult = 1.0 - 0.18 * (1.0 - Math.sqrt(Math.max(0.0, Math.min(1.0, s))));
        if (mult < 0.80) mult = 0.80;     // нижний предел
        if (mult > 1.00) mult = 1.00;

        // Статическое применение без накопления
        double adjusted = base * mult;
        cir.setReturnValue(adjusted);
    }
}