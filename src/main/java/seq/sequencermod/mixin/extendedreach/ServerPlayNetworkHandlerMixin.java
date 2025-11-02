package seq.sequencermod.mixin.extendedreach;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import seq.sequencermod.extendedreach.BlockReach;

/**
 * Надёжная проверка reach на взаимодействие с сущностями:
 * вместо @ModifyConstant делаем HEAD-инжект и отменяем обработку, если далеко.
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow public ServerPlayerEntity player;

    @Inject(method = "onPlayerInteractEntity", at = @At("HEAD"), cancellable = true)
    private void sequencermod$guardEntityReach(PlayerInteractEntityC2SPacket packet, CallbackInfo ci) {
        if (this.player == null) return;

        // getWorld() возвращает World, но в рантайме это ServerWorld — проверим и приведём
        if (!(this.player.getWorld() instanceof ServerWorld sw)) return;

        // entity может быть null, если id несуществующий
        Entity target = packet.getEntity(sw);
        if (target == null) return;

        double distSq = this.player.squaredDistanceTo(target);
        double allowedSq = BlockReach.getReachSq(this.player);

        if (distSq > allowedSq) {
            // Слишком далеко — отменяем обработку пакета взаимодействия
            ci.cancel();
        }
    }
}