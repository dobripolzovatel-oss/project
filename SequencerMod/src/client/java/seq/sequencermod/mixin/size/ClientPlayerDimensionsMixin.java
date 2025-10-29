package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.size.PlayerClientSizes;
import seq.sequencermod.size.PlayerSizeData;

/**
 * CLIENT: меняем только хитбокс игрока (AABB) по валидным данным, пришедшим от сервера.
 */
@Environment(EnvType.CLIENT)
@Mixin(PlayerEntity.class)
public abstract class ClientPlayerDimensionsMixin {

    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void sequencer$clientDims(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        PlayerEntity p = (PlayerEntity)(Object)this;
        PlayerSizeData d = PlayerClientSizes.get(p.getUuid());
        if (d == null) return;                 // нет серверных данных — оставляем ваниль
        if (pose == EntityPose.SLEEPING) return;

        // Игнорируем нулевые/неположительные значения (невалидная/промежуточная синхронизация)
        if (!(d.width > 0.0f) || !(d.height > 0.0f)) return;

        float w = d.width;
        float h = d.height;
        cir.setReturnValue(EntityDimensions.changing(w, h));
    }
}