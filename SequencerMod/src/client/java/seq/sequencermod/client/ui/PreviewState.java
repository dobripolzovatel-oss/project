package seq.sequencermod.client.ui;

import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Хранит yaw/pitch для превью дисгайзов и применяет вращение от мыши.
 * Клиентский класс — размещать только в src/client/java.
 */
public final class PreviewState {
    private final Map<Identifier, Float> yaw = new HashMap<>();
    private final Map<Identifier, Float> pitch = new HashMap<>();

    public float getYaw(Identifier id, float def) {
        return yaw.getOrDefault(id, def);
    }

    public float getPitch(Identifier id, float def) {
        return pitch.getOrDefault(id, def);
    }

    public void setYawPitch(Identifier id, float yawDeg, float pitchDeg) {
        yaw.put(id, yawDeg);
        pitch.put(id, clamp(pitchDeg, -60f, 60f));
    }

    public void applyDrag(Identifier id, double dx, double dy) {
        float y = yaw.getOrDefault(id, 0f) + (float) dx;
        float p = pitch.getOrDefault(id, 10f) - (float) dy;
        yaw.put(id, y);
        pitch.put(id, clamp(p, -60f, 60f));
    }

    public void tickAuto(Iterable<Identifier> ids, float yawDelta) {
        for (Identifier id : ids) {
            float y = yaw.getOrDefault(id, 0f);
            yaw.put(id, y + yawDelta);
        }
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}