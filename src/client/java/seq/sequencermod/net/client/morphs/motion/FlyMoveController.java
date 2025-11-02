package seq.sequencermod.net.client.morphs.motion;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Сглаживание полёта на основе FlightMoveControl:
 * - rotLerp по yaw/pitch
 * - pitch от atan2(vy, horizSpeed)
 * - yaw от atan2(vx, vz)
 * Возвращает "визуальные" yaw/pitch/roll для позирования модели.
 */
public final class FlyMoveController {
    private float renderYaw;
    private float renderPitch;
    private float renderRoll; // небольшой наклон при повороте

    public void tick(Vec3d vel, float currentYaw, float currentPitch, float maxTurnDeg) {
        double horiz = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        float targetYaw = (float)(MathHelper.atan2(vel.z, vel.x) * (180F / Math.PI)) - 90.0F;
        float targetPitch = (float)(-(MathHelper.atan2(vel.y, horiz) * (180F / Math.PI)));

        renderYaw = rotLerp(renderYaw == 0 ? currentYaw : renderYaw, targetYaw, maxTurnDeg);
        renderPitch = rotLerp(renderPitch == 0 ? currentPitch : renderPitch, targetPitch, maxTurnDeg);

        // Имитация крена при повороте (roll) по разнице целевого и текущего yaw
        float yawDelta = MathHelper.wrapDegrees(targetYaw - renderYaw);
        renderRoll += MathHelper.clamp(yawDelta * 0.2f - renderRoll, -3.0f, 3.0f) * 0.25f;
    }

    private static float rotLerp(float from, float to, float maxChange) {
        float f = MathHelper.wrapDegrees(to - from);
        if (f > maxChange) f = maxChange;
        if (f < -maxChange) f = -maxChange;
        return from + f;
    }

    public float getRenderYaw() { return renderYaw; }
    public float getRenderPitch() { return renderPitch; }
    public float getRenderRoll() { return renderRoll; }
}