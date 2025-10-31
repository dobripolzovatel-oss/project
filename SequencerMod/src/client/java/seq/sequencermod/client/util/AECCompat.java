package seq.sequencermod.client.util;

import java.lang.reflect.Method;

public final class AECCompat {
    private AECCompat() {}

    // Универсальный вызов "swirlFreq"/"swirlFrequency" на билдере; возвращает сам билдер для чейнинга
    @SuppressWarnings("unchecked")
    public static <B> B swirl(B builder, float freq) {
        if (builder == null) return null;
        Class<?> c = builder.getClass();
        try {
            Method m = c.getMethod("swirlFreq", float.class);
            m.invoke(builder, freq);
            return builder;
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable t) {
            return builder;
        }
        try {
            Method m2 = c.getMethod("swirlFrequency", float.class);
            m2.invoke(builder, freq);
        } catch (Throwable ignored) {}
        return builder;
    }
}