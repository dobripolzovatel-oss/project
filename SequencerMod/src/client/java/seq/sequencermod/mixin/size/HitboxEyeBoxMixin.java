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
 * Гарантируем, что "красный" дебаг-хитбокс ВСЕГДА строго меньше белого (AABB) и не выходит за его гранцы,
 * монотонно уменьшаясь при любом масштабе вплоть до 1e-5.
 *
 * Работает для обеих сигнатур вызова WorldRenderer.drawBox(...) из EntityRenderDispatcher.renderHitbox(...).
 */
@Environment(EnvType.CLIENT)
@Mixin(EntityRenderDispatcher.class)
public abstract class HitboxEyeBoxMixin {

    // Цвет "красного" бокса в дебаге весьма стабильный: (r≈1, g≈0, b≈0). Подержим с зазором.
    private static boolean isPlayerRedCall(Args args, int rIndex, Entity entity) {
        if (!(entity instanceof PlayerEntity)) return false;
        if (args.size() <= rIndex + 2) return false;
        float r = ((Number) args.get(rIndex)).floatValue();
        float g = ((Number) args.get(rIndex + 1)).floatValue();
        float b = ((Number) args.get(rIndex + 2)).floatValue();
        return r > 0.9f && g < 0.1f && b < 0.1f;
    }

    private static Box shrinkInside(Box white, float shrinkFracXY, float shrinkFracY) {
        // Белые габариты
        double w = white.getXLength();
        double h = white.getYLength();
        double d = white.getZLength();

        // Центр
        double cx = (white.minX + white.maxX) * 0.5;
        double cy = (white.minY + white.maxY) * 0.5;
        double cz = (white.minZ + white.maxZ) * 0.5;

        // Минимальный зазор от границы белого бокса.
        // Пропорционален габариту + абсолютный EPS, чтобы даже при 1e-5 сохранялся строгий "внутрь".
        double baseEps = Math.max(WhiteHitboxScale.EPS_HEIGHT, 1.0e-6);
        double marginX = Math.max(baseEps, 0.01 * w);
        double marginY = Math.max(baseEps, 0.01 * h);
        double marginZ = Math.max(baseEps, 0.01 * d);

        // Целевые половины красного бокса как доля от белого, но строго меньше половины белого минус margin
        double halfWhiteX = 0.5 * w, halfWhiteY = 0.5 * h, halfWhiteZ = 0.5 * d;

        double halfX = Math.max(baseEps, Math.min(halfWhiteX - marginX, shrinkFracXY * halfWhiteX));
        double halfY = Math.max(baseEps, Math.min(halfWhiteY - marginY, shrinkFracY  * halfWhiteY));
        double halfZ = Math.max(baseEps, Math.min(halfWhiteZ - marginZ, shrinkFracXY * halfWhiteZ));

        // На ультра-микро габаритах следим, чтобы margin не "съел" весь объём
        if (halfX <= baseEps) halfX = Math.max(baseEps, 0.45 * halfWhiteX);
        if (halfY <= baseEps) halfY = Math.max(baseEps, 0.45 * halfWhiteY);
        if (halfZ <= baseEps) halfZ = Math.max(baseEps, 0.45 * halfWhiteZ);

        return new Box(cx - halfX, cy - halfY, cz - halfZ, cx + halfX, cy + halfY, cz + halfZ);
    }

    // Вариант с Box
    @ModifyArgs(
            method = "renderHitbox",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/WorldRenderer;drawBox(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/util/math/Box;FFFF)V"
            ),
            require = 0
    )
    private static void sequencer$shrinkRed_Box(Args args, MatrixStack matrices, VertexConsumer vertices, Entity entity, float tickDelta) {
        // Индекс r в сигнатуре drawBox(..., Box, r,g,b,a) — 3
        if (!isPlayerRedCall(args, 3, entity)) return;
        if (!(args.get(2) instanceof Box box)) return;

        // Вертикально сжимаем сильнее (0.60), по X/Z — 0.80. Это гарантирует "красный внутри белого".
        Box shrunk = shrinkInside(box, 0.80f, 0.60f);
        args.set(2, shrunk);
    }

    // Вариант с координатами
    @ModifyArgs(
            method = "renderHitbox",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/WorldRenderer;drawBox(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;DDDDDDFFFF)V"
            ),
            require = 0
    )
    private static void sequencer$shrinkRed_Coords(Args args, MatrixStack matrices, VertexConsumer vertices, Entity entity, float tickDelta) {
        // Индекс r в сигнатуре drawBox(..., minX,minY,minZ,maxX,maxY,maxZ, r,g,b,a) — 8
        if (!isPlayerRedCall(args, 8, entity)) return;

        double minX = ((Number) args.get(2)).doubleValue();
        double minY = ((Number) args.get(3)).doubleValue();
        double minZ = ((Number) args.get(4)).doubleValue();
        double maxX = ((Number) args.get(5)).doubleValue();
        double maxY = ((Number) args.get(6)).doubleValue();
        double maxZ = ((Number) args.get(7)).doubleValue();

        Box white = new Box(minX, minY, minZ, maxX, maxY, maxZ);
        Box shrunk = shrinkInside(white, 0.80f, 0.60f);

        args.set(2, shrunk.minX); args.set(3, shrunk.minY); args.set(4, shrunk.minZ);
        args.set(5, shrunk.maxX); args.set(6, shrunk.maxY); args.set(7, shrunk.maxZ);
    }
}