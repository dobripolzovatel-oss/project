package seq.sequencermod.mixin.morph;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.morph.MorphResolver;
import seq.sequencermod.net.client.MorphClientSizes;
import seq.sequencermod.net.client.MorphClientSync;
import seq.sequencermod.net.client.PlayerClientSizes;

/**
 * Порядок на клиенте:
 * 1) Индивидуальный eyeHeight игрока (если задан)
 * 2) eye морфа от сервера
 * 3) Fallback от MorphResolver
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityEyeHeightMixin {

    @Inject(method = "getActiveEyeHeight", at = @At("HEAD"), cancellable = true)
    private void sequencer$applyAnyEye(EntityPose pose, EntityDimensions dims, CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof PlayerEntity player)) return;

        // 1) Индивидуальный eyeHeight
        var ps = PlayerClientSizes.get(player.getUuid());
        if (ps != null && ps.eyeHeight != null) {
            cir.setReturnValue(ps.eyeHeight);
            return;
        }

        // 2) eye морфа
        Identifier id = MorphClientSync.getMorphType(player.getUuid());
        if (id != null) {
            var e = MorphClientSizes.get(player.getUuid());
            if (e != null && id.equals(e.morphId) && e.eyeHeight != null) {
                cir.setReturnValue(e.eyeHeight);
                return;
            }

            // 3) Fallback из MorphResolver
            var resolved = MorphResolver.resolve((PlayerEntity) self, pose, id);
            if (resolved != null && resolved.eyeHeightOverride != null) {
                cir.setReturnValue(resolved.eyeHeightOverride);
            }
        }
    }
}