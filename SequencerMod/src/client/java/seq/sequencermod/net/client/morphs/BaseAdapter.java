package seq.sequencermod.net.client.morphs;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

public abstract class BaseAdapter<E extends Entity> implements MorphAdapter<E> {

    protected static boolean tryInvokeMethod(Object target, String name, Class<?>[] paramTypes, Object... args) {
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

    protected static void tryInvokeBool(Object target, String name, boolean v) {
        if (target == null || name == null || name.isEmpty()) return;
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                Method m = c.getDeclaredMethod(name, boolean.class);
                m.setAccessible(true);
                m.invoke(target, v);
                return;
            } catch (NoSuchMethodException ignored) {
                try {
                    Method m2 = c.getDeclaredMethod(name, Boolean.class);
                    m2.setAccessible(true);
                    m2.invoke(target, v);
                    return;
                } catch (Throwable ignored2) {}
            } catch (Throwable ignored) { return; }
            c = c.getSuperclass();
        }
    }

    protected static void trySetField(Object target, String field, Object value) {
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

    protected static void trySetFieldFloat(Object target, String field, float value) {
        if (target == null || field == null) return;
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(field);
                f.setAccessible(true);
                if (f.getType() == float.class) f.setFloat(target, value);
                else f.set(target, value);
                return;
            } catch (Throwable ignored) {}
            c = c.getSuperclass();
        }
    }

    protected static float readFieldFloat(Object target, String field, float def) {
        if (target == null || field == null) return def;
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(field);
                f.setAccessible(true);
                if (f.getType() == float.class) return f.getFloat(target);
                Object v = f.get(target);
                if (v instanceof Float fv) return fv;
                if (v instanceof Number n) return n.floatValue();
                return def;
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable ignored) {
                return def;
            }
            c = c.getSuperclass();
        }
        return def;
    }

    protected static void assignStableId(Entity e, UUID playerId) {
        int stable = (int) (playerId.getMostSignificantBits() ^ playerId.getLeastSignificantBits());
        if (stable == 0) stable = 1;
        trySetField(e, "id", stable);
    }

    // Синхронизация зеркала с игрока.
    // Изменения:
    // - Если copyYawPitch == true, prevYaw/prevPitch/prevBodyYaw/prevHeadYaw берём из prev* игрока.
    //   Это устраняет «дёргание» при поворотах из-за расхождения интерполяций.
    protected void syncCommon(AbstractClientPlayerEntity p, E e, boolean copyYawPitch, boolean setVelocity) {
        // 1) prev* = текущее состояние сущности (freezePrevAtCurrent)
        trySetField(e, "prevX", e.getX());
        trySetField(e, "prevY", e.getY());
        trySetField(e, "prevZ", e.getZ());
        trySetField(e, "prevYaw", e.getYaw());
        trySetField(e, "prevPitch", e.getPitch());
        if (e instanceof LivingEntity le) {
            trySetField(le, "prevBodyYaw", le.bodyYaw);
            trySetField(le, "prevHeadYaw", le.getHeadYaw());
        }

        // 1a) Сразу положим prev* из игрока, если копируем углы — чтобы интерполяция совпадала с игроком.
        if (copyYawPitch) {
            trySetField(e, "prevYaw", p.prevYaw);
            trySetField(e, "prevPitch", p.prevPitch);
            if (e instanceof LivingEntity le) {
                trySetField(le, "prevBodyYaw", p.prevBodyYaw);
                trySetField(le, "prevHeadYaw", p.prevHeadYaw);
            }
        }

        // 2) Текущее состояние берём из игрока
        e.setPos(p.getX(), p.getY(), p.getZ());

        if (copyYawPitch) {
            try { e.setYaw(p.bodyYaw); } catch (Throwable ignored) {}
            try { e.setPitch(p.getPitch()); } catch (Throwable ignored) {}
        }

        try { e.setOnGround(p.isOnGround()); } catch (Throwable ignored) {}

        if (setVelocity) {
            double dx = p.getX() - p.prevX;
            double dy = p.getY() - p.prevY;
            double dz = p.getZ() - p.prevZ;
            try { e.setVelocity(dx, dy, dz); } catch (Throwable ignored) {}
        }

        if (e instanceof LivingEntity le) {
            try { le.setPose(p.getPose()); } catch (Throwable ignored) {}
            if (copyYawPitch) {
                try { le.setHeadYaw(p.headYaw); } catch (Throwable ignored) {}
                trySetField(le, "bodyYaw", p.bodyYaw);
            }
            try { le.setSprinting(p.isSprinting()); } catch (Throwable ignored) {}
            try { le.setSneaking(p.isSneaking()); } catch (Throwable ignored) {}
            tryInvokeBool(le, "setSwimming", p.isSwimming());
        }
    }
}