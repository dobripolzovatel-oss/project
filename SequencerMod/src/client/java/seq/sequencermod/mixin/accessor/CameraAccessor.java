package seq.sequencermod.mixin.accessor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(Camera.class)
public interface CameraAccessor {
    @Accessor("pos")
    Vec3d getPos();

    @Accessor("pos")
    void setPos(Vec3d pos);
}