package seq.sequencermod.net.client.morph;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Публичные рефлекс‑утилиты для модулей SimpleMorphsLiving/Projectiles/Special.
 * Вынесены в отдельный пакет seq.sequencermod.net.client.morph,
 * чтобы исправить ошибки "Cannot resolve symbol 'morph'/'MorphEngine'".
 */
public final class MorphEngine {
    private MorphEngine() {}

    public static boolean tryInvokeMethod(Object target, String name, Class<?>[] paramTypes, Object... args) {
        if (target == null || name == null) return false;
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                Method m = c.getDeclaredMethod(name, paramTypes);
                m.setAccessible(true);
                m.invoke(target, args);
                return true;
            } catch (NoSuchMethodException ignored) {
            } catch (Throwable t) {
                return false;
            }
            c = c.getSuperclass();
        }
        return false;
    }

    public static void trySetField(Object target, String field, Object value) {
        if (target == null || field == null) return;
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(field);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (Throwable ignored) {}
            c = c.getSuperclass();
        }
    }

    public static void trySetFieldFloat(Object target, String field, float value) {
        if (target == null || field == null) return;
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(field);
                f.setAccessible(true);
                if (f.getType() == float.class) {
                    f.setFloat(target, value);
                } else {
                    f.set(target, value);
                }
                return;
            } catch (Throwable ignored) {}
            c = c.getSuperclass();
        }
    }
}