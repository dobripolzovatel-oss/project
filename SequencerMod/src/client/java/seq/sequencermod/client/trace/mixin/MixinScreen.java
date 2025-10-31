package seq.sequencermod.client.trace.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.client.trace.TraceLog;

@Mixin(Screen.class)
public abstract class MixinScreen {

    // void render(DrawContext, int, int, float)
    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("HEAD"), require = 0)
    private void seq_trace$renderHead(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        TraceLog.begin("Screen.render", "%s mx=%d my=%d dt=%.3f", this.getClass().getName(), mouseX, mouseY, delta);
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("TAIL"), require = 0)
    private void seq_trace$renderTail(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        TraceLog.end("Screen.render", "%s done", this.getClass().getName());
    }

    // void tick()
    @Inject(method = "tick()V", at = @At("HEAD"), require = 0)
    private void seq_trace$tickHead(CallbackInfo ci) {
        TraceLog.log("Screen.tick %s", this.getClass().getName());
    }

    // boolean keyPressed(int, int, int) — переопределён в Screen
    @Inject(method = "keyPressed(III)Z", at = @At("HEAD"), require = 0)
    private void seq_trace$keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        TraceLog.log("Screen.keyPressed %s key=%d scan=%d mod=%d", this.getClass().getName(), keyCode, scanCode, modifiers);
    }
}