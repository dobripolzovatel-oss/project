package seq.sequencermod.core;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import seq.sequencermod.debug.MorphDebugCommands;
import seq.sequencermod.morph.command.MorphDebugCommand;
import seq.sequencermod.morph.command.MorphCommands;
//import seq.sequencermod.morph.command.PlayerSizeCommands; // <-- добавлено
import seq.sequencermod.net.SequencerNetworking;
import seq.sequencermod.sequencer.SequenceJsonLoader;
import seq.sequencermod.sequencer.SequenceRegistry;
import seq.sequencermod.sequencer.SequenceRunnerManager;

import java.util.stream.Collectors;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class SequencerMod implements ModInitializer {
	public static final Logger LOG = LoggerFactory.getLogger("SequencerMod");
	public static final String MOD_ID = "sequencermod";

	@Override
	public void onInitialize() {
		System.out.println("[SequencerMod] SequencerMod.onInitialize()");

		// Отладочные и морф-команды
		MorphDebugCommands.register();
		MorphDebugCommand.register();
		MorphCommands.register();
		//PlayerSizeCommands.register(); // <-- регистрируем /setsize и /clearsize

		LOG.info("SequencerMod init");

		// 1) Секвенции
		SequenceRegistry.init();
		SequenceJsonLoader.init();

		// 2) Сеть
		SequencerNetworking.init();

		// 3) Тики
		ServerTickEvents.END_SERVER_TICK.register(SequenceRunnerManager::tick);

		// 4) Команды /seq
		CommandRegistrationCallback.EVENT.register(this::registerCommands);

		// 5) GUI data клиенту при входе
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			SequencerNetworking.sendSequencesToClient(handler.player);
		});

		// 6) Очистка при выходе
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			SequenceRegistry.onPlayerLeft(handler.player);
		});
	}

	private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher,
								  CommandRegistryAccess registryAccess,
								  CommandManager.RegistrationEnvironment env) {
		dispatcher.register(literal("seq")
				.then(literal("list")
						.executes(ctx -> {
							var ids = SequenceRegistry.listIds();
							ctx.getSource().sendFeedback(() ->
									Text.literal("Sequences (" + ids.size() + "): " + ids.stream().collect(Collectors.joining(", "))), false);
							return 1;
						}))
				.then(literal("play")
						.then(argument("id", StringArgumentType.string())
								.suggests((c, b) -> {
									SequenceRegistry.listIds().forEach(b::suggest);
									return b.buildFuture();
								})
								.executes(ctx -> {
									String id = StringArgumentType.getString(ctx, "id");
									ServerPlayerEntity player = ctx.getSource().getPlayer();
									if (player == null) {
										ctx.getSource().sendError(Text.literal("Player only command."));
										return 0;
									}
									if (!SequenceRegistry.isKnown(id)) {
										ctx.getSource().sendError(Text.literal("Unknown sequence: " + id));
										return 0;
									}
									SequenceRunnerManager.playFor(player, id);
									ctx.getSource().sendFeedback(() -> Text.literal("Playing sequence: " + id), false);
									return 1;
								})
						)
				)
				.then(literal("stop")
						.executes(ctx -> {
							ServerPlayerEntity player = ctx.getSource().getPlayer();
							if (player == null) {
								ctx.getSource().sendError(Text.literal("Player only command."));
								return 0;
							}
							SequenceRunnerManager.stopFor(player);
							ctx.getSource().sendFeedback(() -> Text.literal("Stopped sequence"), false);
							return 1;
						})
				)
		);
	}
}