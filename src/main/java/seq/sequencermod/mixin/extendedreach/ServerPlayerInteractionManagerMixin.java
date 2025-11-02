/*
 * GENERATED-SKELETON
 */
package seq.sequencermod.mixin.extendedreach;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.extendedreach.BlockReach;

/**
 * Устойчивый вариант: HEAD-инжект в tryBreakBlock(BlockPos)Z с собственной проверкой дистанции.
 * Это снимает проблемы @ModifyConstant с отсутствующими константами/сигнатурами в разных ревизиях.
 */
@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManagerMixin {
    @Shadow protected ServerPlayerEntity player;

    @Inject(
            method = "tryBreakBlock(Lnet/minecraft/util/math/BlockPos;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void sequencermod$guardReach(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        // Позиция глаз игрока и центр блока
        Vec3d eyes = this.player.getEyePos();
        double distSq = eyes.squaredDistanceTo(Vec3d.ofCenter(pos));

        // Сервер-авторитетный квадрат дистанции
        double allowedSq = BlockReach.getReachSq(this.player);

        if (distSq > allowedSq) {
            // Слишком далеко — запрещаем попытку ломать блок
            cir.setReturnValue(false);
        }
    }
}