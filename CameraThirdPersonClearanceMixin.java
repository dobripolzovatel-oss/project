package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(Camera.class)
public abstract class CameraThirdPersonClearanceMixin {

    @Shadow private boolean thirdPerson;

    @Inject(method = "clipToSpace(D)D", at = @At("RETURN"), cancellable = true)
    private void sequencer$softClearance(double desired, CallbackInfoReturnable<Double> cir) {
        if (!this.thirdPerson) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity p = mc != null ? mc.player : null;
        if (p == null) return;

        double d = cir.getReturnValueD();
        if (d < desired) {
            double severity = Math.max(0.0, desired - d);
            float s = Math.max(0.00001f, p.getDimensions(p.getPose()).height / 1.8f);

            double extra =
                    0.032 +                                 // базовый
                            Math.min(0.16, severity * 0.45) +       // от силы поджатия
                            (1.0 - Math.min(1.0, s / 0.25)) * 0.09; // tiny бонус

            if (s < 1.0e-4f) { // ultra-tiny
                extra += 0.04; // ещё +4см зазора
            }

            double adjusted = Math.max(0.0, d - extra);
            cir.setReturnValue(adjusted);
        }
    }
}