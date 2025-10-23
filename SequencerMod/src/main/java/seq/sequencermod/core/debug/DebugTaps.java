package seq.sequencermod.core.debug;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Глобальный тумблер отладочных логов миксинов.
 * По умолчанию ВЫКЛ. Включение: -Dsequencer.debug.mixins=true
 * Дополнительно поддерживается общий флаг: -Dsequencer.debug=true
 */
public final class DebugTaps {
    private DebugTaps() {}

    /**
     * Включатель логов миксинов.
     * Источники:
     * -Dsequencer.debug.mixins=true  (приоритетный)
     * -Dsequencer.debug=true         (общий)
     */
    public static final AtomicBoolean active;

    static {
        boolean mixin = "true".equalsIgnoreCase(System.getProperty("sequencer.debug.mixins", "false"));
        boolean all   = "true".equalsIgnoreCase(System.getProperty("sequencer.debug", "false"));
        boolean on = mixin || all;
        active = new AtomicBoolean(on);
        if (on) {
            System.out.println("[SequencerMixins] Debug logs ENABLED via -Dsequencer.debug.mixins=true");
        }
    }

    public static void enable()  { active.set(true); }
    public static void disable() { active.set(false); }

    public static void log(String msg) {
        if (!active.get()) return;
        System.out.println("[SequencerMixins] " + msg);
    }

    public static void logf(String fmt, Object... args) {
        if (!active.get()) return;
        System.out.println("[SequencerMixins] " + String.format(fmt, args));
    }
}