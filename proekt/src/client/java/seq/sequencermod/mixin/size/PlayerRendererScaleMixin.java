package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import seq.sequencermod.size.PlayerClientSizes;
import seq.sequencermod.size.PlayerSizeData;
import seq.sequencermod.size.auto.SizeDerived;
import seq.sequencermod.size.auto.SizeDerivedStore;
import seq.sequencermod.size.auto.SizeRules;
import seq.sequencermod.size.util.SizeCalc;

@Environment(EnvType.CLIENT)
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerRendererScaleMixin {

    @Inject(
            method = "scale(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/util/math/MatrixStack;F)V",
            at = @At("HEAD")
    )
    private void sequencer$scale(AbstractClientPlayerEntity player, MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        PlayerSizeData d = PlayerClientSizes.get(player.getUuid());
        if (d == null) return;

        SizeDerived derived = SizeDerivedStore.get(player.getUuid());
        if (derived == null) {
            derived = SizeRules.compute(player, d);
            SizeDerivedStore.set(player.getUuid(), derived);
        }

        float real = SizeCalc.resolveScale(d); // физика/логика — реальный масштаб
        float visual = real;

        if (derived.visualUpscaleEnabled && real < derived.minVisualModelScale) {
            float target = derived.minVisualModelScale;
            float maxAllowed = Math.max(real * derived.maxVisualUpscaleFactor, real);
            if (target > maxAllowed) target = maxAllowed;
            visual = target;
        }

        if (visual > 512f) visual = 512f;

        matrices.scale(visual, visual, visual);
    }
}