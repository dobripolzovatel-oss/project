package seq.sequencermod.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import seq.sequencermod.morph.server.MorphServerState;

public final class MorphDebugCommands {
    private MorphDebugCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("morphid")
                    .requires(src -> src.hasPermissionLevel(2))
                    .then(CommandManager.argument("id", StringArgumentType.string())
                            .executes(ctx -> {
                                ServerPlayerEntity p = ctx.getSource().getPlayer();
                                String raw = StringArgumentType.getString(ctx, "id");
                                Identifier id = Identifier.tryParse(raw);
                                if (p == null || id == null) {
                                    ctx.getSource().sendError(net.minecraft.text.Text.literal("Bad id or no player"));
                                    return 0;
                                }
                                MorphServerState.set(p, id);
                                p.calculateDimensions();
                                ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal("Applied morph: " + id), true);
                                return 1;
                            })));
        });
    }
}