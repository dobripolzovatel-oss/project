package seq.sequencermod.client.trace.mixin;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import seq.sequencermod.client.trace.TraceLog;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    // В 1.20.1: render(FJZ)V
    @Inject(method = "render", at = @At("HEAD"))
    private void seq_trace$grHead(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        TraceLog.begin("GameRenderer.render", "begin dt=%.3f tick=%s", tickDelta, tick);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void seq_trace$grTail(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        TraceLog.end("GameRenderer.render", "end dt=%.3f tick=%s", tickDelta, tick);
    }
}