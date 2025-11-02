package seq.sequencermod;

import net.fabricmc.api.ModInitializer;

public final class SequencerModInit implements ModInitializer {
    @Override
    public void onInitialize() {
        System.out.println("[Sequencer|Main] onInitialize");
        // MorphServer.bootstrap(); // УЖЕ вызывается из SequencerMod, чтобы не дублировать регистрацию
    }
}