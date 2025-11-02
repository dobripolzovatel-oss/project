package seq.sequencermod.sequencer;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.registry.Registries;
import seq.sequencermod.core.ServerRef;
import seq.sequencermod.morph.runtime.MorphRuntime;
import seq.sequencermod.sequencer.json.SequenceJsonDef;
import seq.sequencermod.server.morph.MorphServer;

import java.util.UUID;

public final class SequenceEngine {

    private SequenceEngine() {}

    public static boolean run(UUID playerUuid, String seqId, SequenceJsonDef def) {
        MinecraftServer server = ServerRef.get();
        if (server == null) return false;
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
        if (player == null) return false;

        int delaySum = 0;
        for (SequenceJsonDef.Step step : def.steps) {
            final String t = step.type == null ? "" : step.type.toLowerCase();
            final int scheduledAt = Math.max(0, delaySum);

            if ("delay".equals(t) || "wait".equals(t)) {
                delaySum += Math.max(0, step.ticks);
                continue;
            }

            SequenceScheduler.schedule(playerUuid, scheduledAt, s -> executeStep(seqId, step, playerUuid));
        }
        System.out.println("[SequenceEngine] Scheduled sequence '" + seqId + "' for " + player.getName().getString());
        return true;
    }

    public static void stop(UUID playerUuid) {
        SequenceScheduler.cancelFor(playerUuid);
        MorphRuntime.clearMorph(playerUuid, u -> true);
        MinecraftServer server = ServerRef.get();
        if (server != null) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(playerUuid);
            if (p != null) {
                MorphServer.syncToAll(p, null);
                System.out.println("[SequenceEngine] Unmorphed on stop for " + p.getName().getString());
            }
        }
    }

    private static void executeStep(String seqId, SequenceJsonDef.Step step, UUID playerUuid) {
        MinecraftServer server = ServerRef.get();
        if (server == null) return;
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
        if (player == null) return;

        String t = step.type == null ? "" : step.type.toLowerCase();

        switch (t) {
            case "morph": {
                if (step.id == null || step.id.isBlank()) return;
                var err = MorphRuntime.applyMorph(playerUuid, step.id, u -> true);
                if (err == null) {
                    Identifier id = Identifier.tryParse(step.id);
                    MorphServer.syncToAll(player, id);
                    System.out.println("[SequenceEngine] [" + seqId + "] morph -> " + step.id + " for " + player.getName().getString());
                } else {
                    player.sendMessage(net.minecraft.text.Text.literal("[seq:" + seqId + "] morph failed: " + err), false);
                    System.out.println("[SequenceEngine] [" + seqId + "] morph FAILED: " + err);
                }
                break;
            }
            case "unmorph":
            case "clear_morph": {
                MorphRuntime.clearMorph(playerUuid, u -> true);
                MorphServer.syncToAll(player, null);
                System.out.println("[SequenceEngine] [" + seqId + "] unmorph for " + player.getName().getString());
                break;
            }
            case "message": {
                if (step.text != null && !step.text.isBlank()) {
                    player.sendMessage(net.minecraft.text.Text.literal(step.text), false);
                    System.out.println("[SequenceEngine] [" + seqId + "] message: " + step.text);
                }
                break;
            }
            case "play_sound": {
                if (step.id == null || step.id.isBlank()) return;
                Identifier id = Identifier.tryParse(step.id);
                if (id == null) return;
                SoundEvent se = Registries.SOUND_EVENT.get(id);
                if (se == null) return;
                player.playSound(se, step.volume, step.pitch);
                System.out.println("[SequenceEngine] [" + seqId + "] play_sound -> " + step.id);
                break;
            }
            case "particle": {
                if (!(player.getWorld() instanceof ServerWorld sw)) return;
                if (step.id == null || step.id.isBlank()) return;
                Identifier id = Identifier.tryParse(step.id);
                if (id == null) return;
                ParticleType<?> pt = Registries.PARTICLE_TYPE.get(id);
                if (!(pt instanceof DefaultParticleType dpt)) {
                    player.sendMessage(net.minecraft.text.Text.literal("[seq:" + seqId + "] unsupported particle: " + step.id), false);
                    System.out.println("[SequenceEngine] [" + seqId + "] unsupported particle: " + step.id);
                    return;
                }
                Vec3d pos = player.getPos();
                double ox = step.offset != null ? step.offset.x : 0.0;
                double oy = step.offset != null ? step.offset.y : 1.0;
                double oz = step.offset != null ? step.offset.z : 0.0;
                sw.spawnParticles(dpt, pos.x, pos.y + oy, pos.z, Math.max(1, step.count), ox, oy, oz, step.speed);
                System.out.println("[SequenceEngine] [" + seqId + "] particle -> " + step.id);
                break;
            }
            case "run_command": {
                if (step.command == null || step.command.isBlank()) return;
                boolean asPlayer = "player".equalsIgnoreCase(step.as);
                if (asPlayer) {
                    server.getCommandManager().executeWithPrefix(player.getCommandSource(), step.command);
                } else {
                    server.getCommandManager().executeWithPrefix(server.getCommandSource().withLevel(4), step.command);
                }
                System.out.println("[SequenceEngine] [" + seqId + "] run_command: " + step.command + " as=" + ("player".equalsIgnoreCase(step.as) ? "player" : "server"));
                break;
            }
            default:
                System.out.println("[SequenceEngine] [" + seqId + "] unknown step: " + step.type);
                break;
        }
    }
}