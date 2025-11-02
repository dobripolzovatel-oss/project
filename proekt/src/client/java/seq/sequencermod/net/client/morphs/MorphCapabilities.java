package seq.sequencermod.net.client.morphs;

public final class MorphCapabilities {
    public final boolean canFly;
    public final boolean canSwim;
    public final boolean hasWingFlap;
    public final float groundSpeed;   // базовая целевая скорость на земле (м/тик условно)
    public final float flySpeed;      // то же для полёта
    public final float swimSpeed;     // то же для воды
    public final float turnRateDeg;   // лимит изменения yaw/pitch за тик (deg)
    public final float gravityScale;  // 1.0 = как у игрока

    public MorphCapabilities(boolean canFly, boolean canSwim, boolean hasWingFlap,
                             float groundSpeed, float flySpeed, float swimSpeed,
                             float turnRateDeg, float gravityScale) {
        this.canFly = canFly;
        this.canSwim = canSwim;
        this.hasWingFlap = hasWingFlap;
        this.groundSpeed = groundSpeed;
        this.flySpeed = flySpeed;
        this.swimSpeed = swimSpeed;
        this.turnRateDeg = turnRateDeg;
        this.gravityScale = gravityScale;
    }

    // Пресеты под ванильных мобов (можно подправить по ощущениям)
    public static MorphCapabilities phantom() {
        // Фантом: быстрый полёт, умеренная поворотливость
        return new MorphCapabilities(
                true,  false, true,
                0.10f, 0.35f, 0.12f,
                18.0f, 1.0f
        );
        // turnRateDeg = ~FlightMoveControl, flySpeed близок к ENTITY_FLYING_SPEED
    }

    public static MorphCapabilities blaze() {
        // Блейз: зависание/плавный полёт, менее резкий чем фантом
        return new MorphCapabilities(
                true,  false, false,
                0.10f, 0.28f, 0.12f,
                12.0f, 1.0f
        );
    }

    public static MorphCapabilities axolotl() {
        // Аксолотль: лучше в воде, на суше медленнее
        return new MorphCapabilities(
                false, true,  false,
                0.08f, 0.00f, 0.22f,
                10.0f, 1.0f
        );
    }

    public static MorphCapabilities camel() {
        // Верблюд: только земля, инерция побольше
        return new MorphCapabilities(
                false, false, false,
                0.1f,  0.0f,  0.0f,
                8.0f,  1.0f
        );
    }

    public static MorphCapabilities boat() {
        // Лодка: оставим как есть (управление отдельное), тут плейсхолдер
        return new MorphCapabilities(
                false, true,  false,
                0.0f,  0.0f,  0.18f,
                6.0f,  1.0f
        );
    }

    public static MorphCapabilities allay() {
        // Аллей: мягкий полёт, малый размер, лёгкий
        return new MorphCapabilities(
                true,  false, true,
                0.08f, 0.25f, 0.12f,
                14.0f, 1.0f
        );
    }
}