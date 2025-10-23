package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import seq.sequencermod.size.util.WhiteHitboxScale;

/**
 * Приводим "красную" отладочную рамку к БЕЛОМУ хитбоксу:
 * вычисления размеров/смещений делаем от текущего AABB, а не от nominal dimensions,
 * чтобы визуально совпадало на микро-размерах.
 */
@Environment(EnvType.CLIENT)
@Mixin(EntityRenderDispatcher.class)
public abstract class HitboxEyeBoxMixin {

    private static boolean isPlayerRedCall(Args args, int rIndex, Entity entity) {
        if (!(entity instanceof PlayerEntity)) return false;
        float r = ((Number) args.get(rIndex)).floatValue();
        float g = ((Number) args.get(rIndex + 1)).floatValue();
        float b = ((Number) args.get(rIndex + 2)).floatValue();
        return r > 0.9f && g < 0.1f && b < 0.1f;
    }

    private static float clampHalfX(float width) {
        final float eps = WhiteHitboxScale.EPS_HEIGHT;
        double maxHalf = Math.max(eps, (width * 0.5) - eps);
        return (float) (maxHalf <= eps ? Math.max(eps, width * 0.45) : maxHalf);
    }

    private static float clampHalfY(float height) {
        final float eps = WhiteHitboxScale.EPS_HEIGHT;
        return Math.max(eps, height * 0.25f);
    }

    @ModifyArgs(
            method = "renderHitbox",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/WorldRenderer;drawBox(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/util/math/Box;FFFF)V"
            ),
            require = 0
    )
    private static void shrinkRed_Box(Args args, MatrixStack matrices, VertexConsumer vertices, Entity entity, float tickDelta) {
        if (!isPlayerRedCall(args, 3, entity)) return;
        if (!(args.get(2) instanceof Box box)) return;

        // Берём размеры именно текущего AABB (белого)
        float w = (float) (box.getXLength());
        float h = (float) (box.getYLength());

        double cx = (box.minX + box.maxX) * 0.5;
        double cy = (box.minY + box.maxY) * 0.5;
        double cz = (box.minZ + box.maxZ) * 0.5;

        float s = Math.max(WhiteHitboxScale.MIN_SCALE, h / WhiteHitboxScale.BASE_PLAYER_HEIGHT);
        double halfX = Math.max(WhiteHitboxScale.EPS_HEIGHT, Math.min(0.25D * Math.sqrt(s), clampHalfX(w)));
        double halfY = Math.max(WhiteHitboxScale.EPS_HEIGHT, Math.min(0.01D  * Math.sqrt(s), clampHalfY(h)));

        args.set(2, new Box(cx - halfX, cy - halfY, cz - halfX, cx + halfX, cy + halfY, cz + halfX));
    }

    @ModifyArgs(
            method = "renderHitbox",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/WorldRenderer;drawBox(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;DDDDDDFFFF)V"
            ),
            require = 0
    )
    private static void shrinkRed_Coords(Args args, MatrixStack matrices, VertexConsumer vertices, Entity entity, float tickDelta) {
        if (!isPlayerRedCall(args, 8, entity)) return;

        double minX = ((Number) args.get(2)).doubleValue();
        double minY = ((Number) args.get(3)).doubleValue();
        double minZ = ((Number) args.get(4)).doubleValue();
        double maxX = ((Number) args.get(5)).doubleValue();
        double maxY = ((Number) args.get(6)).doubleValue();
        double maxZ = ((Number) args.get(7)).doubleValue();

        double cx = (minX + maxX) * 0.5, cy = (minY + maxY) * 0.5, cz = (minZ + maxZ) * 0.5;

        float w = (float) (maxX - minX);
        float h = (float) (maxY - minY);

        float s = Math.max(WhiteHitboxScale.MIN_SCALE, h / WhiteHitboxScale.BASE_PLAYER_HEIGHT);
        double halfX = Math.max(WhiteHitboxScale.EPS_HEIGHT, Math.min(0.25D * Math.sqrt(s), clampHalfX(w)));
        double halfY = Math.max(WhiteHitboxScale.EPS_HEIGHT, Math.min(0.01D  * Math.sqrt(s), clampHalfY(h)));

        args.set(2, cx - halfX); args.set(3, cy - halfY); args.set(4, cz - halfX);
        args.set(5, cx + halfX); args.set(6, cy + halfY); args.set(7, cz + halfX);
    }
}