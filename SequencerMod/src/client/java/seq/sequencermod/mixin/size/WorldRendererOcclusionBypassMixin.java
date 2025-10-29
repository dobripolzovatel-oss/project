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
 * Аварийный байпас occlusion culling (последнее средство).
 * Когда включён DEBUG_DISABLE_OCCLUSION, все chunk sections считаются видимыми
 * независимо от результатов occlusion query.
 * 
 * Это диагностический инструмент для определения, является ли occlusion culling
 * причиной исчезновения мира.
 */
@Environment(EnvType.CLIENT)
@Mixin(WorldRenderer.class)
public class WorldRendererOcclusionBypassMixin {

    /**
     * В WorldRenderer есть метод isChunkVisible который проверяет результаты occlusion.
     * В 1.20.1 это обычно ChunkBuilder.BuiltChunk.shouldBuild() или похожие методы.
     * Мы перехватываем на уровне, где проверяется видимость chunk section.
     */
    @Inject(method = "isTerrainRenderComplete()Z", at = @At("HEAD"), cancellable = true)
    private void sequencer$bypassTerrainComplete(CallbackInfoReturnable<Boolean> cir) {
        // Если occlusion отключён, всегда считаем terrain complete
        // чтобы избежать бесконечного ожидания occlusion query
        if (MicroRenderConfig.DEBUG_DISABLE_OCCLUSION) {
            cir.setReturnValue(true);
        }
    }
}
