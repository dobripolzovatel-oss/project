package seq.sequencermod.client.ui;

import net.minecraft.util.Identifier;
import seq.sequencermod.disguise.render.DisguiseRenderable;

/**
 * Заглушка реестра disguise-рендереров. Возвращает null — превью рисует плейсхолдер.
 */
public final class DisguiseClientRegistry {
    private DisguiseClientRegistry() {}

    public static DisguiseRenderable resolve(Identifier id) {
        return null;
    }
}