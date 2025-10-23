package seq.sequencermod.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Глобальный флаг "сейчас рендерится рука/предмет в первом лице".
 * Используем ThreadLocal, чтобы безопасно проверять внутри проекции/FOV.
 */
@Environment(EnvType.CLIENT)
public final class RenderPassFlags {
    private static final ThreadLocal<Boolean> IN_HAND = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public static void enterHand() { IN_HAND.set(Boolean.TRUE); }
    public static void exitHand()  { IN_HAND.set(Boolean.FALSE); }
    public static boolean isHand() { return Boolean.TRUE.equals(IN_HAND.get()); }

    private RenderPassFlags() {}
}