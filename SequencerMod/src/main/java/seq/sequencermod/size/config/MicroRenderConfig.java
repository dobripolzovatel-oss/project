package seq.sequencermod.size.config;

public final class MicroRenderConfig {
    private MicroRenderConfig() {}

    // ====== Диагностика/аварийные флаги ======
    public static final boolean DEBUG_DISABLE_FRUSTUM_CULLING = true;

    // Жёстко форсировать ванильную проекцию в world‑pass (диагностический «замок»)
    public static final boolean FORCE_VANILLA_WORLD_PROJECTION = true;

    // Единый «отключатель» наших сдвигов камеры (first/third) для чистой диагностики
    public static final boolean DEBUG_VANILLA_CAMERA = true;

    // Опционально: аварийное отключение окклюзии (по умолчанию выкл)
    public static final boolean DEBUG_DISABLE_OCCLUSION = false;

    // Вообще не трогаем near/far в мире
    public static final boolean APPLY_CUSTOM_NEAR_IN_WORLD = false;

    // Малый near только в рендере руки (для диагностики оставляем выкл)
    public static final boolean APPLY_CUSTOM_NEAR_IN_HAND = false;

    // ====== Клиппинги/пределы ======
    public static final float FAR_CLIP        = 4096f;
    public static final float FAR_CLIP_TINY   = 2048f;
    public static final float FAR_CLIP_MICRO  = 1024f;
    public static final float FAR_CLIP_HARD   = 16_384f;

    public static final float FAR_NEAR_MAX_RATIO = 200_000f;
    public static final float TINY_NEAR_THRESHOLD = 0.060f; // 6 см
    public static final float EYE_NEAR_FRACTION   = 0.02f;

    // Пределы near для tiny/ultra‑tiny (используются там, где это допускается)
    public static final float NEAR_MIN_TINY  = 0.001f;
    public static final float NEAR_MAX_TINY  = 0.050f;
    public static final float HAND_NEAR_MIN  = 0.0002f;
    public static final float HAND_NEAR_MAX  = 0.0200f;

    // ДЛЯ СОВМЕСТИМОСТИ: SAFE‑синонимы, которых не хватало в SizeRules
    public static final float NEAR_MIN_SAFE_TINY = NEAR_MIN_TINY;
    public static final float NEAR_MAX_SAFE_TINY = NEAR_MAX_TINY;

    // Безопасный коридор near для «обычных» размеров (не tiny)
    // Используется только в расчётах производных параметров; сам world‑pass у нас ванильный.
    public static final float NEAR_MIN_SAFE = 0.02f;
    public static final float NEAR_MAX_SAFE = 0.20f;

    // ====== FOV‑скейл ======
    public static final boolean SCALE_FOV = false; // на время диагностики — выкл
    public static final float   TINY_THRESHOLD = 0.50f;
    public static final double  MAX_FOV_REDUCTION = 0.25;

    // ====== Боббинг ======
    public static final float BOB_TINY_DISABLE_HEIGHT = 0.50f;
    public static final float BOB_MAX_HEIGHT_FRACTION = 0.04f;

    // ====== Камера (3‑е лицо) ======
    public static final float CAMERA_EXPONENT = 1.25f;
    public static final float MIN_ABSOLUTE_THIRD_PERSON = 0.30f;
    public static final float THIRD_PERSON_MIN_EYE_FRACTION = 1.25f;
    public static final float THIRD_PERSON_CLEARANCE_FRACTION = 0.30f;
    public static final float THIRD_PERSON_CLEARANCE_ABS_MIN  = 0.05f;
    public static final float THIRD_PERSON_CLEARANCE_ABS_MAX  = 2.00f;

    // ====== Анти‑мерцание модели ======
    public static final boolean ANTIFLICKER_VISUAL_UPSCALE = true;
    public static final float  MIN_VISUAL_MODEL_SCALE = 0.10f;
    public static final float  MAX_UPSCALE_FACTOR     = 8.0f;

    // ====== Поведение ======
    public static final float BEHAVIOR_TINY_HEIGHT = 0.40f;

    // Порог «микро» для спец‑логики
    public static final float MICRO_HEIGHT_THRESHOLD = 0.06f;
}