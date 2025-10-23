package seq.sequencermod.mixin.accessor;

import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.Entity;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EntityRenderDispatcher.class)
public interface EntityRenderDispatcherAccessor {

    // Invoker на private void renderShadow(MatrixStack, VertexConsumerProvider, Entity, float radius, float tickDelta, WorldView, float opacity)
    @Invoker("renderShadow")
    void sequencer$invokeRenderShadow(MatrixStack matrices,
                                      VertexConsumerProvider vertexConsumers,
                                      Entity entity,
                                      float radius,
                                      float tickDelta,
                                      WorldView world,
                                      float opacity);
}