package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.size.util.WhiteHitboxScale;

/**
 * CLIENT: высота глаз локального игрока в ПЕРВОМ лице = пропорция от высоты БЕЛОГО хитбокса (текущего AABB).
 * Всегда зажимаем eye внутри AABB: [пол + eps; потолок - eps].
 * Никаких абсолютных нижних порогов, чтобы не "вылетать" над белым боксом на ультра-микро.
 */
@Environment(EnvType.CLIENT)
@Mixin(value = PlayerEntity.class, priority = 1000)
public abstract class PlayerEyeHeightClientMixin {

    private static float ratioForPose(EntityPose pose) {
        if (pose == null) return 0.90f;
        return switch (pose) {
            case SWIMMING, FALL_FLYING, SPIN_ATTACK -> 0.67f;
            case SLEEPING -> -1f;     // сон оставляем ванилле
            case CROUCHING -> 0.90f;
            default -> 0.90f;
        };
    }

    // getActiveEyeHeight(Lnet/minecraft/entity/EntityPose;Lnet/minecraft/entity/EntityDimensions;)F
    @Inject(
            method = "getActiveEyeHeight(Lnet/minecraft/entity/EntityPose;Lnet/minecraft/entity/EntityDimensions;)F",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sequencer$whiteAabbEye(EntityPose pose, EntityDimensions dims, CallbackInfoReturnable<Float> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        // Только локальный игрок и только 1-е лицо
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player != player || !mc.options.getPerspective().isFirstPerson()) return;

        float ratio = ratioForPose(pose);
        if (ratio < 0f) return; // sleeping -> ванилла

        // КЛЮЧ: берём высоту БЕЛОГО хитбокса (AABB), а не nominal dimensions
        float h = WhiteHitboxScale.whiteHeight(player);

        // Базовый eye по пропорции
        float eye = ratio * h;

        // Мягкие зазоры от пола/потолка: в долях от h + минимальный eps
        float floorEps = Math.max(WhiteHitboxScale.EPS_HEIGHT, 0.005f * h); // ~0.5% h
        float topEps   = Math.max(WhiteHitboxScale.EPS_HEIGHT, 0.01f  * h); // ~1% h

        // Жёстко зажимаем внутрь AABB
        float minEye = floorEps;
        float maxEye = Math.max(WhiteHitboxScale.EPS_HEIGHT, h - topEps);
        if (minEye > maxEye) {
            // На экстремально маленьких h слои могут пересечься — сведём eye к центру
            float mid = h * 0.5f;
            eye = Math.max(WhiteHitboxScale.EPS_HEIGHT, Math.min(h - WhiteHitboxScale.EPS_HEIGHT, mid));
        } else {
            eye = Math.max(minEye, Math.min(maxEye, eye));
        }

        // Микро-антиграница
        if (eye < maxEye) {
            float bumped = Math.nextUp(eye);
            if (bumped < maxEye) eye = bumped;
        }

        cir.setReturnValue(eye);
    }
}