package seq.sequencermod.commander.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public final class ForIncCommand {
    private ForIncCommand() {}

    public static void register() {
        // Типы dispatcher и registryAccess выведутся из интерфейса:
        // (CommandDispatcher<FabricClientCommandSource>, DynamicRegistryManager)
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommandManager.literal("forinc")
                            .executes(ctx -> {
                                ctx.getSource().sendFeedback(Text.literal("[ForInc] Клиентская команда выполнена"));
                                return 1;
                            })
            );
        });
    }
}