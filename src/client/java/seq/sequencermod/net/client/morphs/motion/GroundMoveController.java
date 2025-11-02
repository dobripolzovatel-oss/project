package seq.sequencermod.net.client.morphs.motion;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Сглаживание направления на земле (идея MoveControl):
 * - смотрим по направлению движения
 * - мягко подтягиваем yaw, немного кручим pitch от уклонов
 */
public final class GroundMoveController {
    private float renderYaw;
    private float renderPitch;

    public void tick(Vec3d vel, float currentYaw, float currentPitch, float maxTurnDeg) {
        double horiz = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        if (horiz > 1.0E-4) {
            float targetYaw = (float)(MathHelper.atan2(vel.z, vel.x) * (180F / Math.PI)) - 90.0F;
            renderYaw = rotLerp(renderYaw == 0 ? currentYaw : renderYaw, targetYaw, maxTurnDeg);
        } else {
            // без движения держим текущий
            renderYaw = rotLerp(renderYaw == 0 ? currentYaw : renderYaw, currentYaw, maxTurnDeg);
        }
        // Pitch слегка от наклона/вертикальной скорости (минимально)
        float targetPitch = MathHelper.clamp((float)(-vel.y) * 10.0f, -10.0f, 10.0f);
        renderPitch = rotLerp(renderPitch == 0 ? currentPitch : renderPitch, targetPitch, maxTurnDeg * 0.5f);
    }

    private static float rotLerp(float from, float to, float maxChange) {
        float f = MathHelper.wrapDegrees(to - from);
        if (f > maxChange) f = maxChange;
        if (f < -maxChange) f = -maxChange;
        return from + f;
    }

    public float getRenderYaw() { return renderYaw; }
    public float getRenderPitch() { return renderPitch; }
}