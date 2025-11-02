package seq.sequencermod.net.client.morphs.adapters;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.util.math.MathHelper;
import seq.sequencermod.net.client.morphs.BaseAdapter;

/**
 * Морф для стрел (обычных, спектральных) и трезубца.
 * Горизонталь:
 *   Vanilla renderer делает (180 - yawEntity), поэтому сначала компенсируем (кладём 180 - headYaw),
 *   затем для стрел (PersistentProjectileEntity) дополнительно вычитаем 180°, чтобы развернуть модель (tip вперёд).
 * Вертикаль:
 *   Инвертируем pitch (камера вверх -> наконечник вниз), если INVERT_PITCH = true.
 */
public class ProjectileAdapter extends BaseAdapter<Entity> {

    private static final boolean INVERT_PITCH = true;
    private static final boolean APPLY_RENDERER_COMPENSATION = true;
    private static final float   MAX_STEP_PER_TICK = 360f;

    @Override
    public boolean shouldClientTick(Entity e) {
        return false;
    }

    @Override
    public void syncFromPlayer(AbstractClientPlayerEntity p, Entity e) {
        // Только позиция; углы задаём позже
        syncCommon(p, e, false, false);
        suppressPhysics(e);

        trySetField(e, "prevX", e.getX());
        trySetField(e, "prevY", e.getY());
        trySetField(e, "prevZ", e.getZ());
    }

    @Override
    public void beforeRender(AbstractClientPlayerEntity p, Entity e, float tickDelta) {
        float headYawInterp = MathHelper.lerpAngleDegrees(tickDelta, p.prevHeadYaw, p.headYaw);
        float pitchInterp   = MathHelper.lerp(tickDelta, p.prevPitch, p.getPitch());

        // Базовая компенсация vanilla формулы (180 - yaw)
        float yawTarget = APPLY_RENDERER_COMPENSATION
                ? (180.0f - headYawInterp)
                : headYawInterp;

        // ДОП. флип для стрел (модель развернута): tip смотрит назад без него
        if (e instanceof PersistentProjectileEntity) {
            yawTarget -= 180.0f;
        }
        // Если обнаружишь что трезубец всё-таки тоже задом — раскомментируй:
        // if (e instanceof TridentEntity) { yawTarget -= 180.0f; }

        yawTarget = MathHelper.wrapDegrees(yawTarget);

        float pitchTarget = INVERT_PITCH ? -pitchInterp : pitchInterp;

        float currYaw = e.getYaw();
        float currPitch = e.getPitch();

        // Ограничение шага (фактически отключено — 360°)
        float yawDiff = MathHelper.wrapDegrees(yawTarget - currYaw);
        if (Math.abs(yawDiff) > MAX_STEP_PER_TICK) {
            yawTarget = currYaw + Math.copySign(MAX_STEP_PER_TICK, yawDiff);
        }

        float pitchDiff = pitchTarget - currPitch;
        if (Math.abs(pitchDiff) > MAX_STEP_PER_TICK) {
            pitchTarget = currPitch + Math.copySign(MAX_STEP_PER_TICK, pitchDiff);
        }

        try { e.setYaw(yawTarget); } catch (Throwable ignored) {}
        trySetField(e, "prevYaw", yawTarget);

        try { e.setPitch(pitchTarget); } catch (Throwable ignored) {}
        trySetField(e, "prevPitch", pitchTarget);

        // Сброс артефактов
        if (e instanceof PersistentProjectileEntity ppe) {
            trySetField(ppe, "shake", 0);
            trySetField(ppe, "inGround", false);
            trySetField(ppe, "critical", false);
        }
        if (e instanceof TridentEntity tr) {
            trySetField(tr, "shake", 0);
            trySetField(tr, "dealtDamage", false);
            trySetField(tr, "inGround", false);
        }

        suppressPhysics(e);

        // Жёсткая фиксация prevXYZ
        trySetField(e, "prevX", e.getX());
        trySetField(e, "prevY", e.getY());
        trySetField(e, "prevZ", e.getZ());
    }

    private void suppressPhysics(Entity e) {
        try { e.setNoGravity(true); } catch (Throwable ignored) {}
        try { e.setOnGround(false); } catch (Throwable ignored) {}
        try { e.setVelocity(0, 0, 0); } catch (Throwable ignored) {}
    }
}