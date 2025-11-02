package seq.sequencermod.morph.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import seq.sequencermod.morph.runtime.MorphStateStore;

public final class MorphDebugCommand {
    private MorphDebugCommand() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(CommandManager.literal("morph_debug")
                .executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    if (p == null) return 0;
                    String morph = MorphStateStore.get(p.getUuid());
                    var pose = p.getPose();
                    var dims = p.getDimensions(pose); // это вызовет наш серверный миксин
                    float eye = p.getStandingEyeHeight(); // учитывает серверный eyeHeight миксин
                    ctx.getSource().sendFeedback(() -> Text.literal(
                            "[morph_debug] morph=" + morph +
                                    " pose=" + pose +
                                    " dims=" + (dims != null ? (dims.width + "x" + dims.height) : "null") +
                                    " eye=" + eye
                    ), false);
                    return 1;
                })
        );
    }
}