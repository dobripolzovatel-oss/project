package seq.sequencermod.client.trace;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

public final class TraceLog {
    private TraceLog() {}

    private static final DateTimeFormatter TS = DateTimeFormatter.ISO_INSTANT;
    private static volatile long lastSec = System.currentTimeMillis() / 1000L;
    private static final AtomicInteger perSec = new AtomicInteger(0);

    public static void log(String fmt, Object... args) {
        if (!TraceCfg.ENABLED) return;
        if (!rateOk()) return;
        String msg;
        try {
            msg = (args == null || args.length == 0) ? fmt : String.format(fmt, args);
        } catch (Throwable t) {
            msg = fmt + " [format-error:" + t + "]";
        }
        String th = Thread.currentThread().getName();
        System.out.println("[SequencerTrace] " + TS.format(Instant.now()) + " [" + th + "] " + msg);
    }

    // Удобные обёртки для «начало/конец»
    public static void begin(String tag, String fmt, Object... args) {
        if (!TraceCfg.ENABLED) return;
        if (fmt == null || fmt.isEmpty()) {
            log("→ %s", tag);
        } else {
            log("→ %s | " + fmt, concat(tag, args));
        }
    }

    public static void end(String tag, String fmt, Object... args) {
        if (!TraceCfg.ENABLED) return;
        if (fmt == null || fmt.isEmpty()) {
            log("← %s", tag);
        } else {
            log("← %s | " + fmt, concat(tag, args));
        }
    }

    private static Object[] concat(Object first, Object[] rest) {
        if (rest == null || rest.length == 0) return new Object[]{ first };
        Object[] out = new Object[rest.length + 1];
        out[0] = first;
        System.arraycopy(rest, 0, out, 1, rest.length);
        return out;
    }

    private static boolean rateOk() {
        if (TraceCfg.MAX_PER_SEC <= 0) return true;
        long sec = System.currentTimeMillis() / 1000L;
        if (sec != lastSec) {
            lastSec = sec;
            perSec.set(0);
        }
        return perSec.incrementAndGet() <= TraceCfg.MAX_PER_SEC;
    }
}