package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.size.util.SizeCalc;

@Environment(EnvType.CLIENT)
@Mixin(Entity.class)
public abstract class EntityInsideWallTinyBypassClientMixin {
    @Unique private static final float TINY_BYPASS_HEIGHT = 0.12f; // 0.06..0.12 — порог tiny
    @Unique private static final boolean DEBUG_LOG = false;
    @Unique private static long lastLogNs = 0;

    @Inject(method = "isInsideWall", at = @At("HEAD"), cancellable = true)
    private void sequencermod$disableInWallForTiny(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof PlayerEntity player)) return;

        float h = Math.max(SizeCalc.EPS, player.getDimensions(player.getPose()).height);

        // Для любых размеров ниже tiny-порога полностью отключаем флаг нахождения в блоке
        if (h < TINY_BYPASS_HEIGHT) {
            if (DEBUG_LOG) {
                long now = System.nanoTime();
                if (now - lastLogNs > 300_000_000L) { // не чаще раза в ~0.3с
                    System.out.printf("[TinyInWall] bypass isInsideWall h=%.5f pose=%s pos=(%.2f,%.2f,%.2f)%n",
                            h, player.getPose(), player.getX(), player.getY(), player.getZ());
                    lastLogNs = now;
                }
            }
            cir.setReturnValue(false);
        }
    }
}