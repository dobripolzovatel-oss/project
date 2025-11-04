package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(EntityRenderer.class)
public abstract class PlayerAlwaysRenderTinyMixin {

    private static final double TINY_THRESHOLD = 0.25; // синхронизуем с VisibilityBoxMixin

    // shouldRender(Entity, Frustum, double x, double y, double z)Z
    @Inject(
            method = "shouldRender(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/Frustum;DDD)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sequencer$forceRenderTiny(Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof PlayerEntity)) return;

        Box bb = entity.getBoundingBox();
        if (bb.getXLength() < TINY_THRESHOLD || bb.getYLength() < TINY_THRESHOLD || bb.getZLength() < TINY_THRESHOLD) {
            // Используем visibility box (который уже расширили) для финальной проверки
            // Если по каким-то причинам frustum == null, не рискуем — разрешаем рендер
            if (frustum == null) {
                cir.setReturnValue(true);
            } else {
                cir.setReturnValue(frustum.isVisible(entity.getVisibilityBoundingBox()));
            }
        }
    }
}