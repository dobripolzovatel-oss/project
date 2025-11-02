package seq.sequencermod.size.auto;

/** Производные настройки камеры/рендера/поведения под текущий размер игрока. */
public class SizeDerived {
    // Render: 1st person projection
    public float near;          // безопасный near
    public float far;           // far clip
    public float fovMultiplier; // множитель FOV (1 = без изменений)

    // Bobbing (качание камеры)
    public boolean bobbingEnabled;
    public float bobMaxHeightFraction;

    // 3rd person distance
    public double thirdPersonMinDistance; // минимальная дистанция (относит. от eye, с учётом абс. минимума)
    public double thirdPersonAbsMin;      // абсолютный минимум
    public double cameraExponent;         // влияние роста на clipToSpace

    // 3rd person clearance над полом
    public float thirdPersonClearanceFrac; // доля от высоты
    public float thirdPersonClearanceMin;  // метры
    public float thirdPersonClearanceMax;  // метры

    // 1st person clearance (если глаза внутри формы)
    public float firstPersonClearanceEps; // небольшой зазор (+epsilon)

    // Анти-мигание модели
    public boolean visualUpscaleEnabled;
    public float minVisualModelScale;
    public float maxVisualUpscaleFactor;

    // Поведение
    public boolean disableAutoSwim;

    // Служебное
    public float eyeHeight;    // вычисленная высота глаз
    public float hitboxHeight; // высота хитбокса
    public float scale;        // “реальный” масштаб (≈ height/1.8)
}