package seq.sequencermod.sequencer;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class SequenceRunnerManager {
    private SequenceRunnerManager() {}

    public static void playFor(ServerPlayerEntity player, String sequenceId) {
        if (player == null || sequenceId == null || sequenceId.isBlank()) return;
        SequenceRegistry.start(player.getUuid(), sequenceId);
    }

    public static void stopFor(ServerPlayerEntity player) {
        if (player == null) return;
        SequenceRegistry.stop(player.getUuid());
    }

    public static void tick(MinecraftServer server) {
        // no-op (исполнение через SequenceScheduler)
    }
}