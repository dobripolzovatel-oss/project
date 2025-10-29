package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.size.config.MicroRenderConfig;

/**
 * Аварийный байпас для диагностики проблем рендера (последнее средство).
 * Когда включён DEBUG_DISABLE_OCCLUSION, обходим проверки завершённости рендера terrain.
 * 
 * Это диагностический инструмент для определения, связана ли проблема исчезновения мира
 * с процессом построения и готовности terrain chunks.
 */
@Environment(EnvType.CLIENT)
@Mixin(WorldRenderer.class)
public class WorldRendererOcclusionBypassMixin {

    /**
     * isTerrainRenderComplete() проверяет, завершён ли рендер terrain и готовы ли chunks.
     * Когда диагностический флаг включён, принудительно возвращаем true,
     * чтобы избежать блокировки из-за недостроенных chunks или незавершённых query.
     * 
     * Примечание: это не напрямую occlusion culling, но связано с готовностью chunks.
     * Название "OcclusionBypass" отражает диагностическую цель - определить,
     * связана ли проблема с системой построения и видимости chunks.
     */
    @Inject(method = "isTerrainRenderComplete()Z", at = @At("HEAD"), cancellable = true)
    private void sequencer$bypassTerrainComplete(CallbackInfoReturnable<Boolean> cir) {
        // Если диагностический режим включён, всегда считаем terrain complete
        // чтобы избежать бесконечного ожидания построения chunks
        if (MicroRenderConfig.DEBUG_DISABLE_OCCLUSION) {
            cir.setReturnValue(true);
        }
    }
}
