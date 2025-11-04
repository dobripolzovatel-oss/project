package seq.sequencermod.mixin.size;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.size.PlayerSizeData;
import seq.sequencermod.size.PlayerSizeServerStore;
import seq.sequencermod.size.util.SizeCalc;

@Mixin(value = PlayerEntity.class, priority = 1200)
public abstract class ServerPlayerDimensionsMixin {

    @Inject(method = "getDimensions(Lnet/minecraft/entity/EntityPose;)Lnet/minecraft/entity/EntityDimensions;",
            at = @At("HEAD"), cancellable = true)
    private void sequencer$dims(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        PlayerEntity p = (PlayerEntity)(Object)this;
        PlayerSizeData d = PlayerSizeServerStore.get(p.getUuid());
        if (d == null) return;
        if (pose == EntityPose.SLEEPING) return;

        float w = Math.max(SizeCalc.EPS, d.width);
        float h = Math.max(SizeCalc.EPS, d.height);
        cir.setReturnValue(EntityDimensions.changing(w, h));
    }
}