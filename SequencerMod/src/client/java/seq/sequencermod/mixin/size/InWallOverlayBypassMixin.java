package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.size.util.SizeCalc;

/**
 * Client-only: отключаем in-wall overlay (чёрный экран) для маленького игрока в первом лице.
 * Делается на этапе определения состояния блока: getInWallBlockState(player) -> null,
 * тогда renderInWallOverlay(...) просто не вызывается.
 *
 * По дампу 1.20.1 (yarn 1.20.1+build.10) сигнатура цели:
 *   getInWallBlockState(Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/block/BlockState;
 */
@Environment(EnvType.CLIENT)
@Mixin(InGameOverlayRenderer.class)
public abstract class InWallOverlayBypassMixin {

    // Отключаем overlay для всех "tiny" — синхронизировано с остальными миксинами
    @Unique private static final float TINY_BYPASS_HEIGHT = 0.12f;

    @Unique
    private static boolean shouldBypass(PlayerEntity p) {
        if (p == null) return false;
        if (p.getPose() == EntityPose.SLEEPING) return false; // сон оставляем ваниле
        float h = Math.max(SizeCalc.EPS, p.getDimensions(p.getPose()).height);
        // ВАЖНО: без нижнего микро‑порога — всегда байпасим overlay при h < 0.12
        return h < TINY_BYPASS_HEIGHT;
    }

    @Inject(
            method = "getInWallBlockState(Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/block/BlockState;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void sequencer$nullInWallForTiny(PlayerEntity player, CallbackInfoReturnable<BlockState> cir) {
        if (shouldBypass(player)) {
            // Сообщаем "нет блока у камеры" — overlay не будет отрисован
            cir.setReturnValue(null);
        }
    }
}