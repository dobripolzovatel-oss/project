package seq.sequencermod.client.preview;

import org.lwjgl.opengl.GL11;

public final class AecPreviewProbe {
    private AecPreviewProbe() {}

    // Включено по умолчанию. Можно отключить: -Dsequencer.probe=false
    private static volatile boolean ENABLED = !"false".equalsIgnoreCase(System.getProperty("sequencer.probe", "true"));
    // Дамп GL-состояний (по умолчанию выкл): -Dsequencer.probe.gl=true
    private static volatile boolean GL = "true".equalsIgnoreCase(System.getProperty("sequencer.probe.gl", "false"));

    private static String last = null;
    private static int repeat = 0;
    private static long lastPrintMs = 0L;
    private static int frame = 0;

    public static boolean on() { return ENABLED; }
    public static void setEnabled(boolean v) { ENABLED = v; }
    public static void setGl(boolean v) { GL = v; }

    public static int nextFrame() {
        if (!ENABLED) return ++frame;
        flushRepeats();
        frame++;
        System.out.println("[AEC][PROBE] ---- frame " + frame + " ----");
        return frame;
    }

    public static void rect(String tag, int x, int y, int w, int h) {
        out("%s: rect x=%d y=%d w=%d h=%d", tag, x, y, w, h);
    }

    public static void style(String tag, ManualAECPreviewRenderer.Style s) {
        if (s == null) { out("%s: style=null", tag); return; }
        String tint = (s.tintOverride == null) ? "null" : String.format("0x%08X", s.tintOverride);
        out("%s: style name='%s' sprites=%s baseDisk=%s softPuffs=%s grid=%d density=%.2f alpha=%.2f wobble=%.3f puffMul=%.2f tint=%s",
                tag, s.name, s.renderPotionSprites, s.renderBaseDisk, s.renderSoftPuffs, s.spriteGrid,
                s.spriteDensity, s.spriteAlpha, s.edgeWobble, s.puffCountMul, tint);
    }

    public static void entity(String tag, Object e) {
        out("%s: %s", tag, (e == null ? "null" : e.getClass().getName()));
    }

    public static void out(String fmt, Object... args) {
        if (!ENABLED) return;
        String msg = "[AEC][PROBE] " + String.format(fmt, args);
        dedupPrint(msg);
    }

    public static void gl(String tag) {
        if (!ENABLED || !GL) return;
        try {
            boolean sc = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
            boolean dt = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            boolean bl = GL11.glIsEnabled(GL11.GL_BLEND);
            boolean cu = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            out("%s: GL scissor=%s depth=%s blend=%s cull=%s", tag, sc, dt, bl, cu);
        } catch (Throwable t) {
            out("%s: GL dump failed: %s", tag, t.toString());
        }
    }

    private static void dedupPrint(String s) {
        long now = System.currentTimeMillis();
        if (s.equals(last)) {
            repeat++;
            // каждые 200 одинаковых строк или раз в 1.5 сек — печатаем прогресс
            if (repeat % 200 == 0 || now - lastPrintMs > 1500) {
                System.out.println(s + " (x" + (repeat + 1) + ")");
                lastPrintMs = now;
            }
            return;
        }
        flushRepeats();
        System.out.println(s);
        last = s;
        repeat = 0;
        lastPrintMs = now;
    }

    private static void flushRepeats() {
        if (repeat > 0 && last != null) {
            System.out.println(last + " (x" + (repeat + 1) + ")");
        }
        last = null;
        repeat = 0;
    }
}