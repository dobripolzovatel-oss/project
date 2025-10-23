package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.size.util.SizeCalc;

/**
 * Мягкая компенсация FOV для tiny: в первом лице немного уменьшаем FOV по мере уменьшения роста.
 * Сделано очень деликатно и сглажено, чтобы не было скачков.
 */
@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererTinyFovMixin {

    @Unique private double sequencer$prevFov = -1.0;

    private static float scaleFor(PlayerEntity p) {
        if (p == null) return 1.0f;
        float h = Math.max(SizeCalc.EPS, p.getDimensions(p.getPose()).height);
        return Math.max(0.01f, h / 1.8f);
    }

    // getFov(Camera, float tickDelta, boolean changingFov) -> double
    @Inject(method = "getFov(Lnet/minecraft/client/render/Camera;FZ)D",
            at = @At("RETURN"), cancellable = true)
    private void sequencer$softFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
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
        double mult = 1.0 - 0.18 * (1.0 - Math.sqrt(s));
        if (mult < 0.80) mult = 0.80;     // нижний предел
        if (mult > 1.00) mult = 1.00;

        double target = base * mult;

        // Сглаживаем, чтобы не дёргалось
        if (this.sequencer$prevFov < 0) this.sequencer$prevFov = target;
        double alpha = 0.25; // 0..1 (больше — быстрее)
        double smooth = this.sequencer$prevFov + (target - this.sequencer$prevFov) * alpha;
        this.sequencer$prevFov = smooth;

        cir.setReturnValue(smooth);
    }
}