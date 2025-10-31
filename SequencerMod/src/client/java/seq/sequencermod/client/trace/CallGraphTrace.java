package seq.sequencermod.client.trace;

import java.util.Arrays;

public final class CallGraphTrace {
    private CallGraphTrace() {}

    private static final String PKG = "seq.sequencermod.client";
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    public static void onEnter(String calleeClass, String calleeMethod, Object[] args) {
        if (!(TraceCfg.ENABLED && TraceCfg.TRACE_EDGES)) return;

        // Ищем «настоящего» caller-а в стеке (первый кадр из нашего пакета, но не из .trace и не сам аспект)
        String caller = "<external>";
        try {
            StackTraceElement[] st = Thread.currentThread().getStackTrace();
            for (StackTraceElement e : st) {
                String cls = e.getClassName();
                if (cls == null) continue;
                if (!cls.startsWith(PKG)) continue;
                if (cls.startsWith(PKG + ".trace")) continue; // пропускаем трассирующие классы
                // сам callee может тоже попасть в стек — пропустим его кадр как caller
                if (cls.equals(calleeClass) && e.getMethodName().equals(calleeMethod)) continue;
                caller = cls + "." + e.getMethodName();
                break;
            }
        } catch (Throwable ignored) {}

        int d = DEPTH.get();
        String indent = (d <= 0) ? "" : "  ".repeat(Math.min(d, 40));
        String argInfo = TraceCfg.VERBOSITY >= 2
                ? "args=" + safeArgs(args)
                : "args=" + (args == null ? 0 : args.length);

        TraceLog.log("%sCALL: %s -> %s.%s(%s)", indent, caller, calleeClass, calleeMethod, argInfo);
        DEPTH.set(d + 1);
    }

    public static void onExit(String calleeClass, String calleeMethod) {
        if (!(TraceCfg.ENABLED && TraceCfg.TRACE_EDGES)) return;
        int d = Math.max(0, DEPTH.get() - 1);
        DEPTH.set(d);
        if (TraceCfg.VERBOSITY >= 2) {
            String indent = (d <= 0) ? "" : "  ".repeat(Math.min(d, 40));
            TraceLog.log("%sRET: %s.%s()", indent, calleeClass, calleeMethod);
        }
    }

    private static String safeArgs(Object[] args) {
        try {
            if (args == null || args.length == 0) return "[]";
            // не зовём тяжёлые toString у MC-объектов — только классы
            String[] a = Arrays.stream(args)
                    .map(o -> o == null ? "null" : o.getClass().getSimpleName())
                    .toArray(String[]::new);
            return Arrays.toString(a);
        } catch (Throwable t) {
            return "[?]";
        }
    }
}