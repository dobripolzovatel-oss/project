package seq.sequencermod.sequencer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.stream.Collectors;

public final class SeqCommands {
    private SeqCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("seq")
                .then(CommandManager.literal("start")
                        .then(CommandManager.argument("id", StringArgumentType.word())
                                .suggests((ctx, sb) -> {
                                    var list = SequenceRegistry.listIds().stream().toList();
                                    list.forEach(sb::suggest);
                                    return sb.buildFuture();
                                })
                                .executes(ctx -> {
                                    var player = ctx.getSource().getPlayer();
                                    if (player == null) return 0;
                                    String id = StringArgumentType.getString(ctx, "id");
                                    boolean ok = SequenceRegistry.start(player.getUuid(), id);
                                    if (!ok) {
                                        ctx.getSource().sendError(Text.literal("Unknown sequence: " + id));
                                        return 0;
                                    }
                                    ctx.getSource().sendFeedback(() -> Text.literal("Sequence started: " + id), true);
                                    return 1;
                                })
                        )
                )
                .then(CommandManager.literal("stop")
                        .executes(ctx -> {
                            var player = ctx.getSource().getPlayer();
                            if (player == null) return 0;
                            SequenceRegistry.stop(player.getUuid());
                            ctx.getSource().sendFeedback(() -> Text.literal("Sequence stopped"), true);
                            return 1;
                        })
                )
                .then(CommandManager.literal("list")
                        .executes(ctx -> {
                            var ids = SequenceRegistry.listIds();
                            String s = ids.isEmpty() ? "(none)" : ids.stream().collect(Collectors.joining(", "));
                            ctx.getSource().sendFeedback(() -> Text.literal("Sequences: " + s), false);
                            return 1;
                        })
                )
                .then(CommandManager.literal("reload")
                        .executes(ctx -> {
                            // Секвенции из датапаков обновляются стандартной командой /reload
                            ctx.getSource().sendFeedback(
                                    () -> Text.literal("Use /reload to reload datapacks (sequences live in data/sequencermod/sequences)"),
                                    false
                            );
                            return 1;
                        })
                )
        );
    }
}