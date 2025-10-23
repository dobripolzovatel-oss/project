package seq.sequencermod.client.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import seq.sequencermod.disguise.render.DisguiseRenderable; // ВАЖНО: клиентский интерфейс

import java.util.Collection;

/**
 * Рендер превью дисгайзов в GUI.
 * Работает через DisguiseClientRegistry и DisguiseRenderable (client-side).
 */
public final class DisguisePreviewRenderer {
    private final PreviewState state;

    public DisguisePreviewRenderer(PreviewState state) {
        this.state = state;
    }

    /**
     * Рендер "основного" превью в прямоугольнике: центрирование, подбор размера, вращение.
     * areaX/areaY/areaW/areaH — координаты прямоугольника превью в экранных пикселях.
     */
    public void renderMain(Identifier disguiseId, DrawContext ctx, int areaX, int areaY, int areaW, int areaH, float tickDelta) {
        if (disguiseId == null) {
            drawPlaceholder(ctx, areaX, areaY, areaW, areaH, "No disguise");
            return;
        }
        DisguiseRenderable r = DisguiseClientRegistry.resolve(disguiseId);
        if (r == null) {
            drawPlaceholder(ctx, areaX, areaY, areaW, areaH, disguiseId.toString());
            return;
        }
        int centerX = areaX + areaW / 2;
        int bottomY = areaY + areaH - 6; // небольшой отступ снизу
        int size = Math.min(areaW, areaH) - 12; // рамка
        float yaw = state.getYaw(disguiseId, 0f);
        float pitch = state.getPitch(disguiseId, 10f);

        // Подложка (опционально)
        drawFrame(ctx, areaX, areaY, areaW, areaH);

        // Рендер самой модели
        r.renderInGui(ctx, centerX, bottomY, size, yaw, pitch, tickDelta);
    }

    /**
     * Рендер ячейки мини‑превью. x/y — верхний левый угол ячейки; size — размер квадрата.
     */
    public void renderCell(Identifier disguiseId, DrawContext ctx, int x, int y, int size, float tickDelta, boolean drawBorder, boolean drawLabel) {
        if (disguiseId == null) {
            drawPlaceholder(ctx, x, y, size, size, "—");
            return;
        }
        DisguiseRenderable r = DisguiseClientRegistry.resolve(disguiseId);
        if (drawBorder) {
            drawFrame(ctx, x, y, size, size);
        }
        if (r == null) {
            drawPlaceholder(ctx, x, y, size, size, "N/A");
            if (drawLabel) {
                ctx.drawText(MinecraftClient.getInstance().textRenderer, disguiseId.toString(), x, y + size + 2, 0xFFFFFF, false);
            }
            return;
        }
        int centerX = x + size / 2;
        int bottomY = y + size;
        float yaw = state.getYaw(disguiseId, 0f);
        float pitch = state.getPitch(disguiseId, 5f);
        r.renderInGui(ctx, centerX, bottomY, size, yaw, pitch, tickDelta);

        if (drawLabel) {
            ctx.drawText(MinecraftClient.getInstance().textRenderer, shortId(disguiseId), x, y + size + 2, 0xFFFFFF, false);
        }
    }

    /**
     * Лёгкая авто-крутилка для набора id (например, видимые слоты).
     */
    public void tickAutoRotation(Collection<Identifier> visibleIds, float yawDelta) {
        state.tickAuto(visibleIds, yawDelta);
    }

    private static void drawFrame(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, 0x40000000);
        ctx.drawBorder(x, y, w, h, 0xFF7F7F7F);
    }

    private static void drawPlaceholder(DrawContext ctx, int x, int y, int w, int h, String text) {
        drawFrame(ctx, x, y, w, h);
        var tr = MinecraftClient.getInstance().textRenderer;
        int tw = tr.getWidth(text);
        int th = tr.fontHeight;
        int tx = x + (w - tw) / 2;
        int ty = y + (h - th) / 2;
        ctx.drawText(tr, Text.literal(text), tx, ty, 0xFFAAAAAA, false);
    }

    private static String shortId(Identifier id) {
        String path = id.getPath();
        int slash = path.lastIndexOf('/');
        return id.getNamespace() + ":" + (slash >= 0 ? path.substring(slash + 1) : path);
    }
}