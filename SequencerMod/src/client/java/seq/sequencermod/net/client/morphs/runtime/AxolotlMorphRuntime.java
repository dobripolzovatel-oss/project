package seq.sequencermod.net.client.morphs.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Клиентское runtime состояние аксолотля для каждого игрока в морфе.
 * Хранит:
 *  - ticksPlayingDead (обратный счёт)
 *  - variant (0..4) LUCY/WILD/GOLD/CYAN/BLUE
 *  - air (0..6000)
 *
 * Сервер (при наличии) должен синхронизировать S2C (play dead + variant + air опционально).
 */
public final class AxolotlMorphRuntime {

    private static final Map<UUID, AxolotlMorphRuntime> DATA = new HashMap<>();

    public static AxolotlMorphRuntime get(UUID id, boolean create) {
        AxolotlMorphRuntime rt = DATA.get(id);
        if (rt == null && create) {
            rt = new AxolotlMorphRuntime();
            DATA.put(id, rt);
        }
        return rt;
    }

    public static void remove(UUID id) {
        DATA.remove(id);
    }

    // --- fields ---
    private int playDeadTicks = 0;
    private int variant = 0;
    private int air = 6000; // как ванильный максимум

    private AxolotlMorphRuntime() {}

    // === Play Dead ===
    public boolean isPlayingDead() {
        return playDeadTicks > 0;
    }
    public int getPlayDeadTicks() { return playDeadTicks; }
    public void startPlayDeadLocal(int ticks) {
        this.playDeadTicks = ticks;
    }
    public void stopPlayDeadLocal() { this.playDeadTicks = 0; }
    public void tickPlayDead() {
        if (playDeadTicks > 0) playDeadTicks--;
    }

    // === Variant ===
    public int getVariant() { return variant; }
    public void setVariant(int v) {
        if (v < 0) v = 0;
        if (v > 4) v = 4;
        this.variant = v;
    }

    // === Air ===
    public int getAir() { return air; }
    public void setAir(int a) {
        if (a < -20) a = -20;
        if (a > 6000) a = 6000;
        this.air = a;
    }
    public void tickAir(boolean wet) {
        if (wet) {
            this.air = 6000;
        } else {
            this.air--;
            if (this.air == -20) {
                // В ваниле аксолотль получает урон и air = 0; здесь можно оформить визуал/эффект
                this.air = 0;
            }
        }
    }
}