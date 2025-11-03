package seq.sequencermod.client.ui;

import net.minecraft.util.Identifier;
import seq.sequencermod.net.client.MorphClientSync;

/**
 * Вызовите из вашего UI при подтверждении выбора морфа.
 */
public final class MorphApply {
    public static void applyFromUi(Identifier id) {
        MorphClientSync.requestMorph(id);
    }
    private MorphApply() {}
}