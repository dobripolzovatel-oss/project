/*
 * GENERATED-SKELETON
 */
package seq.sequencermod.mixin.client.extendedreach;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import seq.sequencermod.extendedreach.ExtendedReachClient;

/**
 * Заменяет константы дальности в GameRenderer#updateTargetedEntity(F)V.
 * В 1.20.1 встречаются 3.0D (обычный) и 6.0D (креатив).
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @ModifyConstant(method = "updateTargetedEntity(F)V", constant = @Constant(doubleValue = 6.0D), require = 0)
    private double sequencermod$replaceReach6(double original) {
        return ExtendedReachClient.getReach();
    }

    @ModifyConstant(method = "updateTargetedEntity(F)V", constant = @Constant(doubleValue = 3.0D), require = 0)
    private double sequencermod$replaceReach3(double original) {
        return ExtendedReachClient.getReach();
    }
}