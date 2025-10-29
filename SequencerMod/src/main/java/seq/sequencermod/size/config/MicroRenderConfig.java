package seq.sequencermod.size.config;

/**
 * Configuration constants for micro-scale rendering adjustments.
 * <p>
 * <b>Rationale:</b> When players are scaled to tiny/micro sizes (height &lt; 0.1m),
 * vanilla projection near/far planes can cause visual artifacts. However, modifying
 * the projection matrix for the main world rendering pass breaks frustum culling,
 * causing most of the world to disappear (only sky visible, or massive voids).
 * <p>
 * <b>Solution:</b> By default, this mod behaves exactly like vanilla for world rendering.
 * Optional tweaks are available for advanced users:
 * <ul>
 *   <li>{@link #APPLY_CUSTOM_NEAR_IN_WORLD} - allows projection changes in world pass (DANGEROUS, off by default)</li>
 *   <li>{@link #APPLY_CUSTOM_NEAR_IN_HAND} - allows safe near-plane adjustments only for hand rendering (off by default)</li>
 *   <li>{@link #DEBUG_DISABLE_FRUSTUM_CULLING} - diagnostic bypass of frustum culling (off by default)</li>
 * </ul>
 * <p>
 * With all flags at their defaults (false), the game renders identically to vanilla.
 */
public final class MicroRenderConfig {
    private MicroRenderConfig() {}

    // ====== Диагностика/аварийные флаги ======
    /**
     * DEBUG ONLY: Completely disable frustum culling on the client.
     * <p>
     * When enabled, all chunks/entities are rendered regardless of camera frustum.
     * This helps verify that disappearing terrain is caused by incorrect culling
     * rather than other issues. Should NEVER be enabled in production/normal gameplay.
     * <p>
     * <b>Default: false</b> (vanilla behavior)
     */
    public static final boolean DEBUG_DISABLE_FRUSTUM_CULLING = false;

    /**
     * Enable custom near/far plane projection modifications for the main world rendering pass.
     * <p>
     * <b>WARNING:</b> Changing the projection during world rendering breaks frustum culling
     * assumptions, causing most chunks to be incorrectly rejected. This results in
     * "empty sky" or massive terrain voids. Only enable this if you fully understand
     * the implications and have modified the frustum calculation accordingly.
     * <p>
     * <b>Default: false</b> (vanilla behavior - no projection changes)
     */
    public static final boolean APPLY_CUSTOM_NEAR_IN_WORLD = false;

    /**
     * Enable custom near-plane adjustments for the hand rendering pass only (first-person view).
     * <p>
     * When enabled, the projection is safely saved/restored around the hand rendering pass,
     * allowing closer near planes for tiny players without affecting world rendering or culling.
     * The near plane is clamped to safe values and the far/near ratio is bounded.
     * <p>
     * <b>Default: false</b> (vanilla behavior - no hand pass modifications)
     */
    public static final boolean APPLY_CUSTOM_NEAR_IN_HAND  = false;

    // ====== Порог микромасштаба ======
    public static final float MICRO_HEIGHT_THRESHOLD = 0.10f; // 10 см

    // ====== near для tiny/micro ======
    public static final float NEAR_MIN_ULTRAMICRO = 0.003f; // 3 мм
    public static final float NEAR_MAX_ULTRAMICRO = 0.020f; // 2 см

    public static final float NEAR_MIN_TINY = 0.010f; // 1 см
    public static final float NEAR_MAX_TINY = 0.040f; // 4 см

    // Алиасы для SizeRules
    public static final float NEAR_MIN_SAFE_TINY = NEAR_MIN_TINY;
    public static final float NEAR_MAX_SAFE_TINY = NEAR_MAX_TINY;

    // “Безопасные” рамки near для обычного роста
    public static final float NEAR_MIN_SAFE = 0.050f;
    public static final float NEAR_MAX_SAFE = 0.200f;

    // Для пасса руки (оставим на будущее, сейчас выключено APPLY_CUSTOM_NEAR_IN_HAND)
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