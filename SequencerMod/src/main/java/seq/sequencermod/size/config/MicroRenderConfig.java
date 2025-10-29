package seq.sequencermod.size.config;

public final class MicroRenderConfig {
    private MicroRenderConfig() {}

    // ====== ГЛОБАЛЬНЫЕ ПЕРЕКЛЮЧАТЕЛИ ======
    public static final boolean APPLY_CUSTOM_NEAR_IN_WORLD = false; // мир — ваниль
    public static final boolean APPLY_CUSTOM_NEAR_IN_HAND  = true;  // малый near только в руке

    // ====== Пороги микромасштаба ======
    public static final float MICRO_HEIGHT_THRESHOLD = 0.10f; // 10 см

    // ====== near для tiny/micro ======
    public static final float NEAR_MIN_ULTRAMICRO = 0.003f; // 3 мм
    public static final float NEAR_MAX_ULTRAMICRO = 0.020f; // 2 см

    public static final float NEAR_MIN_TINY = 0.010f; // 1 см
    public static final float NEAR_MAX_TINY = 0.040f; // 4 см

    // Алиасы для SizeRules (исправляет "Cannot resolve symbol ... SAFE_TINY")
    public static final float NEAR_MIN_SAFE_TINY = NEAR_MIN_TINY;
    public static final float NEAR_MAX_SAFE_TINY = NEAR_MAX_TINY;

    // “Безопасные” рамки near для обычного роста
    public static final float NEAR_MIN_SAFE = 0.050f;
    public static final float NEAR_MAX_SAFE = 0.200f;

    // Для пасса руки (можно чуть агрессивнее)
    public static final float HAND_NEAR_MIN = 0.0025f;
    public static final float HAND_NEAR_MAX = 0.020f;

    // ====== FAR/отношения ======
    public static final float FAR_CLIP_MICRO = 512f;
    public static final float FAR_CLIP_TINY  = 1024f;
    public static final float FAR_CLIP       = 4096f;
    public static final float FAR_CLIP_HARD  = 16_384f;

    public static final float FAR_NEAR_MAX_RATIO = 200_000f;
    public static final float TINY_NEAR_THRESHOLD = 0.060f; // 6 см
    public static final float EYE_NEAR_FRACTION   = 0.02f;

    // ====== FOV-скейл ======
    public static final boolean SCALE_FOV = true;
    public static final float   TINY_THRESHOLD = 0.50f;
    public static final double  MAX_FOV_REDUCTION = 0.25;

    // ====== Боббинг ======
    public static final float BOB_TINY_DISABLE_HEIGHT = 0.50f;
    public static final float BOB_MAX_HEIGHT_FRACTION = 0.04f;

    // ====== Камера (3-е лицо) ======
    public static final float CAMERA_EXPONENT = 1.25f;
    public static final float MIN_ABSOLUTE_THIRD_PERSON = 0.30f;
    public static final float THIRD_PERSON_MIN_EYE_FRACTION = 1.25f;
    public static final float THIRD_PERSON_CLEARANCE_FRACTION = 0.30f;
    public static final float THIRD_PERSON_CLEARANCE_ABS_MIN  = 0.05f;
    public static final float THIRD_PERSON_CLEARANCE_ABS_MAX  = 2.00f;

    // ====== Анти-мерцание модели ======
    public static final boolean ANTIFLICKER_VISUAL_UPSCALE = true;
    public static final float  MIN_VISUAL_MODEL_SCALE = 0.10f;
    public static final float  MAX_UPSCALE_FACTOR     = 8.0f;

    // ====== Поведение ======
    public static final float BEHAVIOR_TINY_HEIGHT = 0.40f;
}