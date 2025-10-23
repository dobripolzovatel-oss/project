package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import seq.sequencermod.size.util.SizeCalc;

/**
 * Масштабирует базовую дистанцию 3-го лица под размер игрока.
 * Патчит константу 4.0 внутри Camera.update(...).
 */
@Environment(EnvType.CLIENT)
@Mixin(Camera.class)
public abstract class CameraThirdPersonDistanceMixin {

    @ModifyConstant(method = "update", constant = @org.spongepowered.asm.mixin.injection.Constant(doubleValue = 4.0), require = 0)
    private double sequencer$scaleThirdPersonBaseDistance(double original) {
        PlayerEntity p = MinecraftClient.getInstance() != null ? MinecraftClient.getInstance().player : null;
        if (p == null) return original;

        float h = Math.max(SizeCalc.EPS, p.getDimensions(p.getPose()).height);
        double scale = Math.max(0.00001f, h / 1.8f);
        return original * scale;
    }
}