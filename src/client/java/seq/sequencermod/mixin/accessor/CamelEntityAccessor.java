package seq.sequencermod.mixin.accessor;

import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CamelEntity.class)
public interface CamelEntityAccessor {
    @Invoker("startSitting")
    void sequencer$invokeStartSitting();

    @Invoker("startStanding")
    void sequencer$invokeStartStanding();

    // В Camel/AbstractHorse называется startJumping(int)
    @Invoker("startJumping")
    void sequencer$invokeStartJumping(int strength);

    // Сигнатура jump(float, Vec3d) в CamelEntity
    @Invoker("jump")
    void sequencer$invokeJump(float strength, Vec3d movementInput);
}