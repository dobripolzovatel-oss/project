package seq.sequencermod.client.trace;

public final class TraceCfg {
    private TraceCfg() {}

    // Глобальный выключатель всех трассировок (включая ребра вызовов)
    // Включить: -Dsequencer.trace=1
    public static final boolean ENABLED = readBool("sequencer.trace", false);

    // Трассировка «caller -> callee» для всего пакета seq.sequencermod.client..*
    // Включить: -Dsequencer.trace.edges=1
    public static final boolean TRACE_EDGES = readBool("sequencer.trace.edges", false);

    // Уровень детализации: 0 — минимум, 1 — стандарт, 2 — подробно (например, аргументы)
    // Пример: -Dsequencer.trace.verbosity=2
    public static final int VERBOSITY = readInt("sequencer.trace.verbosity", 1, 0, 2);

    // Ограничение спама: макс. сообщений в секунду (0 — без лимита)
    // Пример: -Dsequencer.trace.rate=4000
    public static final int MAX_PER_SEC = readInt("sequencer.trace.rate", 2000, 0, Integer.MAX_VALUE);

    private static boolean readBool(String key, boolean def) {
        try {
            String v = System.getProperty(key, def ? "1" : "0").trim();
            return "1".equals(v) || "true".equalsIgnoreCase(v) || "yes".equalsIgnoreCase(v);
        } catch (Throwable ignored) { return def; }
    }
    private static int readInt(String key, int def, int min, int max) {
        try {
            int v = Integer.getInteger(key, def);
            if (v < min) v = min;
            if (v > max) v = max;
            return v;
        } catch (Throwable ignored) { return def; }
    }
}