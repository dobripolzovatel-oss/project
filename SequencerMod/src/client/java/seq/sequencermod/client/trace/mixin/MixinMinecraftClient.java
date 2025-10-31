package seq.sequencermod.client.trace.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import seq.sequencermod.client.trace.TraceLog;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {

    @Inject(method = "tick", at = @At("HEAD"))
    private void seq_trace$tickHead(CallbackInfo ci) {
        TraceLog.begin("MC.tick", "begin");
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void seq_trace$tickTail(CallbackInfo ci) {
        TraceLog.end("MC.tick", "end");
    }

    // В 1.20.1 сигнатура MinecraftClient.render(Z)V — параметр tick (boolean)
    @Inject(method = "render", at = @At("HEAD"))
    private void seq_trace$renderHead(boolean tick, CallbackInfo ci) {
        TraceLog.begin("MC.render", "begin tick=%s", tick);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void seq_trace$renderTail(boolean tick, CallbackInfo ci) {
        TraceLog.end("MC.render", "end tick=%s", tick);
    }
}