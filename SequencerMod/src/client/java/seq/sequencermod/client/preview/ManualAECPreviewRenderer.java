package seq.sequencermod.client.preview;

import seq.sequencermod.client.preview.AecPreviewProbe;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/**
 * Превью‑рендер AEC c жёстким сохранением/восстановлением GL‑состояния:
 * - Внутри оффскрин‑рендера всегда переключаемся на PREVIEW_FBO и отключаем scissor для FBO.
 * - После оффскрина ПОЛНОСТЬЮ восстанавливаем viewport и scissor, а также возвращаемся в основной framebuffer.
 */
public final class ManualAECPreviewRenderer {
    private ManualAECPreviewRenderer() {}

    private static final Identifier WHITE_TEX = new Identifier("minecraft", "textures/misc/white.png");

    // ========= OFFSCREEN → BLIT =========

    private static SimpleFramebuffer PREVIEW_FBO = null;
    private static int FBO_W = 0, FBO_H = 0;

    private static void ensureFbo(int w, int h) {
        if (w <= 0 || h <= 0) return;
        if (PREVIEW_FBO != null && FBO_W == w && FBO_H == h) return;
        if (PREVIEW_FBO != null) {
            PREVIEW_FBO.delete();
            PREVIEW_FBO = null;
        }
        PREVIEW_FBO = new SimpleFramebuffer(w, h, true, MinecraftClient.IS_SYSTEM_MAC);
        PREVIEW_FBO.setClearColor(0f, 0f, 0f, 0f);
        FBO_W = w; FBO_H = h;
    }

    // Простая защита GL‑состояния viewport/scissor
    private static final class GlStateGuard {
        final int[] vp = new int[4];
        final int[] sc = new int[4];
        final boolean scEnabled;

        private GlStateGuard(int[] vp, int[] sc, boolean se) {
            System.arraycopy(vp, 0, this.vp, 0, 4);
            System.arraycopy(sc, 0, this.sc, 0, 4);
            this.scEnabled = se;
        }

        static GlStateGuard capture() {
            int[] vp = new int[4];
            int[] sc = new int[4];
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, vp);
            boolean se = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, sc);
            return new GlStateGuard(vp, sc, se);
        }

        void restore() {
            // Восстановить viewport
            RenderSystem.viewport(vp[0], vp[1], vp[2], vp[3]);
            // Восстановить scissor
            if (scEnabled) {
                RenderSystem.enableScissor(sc[0], sc[1], sc[2], sc[3]);
            } else {
                RenderSystem.disableScissor();
            }
        }
    }

    public static void renderOffscreenAndBlit(DrawContext ctx,
                                              int x, int y, int w, int h,
                                              AreaEffectCloudEntity aec,
                                              float tickDelta,
                                              Style style) {
        AecPreviewProbe.out("MAEC FBO: enter dst=(%d,%d %dx%d)", x, y, w, h);
        AecPreviewProbe.out("MAEC FBO: ensureFbo(%d,%d)", w, h);
        AecPreviewProbe.style("MAEC FBO: style", style);

        if (aec == null || style == null || w <= 2 || h <= 2) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        var window = mc.getWindow();

        ensureFbo(w, h);

        // Захватываем текущий GL‑стейт до оффскрина
        GlStateGuard guard = GlStateGuard.capture();
        AecPreviewProbe.gl("before-offscreen");

        try {
            // 1) Рисуем в FBO
            PREVIEW_FBO.beginWrite(true); // также выставит viewport на размер FBO
            RenderSystem.disableScissor(); // оффскрин без Scissor (если не требуется)
            // По желанию: сбросить depth/color
            RenderSystem.clearColor(0f, 0f, 0f, 0f);
            RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, false);

            // ... ваша логика 3D/2D рендера превью ...
            // render(ctx, aec, tickDelta, style) — опущено для краткости

            AecPreviewProbe.gl("after-offscreen");

            // 2) Возвращаем основной framebuffer и BLIT в прямоугольник GUI
            mc.getFramebuffer().beginWrite(true);
            // Важно: основной viewport должен быть ровно размерами окна
            RenderSystem.viewport(0, 0, window.getFramebufferWidth(), window.getFramebufferHeight());
            // Сам blit текстуры FBO → rect GUI (опущено — зависит от вашего пути отрисовки)
            // ctx.drawTexture(...) или ручной fullscreen quad с PREVIEW_FBO.getColorAttachment();
        } finally {
            // 3) Жёстко восстановить исходный viewport/scissor
            guard.restore();
            AecPreviewProbe.gl("after-restore");
        }
    }

    // --------- Вспомогательные типы/рендер Style (оставьте существующую реализацию) ---------
    public static final class Style {
        public final String name;
        public final boolean renderPotionSprites;
        public final boolean renderBaseDisk;
        public final boolean renderSoftPuffs;
        public final Integer tintOverride;
        public final int spriteGrid;
        public final float spriteDensity;
        public final float spriteAlpha;
        public final float edgeWobble;
        public final float puffCountMul;

        private Style(Builder b) {
            this.name = b.name;
            this.renderPotionSprites = b.renderPotionSprites;
            this.renderBaseDisk = b.renderBaseDisk;
            this.renderSoftPuffs = b.renderSoftPuffs;
            this.tintOverride = b.tintOverride;
            this.spriteGrid = b.spriteGrid;
            this.spriteDensity = b.spriteDensity;
            this.spriteAlpha = b.spriteAlpha;
            this.edgeWobble = b.edgeWobble;
            this.puffCountMul = b.puffCountMul;
        }

        public static Builder builder() { return new Builder(); }

        public static final class Builder {
            private String name = "Preview";
            private boolean renderPotionSprites;
            private boolean renderBaseDisk = true;
            private boolean renderSoftPuffs = true;
            private Integer tintOverride;
            private int spriteGrid = 8;
            private float spriteDensity = 1.0f;
            private float spriteAlpha = 1.0f;
            private float edgeWobble = 0.05f;
            private float puffCountMul = 1.0f;

            public Builder name(String v) { name = v; return this; }
            public Builder renderPotionSprites(boolean v) { renderPotionSprites = v; return this; }
            public Builder renderBaseDisk(boolean v) { renderBaseDisk = v; return this; }
            public Builder renderSoftPuffs(boolean v) { renderSoftPuffs = v; return this; }
            public Builder tintOverride(Integer v) { tintOverride = v; return this; }
            public Builder spriteGrid(int v) { spriteGrid = v; return this; }
            public Builder spriteDensity(float v) { spriteDensity = v; return this; }
            public Builder spriteAlpha(float v) { spriteAlpha = v; return this; }
            public Builder edgeWobble(float v) { edgeWobble = v; return this; }
            public Builder puffCountMul(float v) { puffCountMul = v; return this; }

            public Style build() { return new Style(this); }
        }
    }
}