package seq.sequencermod.size.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

/**
 * Клиентские помощники для доступа к масштабу "белого" хитбокса зрителя.
 */
@Environment(EnvType.CLIENT)
public final class WhiteHitboxScaleClient {

    private WhiteHitboxScaleClient() {}

    /**
     * Текущая камера (viewer) — сущность, от лица которой рендерится мир.
     */
    public static Entity getViewer() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return null;
        // В 1.20.1 камера берётся через getCameraEntity(), если null — можно вернуть игрока
        Entity cam = mc.getCameraEntity();
        if (cam != null) return cam;
        return mc.player;
    }

    /**
     * Масштаб "белого" хитбокса для текущего viewer.
     * Возвращает 1.0f, если viewer недоступен.
     */
    public static float viewerWhiteScaleOrOne() {
        Entity viewer = getViewer();
        return viewer != null ? WhiteHitboxScale.whiteScale(viewer) : 1.0f;
    }

    /**
     * Высота "белого" хитбокса для текущего viewer (если нужно именно h, а не s).
     * Возвращает базовый EPS, если viewer недоступен.
     */
    public static float viewerWhiteHeightOrEps() {
        Entity viewer = getViewer();
        return viewer != null ? WhiteHitboxScale.whiteHeight(viewer) : WhiteHitboxScale.EPS_HEIGHT;
    }
}