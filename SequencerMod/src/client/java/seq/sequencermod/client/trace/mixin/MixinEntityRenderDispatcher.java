package seq.sequencermod.client.trace.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import seq.sequencermod.client.trace.TraceLog;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {

    // Yarn 1.20.1: render(Lnet/minecraft/entity/Entity;DDDFFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V
    @Inject(method = "render(Lnet/minecraft/entity/Entity;DDDFFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"))
    private void seq_trace$entityRenderHead(Entity e, double x, double y, double z, float yaw, float tickDelta,
                                            MatrixStack matrices, VertexConsumerProvider providers, int light, CallbackInfo ci) {
        Identifier id = Registries.ENTITY_TYPE.getId(e.getType());
        TraceLog.begin("EntityRender", "%s at(%.2f,%.2f,%.2f) yaw=%.1f dt=%.3f", (id != null ? id.toString() : "<unknown>"), x, y, z, yaw, tickDelta);
    }

    @Inject(method = "render(Lnet/minecraft/entity/Entity;DDDFFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("TAIL"))
    private void seq_trace$entityRenderTail(Entity e, double x, double y, double z, float yaw, float tickDelta,
                                            MatrixStack matrices, VertexConsumerProvider providers, int light, CallbackInfo ci) {
        Identifier id = Registries.ENTITY_TYPE.getId(e.getType());
        TraceLog.end("EntityRender", "%s done", (id != null ? id.toString() : "<unknown>"));
    }
}