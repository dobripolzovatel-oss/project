package seq.sequencermod.mixin.client.extendedreach;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.extendedreach.ExtendedReachClient;

/**
 * В вашей ревизии Yarn getReachDistance возвращает float (getReachDistance()F).
 * Переопределяем именно эту версию.
 */
@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(method = "getReachDistance()F", at = @At("HEAD"), cancellable = true)
    private void sequencermod$overrideReachF(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(ExtendedReachClient.getReachF());
    }
}