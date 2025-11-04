package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.size.util.ReachCalc;

/**
 * Клиент: уменьшаем reach для tiny. Серверные проверки остаются ванильными.
 * В твоих маппингах есть ТОЛЬКО getReachDistance(): float — оставляем один инжект.
 */
@Environment(EnvType.CLIENT)
@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientReachDistanceMixin {

    @Inject(method = "getReachDistance()F", at = @At("HEAD"), cancellable = true)
    private void sequencer$reachF(CallbackInfoReturnable<Float> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity p = mc != null ? mc.player : null;
        if (p == null) return;

        boolean creative = p.getAbilities().creativeMode;
        float vanilla = creative ? 5.0f : 4.5f; // ванильные значения

        float scaled = ReachCalc.blockReachFor(p, creative, vanilla);
        cir.setReturnValue(scaled);
    }
}