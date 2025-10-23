package seq.sequencermod.net.client.morphs;

import net.minecraft.client.network.AbstractClientPlayerEntity;

import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Runtime-флаги активных морфов. Сейчас только Allay (render-only).
 * При добавлении других морфов расширяй isMorphActive().
 */
public final class MorphRuntimeFlags {

    private MorphRuntimeFlags() {}

    // Render-only Allay morph flag
    private static final WeakHashMap<UUID, Boolean> ALLAY = new WeakHashMap<>();

    public static void setAllayMorph(AbstractClientPlayerEntity p, boolean v) {
        ALLAY.put(p.getUuid(), v);
    }

    public static boolean isAllayMorph(AbstractClientPlayerEntity p) {
        return ALLAY.getOrDefault(p.getUuid(), false);
    }

    /**
     * Общий "морф активен?" — раньше был эквивалент isAllayMorph.
     * Теперь считаем активным ЛЮБОЙ морф, известный MorphClientSync.
     */
    public static boolean isMorphActive(AbstractClientPlayerEntity p) {
        try {
            // Прямая проверка текущего морфа
            return seq.sequencermod.net.client.MorphClientSync.getMorphType(p.getUuid()) != null;
        } catch (Throwable ignored) {
            // На всякий случай оставим поддержку старого флага
            return isAllayMorph(p);
        }
    }
}