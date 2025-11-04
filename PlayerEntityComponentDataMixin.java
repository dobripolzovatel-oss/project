package seq.sequencermod.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Serialize/deserialize custom components into PlayerEntity custom NBT.
 * Пока заглушка: ничего не делаем.
 */
@Mixin(PlayerEntity.class)
public class PlayerEntityComponentDataMixin {

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void sequencermod$writeCustom(NbtCompound nbt, CallbackInfo ci) {
        // no-op
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    private void sequencermod$readCustom(NbtCompound nbt, CallbackInfo ci) {
        // no-op
    }
}