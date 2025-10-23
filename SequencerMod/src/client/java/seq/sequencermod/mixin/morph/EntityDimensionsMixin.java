package seq.sequencermod.mixin.morph;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
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
 * 1) Индивидуальный размер игрока (PlayerClientSizes)
 * 2) Размеры морфа от сервера (MorphClientSizes)
 * 3) Ванильные размеры по типу (MorphResolver)
 */
@Mixin(Entity.class)
public abstract class EntityDimensionsMixin {

    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void sequencer$applyAnySize(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof PlayerEntity player)) return;

        // 1) Индивидуальный размер игрока
        var ps = PlayerClientSizes.get(player.getUuid());
        if (ps != null) {
            cir.setReturnValue(EntityDimensions.changing(ps.width, ps.height));
            return;
        }

        // 2) Размеры морфа от сервера
        Identifier id = MorphClientSync.getMorphType(player.getUuid());
        if (id != null) {
            var e = MorphClientSizes.get(player.getUuid());
            if (e != null && id.equals(e.morphId)) {
                cir.setReturnValue(EntityDimensions.changing(e.width, e.height));
                return;
            }

            // 3) Fallback — ванильные размеры по типу
            var resolved = MorphResolver.resolve(player, pose, id);
            if (resolved != null && resolved.dimensions != null) {
                cir.setReturnValue(resolved.dimensions);
            }
        }
    }
}