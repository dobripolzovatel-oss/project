package seq.sequencermod.mixin;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.client.preview.PreviewDebug;

/**
 * Безопасная версия: подменяет только CULL-слой на NON-CULL в режиме превью.
 * НИКАКИХ инжектов в getEntityTranslucent (с/без boolean), чтобы не конфликтовать с Indigo.
 */
@Mixin(RenderLayer.class)
public abstract class RenderLayerNonCullInGuiMixin {

    static {
        System.out.println("[SequencerMixins] RenderLayerNonCullInGuiMixin loaded (safe)");
    }

    // Универсальный инжект: без дескриптора (на случай различий маппингов)
    @Inject(method = "getEntityTranslucentCull", at = @At("HEAD"), cancellable = true, require = 0)
    private static void sequencer$swapCullNoDesc(Identifier texture, CallbackInfoReturnable<RenderLayer> cir) {
        if (PreviewDebug.inGui()) {
            System.out.println("[SequencerMixins] swap CULL -> NON-CULL for " + texture);
            cir.setReturnValue(RenderLayer.getEntityTranslucent(texture));
        }
    }

    // Точный дескриптор под 1.20.1 — оставляем как дубль, но это та же логика.
    @Inject(
            method = "getEntityTranslucentCull(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private static void sequencer$swapCullWithDesc(Identifier texture, CallbackInfoReturnable<RenderLayer> cir) {
        if (PreviewDebug.inGui()) {
            System.out.println("[SequencerMixins] swap (with-desc) CULL -> NON-CULL for " + texture);
            cir.setReturnValue(RenderLayer.getEntityTranslucent(texture));
        }
    }
}