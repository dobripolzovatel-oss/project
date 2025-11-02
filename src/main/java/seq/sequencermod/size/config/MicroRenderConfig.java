package seq.sequencermod.size.config;

/**
 * Конфигурация визуальных и рендер-настроек для микро‑масштабов.
 */
public final class MicroRenderConfig {
    private MicroRenderConfig() {}

    // Граница "tiny" для камер/рендера
    public static final float TINY_THRESHOLD = 0.05f;

    // ----- Масштабирование FOV -----
    public static final boolean SCALE_FOV = true;
    public static final double MAX_FOV_REDUCTION = 0.55;

    // ----- Безопасный near/far для 1-го лица -----
    public static final boolean ENABLE_CUSTOM_NEAR = true;
    // Базовые клампы near для обычных размеров
    public static final float NEAR_MIN_SAFE = 0.0010f;
    public static final float NEAR_MAX_SAFE = 0.0050f;
    public static final float EYE_NEAR_FRACTION = 0.35f;
    public static final float FAR_NEAR_MAX_RATIO = 2_000_000f;

    // Доп. клампы near для "почти‑микро" размеров (около 0.05..0.06)
    // Используются, чтобы near не задирался до 0.005 на росте ~0.0555.
    public static final float TINY_NEAR_THRESHOLD = 0.06f;     // h < 0.06 → tiny‑режим near
    public static final float NEAR_MIN_SAFE_TINY  = 0.0008f;
    public static final float NEAR_MAX_SAFE_TINY  = 0.0030f;

    // Дальняя плоскость
    public static final float FAR_CLIP = 4096.0f;
    public static final float FAR_CLIP_TINY = 1024.0f;   // для h < 0.06
    public static final float FAR_CLIP_MICRO = 512.0f;   // для h < 0.005

    // ----- Bobbing -----
    public static final float BOB_TINY_DISABLE_HEIGHT = 0.10f;
    public static final float BOB_MAX_HEIGHT_FRACTION = 0.02f;

    // ----- Анти-мигание модели игрока (визуальный апскейл) -----
    public static final boolean ANTIFLICKER_VISUAL_UPSCALE = true;
    public static final float MIN_VISUAL_MODEL_SCALE = 0.04f;
    public static final float MAX_UPSCALE_FACTOR    = 200f;

    // ----- Камера в 3‑ем лице -----
    public static final boolean ENABLE_THIRD_PERSON_DISTANCE_TWEAK = true;
    public static final double CAMERA_EXPONENT = 1.0;
    public static final double MIN_ABSOLUTE_THIRD_PERSON = 0.01;
    public static final float THIRD_PERSON_MIN_EYE_FRACTION = 0.60f;

    public static final float THIRD_PERSON_CLEARANCE_FRACTION = 0.10f;
    public static final float THIRD_PERSON_CLEARANCE_ABS_MIN  = 0.01f;
    public static final float THIRD_PERSON_CLEARANCE_ABS_MAX  = 0.15f;

    // ----- Поведение -----
    public static final float BEHAVIOR_TINY_HEIGHT = 0.30f;

    // (опционально) Клиентский порог отключения in‑wall overlay, если захочешь использовать его
    // публично — пока оставить как есть в миксине.
    // public static final float IN_WALL_OVERLAY_DISABLE_HEIGHT = 0.10f;
}