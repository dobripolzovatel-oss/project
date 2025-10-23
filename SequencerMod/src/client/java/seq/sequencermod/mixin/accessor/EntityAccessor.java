package seq.sequencermod.mixin.accessor;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Invoker("getJumpVelocityMultiplier")
    float sequencer$invokeGetJumpVelocityMultiplier();
}