package seq.sequencermod.size.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import seq.sequencermod.size.PlayerSizeData;
import seq.sequencermod.size.PlayerSizeServerStore;
import seq.sequencermod.size.net.SizePackets;

public final class SizeCommand {
    private SizeCommand(){}

    private static final float MIN_W = 0.000001f;
    private static final float MIN_H = 0.000001f;

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, ra, env) -> {
            dispatcher.register(
                    CommandManager.literal("setsize")
                            .then(CommandManager.argument("width", FloatArgumentType.floatArg(MIN_W))
                                    .then(CommandManager.argument("height", FloatArgumentType.floatArg(MIN_H))
                                            .executes(SizeCommand::apply)))
            );
            dispatcher.register(CommandManager.literal("clearsize").executes(SizeCommand::clear));
            dispatcher.register(CommandManager.literal("getsize").executes(SizeCommand::get));
        });
    }

    private static int apply(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity p = ctx.getSource().getPlayer(); if (p == null) return 0;
        float w = Math.max(MIN_W, FloatArgumentType.getFloat(ctx, "width"));
        float h = Math.max(MIN_H, FloatArgumentType.getFloat(ctx, "height"));
        PlayerSizeData data = new PlayerSizeData(w, h, null, null, false, false);
        PlayerSizeServerStore.set(p.getUuid(), data);
        p.calculateDimensions();
        SizePackets.broadcastAll(p, data);
        ctx.getSource().sendFeedback(() -> Text.literal("Hitbox set w=" + w + " h=" + h), true);
        return 1;
    }

    private static int clear(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity p = ctx.getSource().getPlayer(); if (p == null) return 0;
        PlayerSizeServerStore.clear(p.getUuid());
        p.calculateDimensions();
        SizePackets.broadcastAll(p, null);
        ctx.getSource().sendFeedback(() -> Text.literal("Hitbox cleared (vanilla)."), true);
        return 1;
    }

    private static int get(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity p = ctx.getSource().getPlayer(); if (p == null) return 0;
        PlayerSizeData d = PlayerSizeServerStore.get(p.getUuid());
        String msg = (d == null) ? "Size: <vanilla>" : ("Size w=" + d.width + " h=" + d.height);
        ctx.getSource().sendFeedback(() -> Text.literal(msg), false);
        return 1;
    }
}