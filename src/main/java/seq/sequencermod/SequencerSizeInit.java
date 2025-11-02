package seq.sequencermod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import seq.sequencermod.size.command.SizeCommand;
import seq.sequencermod.size.net.SizePackets;

public class SequencerSizeInit implements ModInitializer {
    @Override
    public void onInitialize() {
        SizeCommand.register();
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            SizePackets.sendAllTo(handler.getPlayer());
        });
    }
}