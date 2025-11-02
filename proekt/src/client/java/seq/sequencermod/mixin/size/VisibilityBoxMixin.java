package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.size.PlayerClientSizes;
import seq.sequencermod.size.PlayerSizeData;

@Environment(EnvType.CLIENT)
@Mixin(value = Entity.class, priority = 1) // priority=1 -> выполняемся последними и "перебиваем" других
public abstract class VisibilityBoxMixin {

    // === ПАРАМЕТРЫ ДЛЯ ПОДСТРОЙКИ ===
    // tiny-порог: если любая грань AABB меньше этого — считаем tiny
    private static final double TINY_THRESHOLD = 0.25;
    // минимальный "видимый" объём (под ванильную модель) + запас
    private static final double MIN_VIS_W = 1.00;
    private static final double MIN_VIS_D = 1.00;
    private static final double MIN_VIS_H = 2.40;
    private static final double EXPAND_MARGIN = 0.30; // доп. запас по всем осям
    private static final boolean CENTER_ON_XZ = true;  // центрировать расширение по X/Z
    private static final boolean DEBUG_LOG = false;    // true -> печатать в лог, когда срабатывает

    @Inject(method = "getVisibilityBoundingBox", at = @At("HEAD"), cancellable = true)
    private void sequencer$expandForTiny(CallbackInfoReturnable<Box> cir) {
        Entity self = (Entity)(Object) this;
        if (!(self instanceof PlayerEntity p)) return;

        Box bb = self.getBoundingBox();
        double w = bb.getXLength(), h = bb.getYLength(), d = bb.getZLength();

        boolean tinyByBox = (h < TINY_THRESHOLD) || (w < TINY_THRESHOLD) || (d < TINY_THRESHOLD);

        // Доп. индикатор tiny — синхронизированные размеры (если есть)
        boolean tinyByData = false;
        PlayerSizeData data = PlayerClientSizes.get(p.getUuid());
        if (data != null) {
            tinyByData = (data.height < TINY_THRESHOLD) || (data.width < TINY_THRESHOLD);
        }

        if (!(tinyByBox || tinyByData)) return;

        double targetW = Math.max(w, MIN_VIS_W);
        double targetD = Math.max(d, MIN_VIS_D);
        double targetH = Math.max(h, MIN_VIS_H);

        Box vis;
        if (CENTER_ON_XZ) {
            double cx = (bb.minX + bb.maxX) * 0.5;
            double cz = (bb.minZ + bb.maxZ) * 0.5;
            double halfW = targetW * 0.5;
            double halfD = targetD * 0.5;
            double minY = bb.minY;
            double maxY = Math.max(bb.maxY, bb.minY + targetH);
            vis = new Box(cx - halfW, minY, cz - halfD, cx + halfW, maxY, cz + halfD);
        } else {
            double minX = bb.minX, minZ = bb.minZ;
            double maxX = Math.max(bb.maxX, bb.minX + targetW);
            double maxZ = Math.max(bb.maxZ, bb.minZ + targetD);
            double minY = bb.minY;
            double maxY = Math.max(bb.maxY, bb.minY + targetH);
            vis = new Box(minX, minY, minZ, maxX, maxY, maxZ);
        }

        vis = vis.expand(EXPAND_MARGIN);
        if (DEBUG_LOG) {
            System.out.printf("[VisBox] expand %s: box=(%.3f,%.3f,%.3f) -> vis=(%.3f,%.3f,%.3f)%n",
                    p.getEntityName(), w, h, d, vis.getXLength(), vis.getYLength(), vis.getZLength());
        }
        cir.setReturnValue(vis);
    }
}