package seq.sequencermod.morph;

import net.fabricmc.api.ModInitializer;
import seq.sequencermod.core.ServerRef;
import seq.sequencermod.sequencer.SequenceJsonLoader;

/**
 * Упрощённый init: без старой морф-сети/рантайма/команд, чтобы не конфликтовать с новым стеком.
 */
public final class MorphInit implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerRef.init();
        // Только загрузчик секвенций (если нужен здесь)
        SequenceJsonLoader.init();
    }
}