package seq.sequencermod.client.trace.mixin;

import net.minecraft.client.gui.Element;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.client.trace.TraceLog;

/**
 * Инжекты в default-методы интерфейса Element.
 * В интерфейсном миксине методы-инжекторы должны иметь реализацию; используем private (Java 17 это поддерживает).
 */
@Mixin(Element.class)
public interface MixinElement {

    // boolean mouseClicked(double, double, int)
    @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), require = 0)
    private void seq_trace$mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        TraceLog.log("Screen.mouseClicked %s x=%.1f y=%.1f btn=%d", this.getClass().getName(), mouseX, mouseY, button);
    }

    // boolean mouseReleased(double, double, int)
    @Inject(method = "mouseReleased(DDI)Z", at = @At("HEAD"), require = 0)
    private void seq_trace$mouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        TraceLog.log("Screen.mouseReleased %s x=%.1f y=%.1f btn=%d", this.getClass().getName(), mouseX, mouseY, button);
    }

    // boolean mouseDragged(double, double, int, double, double)
    @Inject(method = "mouseDragged(DDIDD)Z", at = @At("HEAD"), require = 0)
    private void seq_trace$mouseDragged(double mouseX, double mouseY, int button, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        TraceLog.log("Screen.mouseDragged %s x=%.1f y=%.1f btn=%d dx=%.2f dy=%.2f",
                this.getClass().getName(), mouseX, mouseY, button, dx, dy);
    }

    // boolean mouseScrolled(double mouseX, double mouseY, double amount)
    @Inject(method = "mouseScrolled(DDD)Z", at = @At("HEAD"), require = 0)
    private void seq_trace$mouseScrolled(double mouseX, double mouseY, double amount, CallbackInfoReturnable<Boolean> cir) {
        TraceLog.log("Screen.mouseScrolled %s x=%.1f y=%.1f amt=%.2f",
                this.getClass().getName(), mouseX, mouseY, amount);
    }

    // boolean charTyped(char chr, int modifiers)
    @Inject(method = "charTyped(CI)Z", at = @At("HEAD"), require = 0)
    private void seq_trace$charTyped(char chr, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        String p = Character.isISOControl(chr) ? String.format("\\u%04X", (int) chr) : "'" + chr + "'";
        TraceLog.log("Screen.charTyped %s char=%s mod=%d", this.getClass().getName(), p, modifiers);
    }
}