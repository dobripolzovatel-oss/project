package seq.sequencermod.mixin.morph;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import seq.sequencermod.net.client.morphs.MorphRuntimeFlags;

@Mixin(EntityRenderDispatcher.class)
public class PlayerShadowMixin {
    // Скрываем тень игрока, когда активен морф (чтобы не было двойной тени).
    @Inject(
            method = "renderShadow(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/entity/Entity;FFLnet/minecraft/world/WorldView;F)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void sequencer$skipPlayerShadow(
            MatrixStack matrices,
            VertexConsumerProvider vertices,
            Entity entity,
            float opacity,
            float tickDelta,
            WorldView world,
            float radius,
            CallbackInfo ci
    ) {
        if (entity instanceof AbstractClientPlayerEntity player && MorphRuntimeFlags.isMorphActive(player)) {
            ci.cancel();
        }
    }
}