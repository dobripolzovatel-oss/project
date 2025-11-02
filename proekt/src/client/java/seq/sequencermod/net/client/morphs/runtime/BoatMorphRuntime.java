package seq.sequencermod.net.client.morphs.runtime;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Runtime лодочного морфа:
 *  - плавное "подтягивание" yaw к углу тела игрока
 *  - локальная эмуляция ввода (W/A/S/D) -> флаги гребли
 *  - фазы вёсел (аналог vanilla: increment = PI/8)
 *  - интерполяция фазы при рендере как в BoatEntity.interpolatePaddlePhase
 *  - сглаживание (SMOOTH) переходов позиций весла (старт/стоп) по pitch/yaw
 */
public final class BoatMorphRuntime {

    private static final Map<UUID, BoatMorphRuntime> REG = new HashMap<>();
    public static BoatMorphRuntime get(UUID id) {
        return REG.computeIfAbsent(id, u -> new BoatMorphRuntime());
    }

    // --- Константы ---
    private static final float PHASE_STEP = 0.3926991f;     // π/8
    private static final float YAW_SMOOTH = 0.25f;          // подтягивание корпуса
    private static final float PADDLE_SMOOTH = 0.25f;       // сглаживание анимации весёл

    // Фазы
    private final float[] phase = new float[2];
    private final boolean[] moving = new boolean[2];

    // Сглаженные углы (pitch/yaw) текущего кадра
    private final float[] smoothPitch = new float[2];
    private final float[] smoothYaw   = new float[2];
    private boolean paddlesInit = false;

    // Плавный yaw лодки
    private float yaw;
    private float prevYaw;
    private boolean initialized = false;

    // Ввод текущего тика
    private boolean pressF, pressB, pressL, pressR;

    // chest boat ли
    private boolean chest;

    public void setChest(boolean chest) {
        this.chest = chest;
    }
    public boolean isChest() {
        return chest;
    }

    public void clientTick(AbstractClientPlayerEntity player) {
        if (!initialized) {
            yaw = player.bodyYaw;
            prevYaw = yaw;
            initialized = true;
        }
        prevYaw = yaw;

        // Ввод
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.options != null) {
            pressF = mc.options.forwardKey.isPressed();
            pressB = mc.options.backKey.isPressed();
            pressL = mc.options.leftKey.isPressed();
            pressR = mc.options.rightKey.isPressed();
        } else {
            pressF = pressB = pressL = pressR = false;
        }

        // Целевой yaw
        float target = player.bodyYaw;
        float diff = MathHelper.wrapDegrees(target - yaw);
        yaw += diff * YAW_SMOOTH;

        // Логика движущихся весёл (аналог vanilla setPaddleMovings):
        boolean leftMove  = (pressR && !pressL) || pressF;
        boolean rightMove = (pressL && !pressR) || pressF;
        moving[0] = leftMove;
        moving[1] = rightMove;

        // Фазы (как vanilla конец tick)
        for (int i = 0; i < 2; i++) {
            if (moving[i]) {
                phase[i] += PHASE_STEP;
            } else {
                phase[i] = 0f;
            }
        }
    }

    /**
     * Интерполированная фаза весла (BoatEntity.interpolatePaddlePhase).
     */
    public float interpolatedPhase(int index, float tickDelta) {
        if (moving[index]) {
            float start = phase[index] - PHASE_STEP;
            float end = phase[index];
            tickDelta = MathHelper.clamp(tickDelta, 0f, 1f);
            return start + (end - start) * tickDelta;
        }
        return 0f;
    }

    public float getRenderYaw(float tickDelta) {
        return prevYaw + (yaw - prevYaw) * tickDelta;
    }

    /**
     * Сглаживание углов весла.
     * @param index 0=левое 1=правое
     * @param targetPitch целевой pitch (рад)
     * @param targetYaw   целевой yaw (рад)
     */
    public void smoothPaddle(int index, float targetPitch, float targetYaw) {
        if (!paddlesInit) {
            smoothPitch[index] = targetPitch;
            smoothYaw[index] = targetYaw;
            return;
        }
        smoothPitch[index] += (targetPitch - smoothPitch[index]) * PADDLE_SMOOTH;
        smoothYaw[index]   += (targetYaw   - smoothYaw[index])   * PADDLE_SMOOTH;
    }

    public float getSmoothedPitch(int idx) { return smoothPitch[idx]; }
    public float getSmoothedYaw(int idx) { return smoothYaw[idx]; }

    public void commitPaddlesInit() {
        paddlesInit = true;
    }
}