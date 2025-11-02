package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.size.util.SizeCalc;

@Environment(EnvType.CLIENT)
@Mixin(Entity.class)
public abstract class EntityInsideWallTinyBypassClientMixin {
    // Порог высоты хитбокса, ниже которого overlay отключаем
    private static final float TINY_BYPASS_HEIGHT = 0.08f; // изменяй при необходимости: 0.06..0.10
    private static final boolean DEBUG_LOG = false;
    private static long lastLogNs = 0;

    @Inject(method = "isInsideWall", at = @At("HEAD"), cancellable = true)
    private void sequencermod$disableInWallForTiny(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof PlayerEntity player)) return;

        float h = Math.max(SizeCalc.EPS, player.getDimensions(player.getPose()).height);
        if (h < TINY_BYPASS_HEIGHT) {
            if (DEBUG_LOG) {
                long now = System.nanoTime();
                if (now - lastLogNs > 300_000_000L) { // не чаще раза в ~0.3с
                    System.out.printf("[TinyInWall] bypass h=%.5f pose=%s pos=(%.2f,%.2f,%.2f)%n",
                            h, player.getPose(), player.getX(), player.getY(), player.getZ());
                    lastLogNs = now;
                }
            }
            cir.setReturnValue(false);
        }
    }
}