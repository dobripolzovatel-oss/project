package seq.sequencermod.core.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import seq.sequencermod.debug.ReachSigDump;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import seq.sequencermod.debug.OverlaySigDump;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import seq.sequencermod.client.gui.SequencerScreen;
import seq.sequencermod.client.ui.SequencerMainScreen;
import seq.sequencermod.net.SequencerNetworkingClient;
import seq.sequencermod.net.client.MorphClientSync;

// ДОБАВЛЕНО: реестры и поддержки морфов
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import seq.sequencermod.net.client.MorphClientSync_AxolotlHook;
import seq.sequencermod.net.client.SimpleMorphs;
import seq.sequencermod.net.client.morphs.render.axolotl.AxolotlMorphRenderer;
import seq.sequencermod.net.client.morphs.support.AxolotlMorphSupport;
import seq.sequencermod.net.client.morphs.support.CamelMorphSupport;
import seq.sequencermod.net.client.morphs.support.AllayMorphSupport;
import seq.sequencermod.net.client.PlayerSizeClientSync;

public final class SequencerModClient implements ClientModInitializer {
	// Г — открыть старый SequencerScreen
	private KeyBinding openSequencerKey;
	// H — открыть новый UI морфов/пресетов (единственный хоткей)
	private static KeyBinding OPEN_UI;
	// U — тоггл седла на активном морфе верблюда
	private static KeyBinding CAMEL_TOGGLE_SADDLE;

	@Override
	public void onInitializeClient() {
		System.out.println("[SequencerMod] SequencerModClient.onInitializeClient()");
		OverlaySigDump.dumpOnce();
		ReachSigDump.dumpOnce();

		// Хоткеи: единая регистрация здесь
		OPEN_UI = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.sequencermod.open_ui",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_H,
				"key.categories.sequencermod"
		));
		openSequencerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.sequencermod.open_sequencer",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_G,
				"key.categories.sequencermod"
		));
		CAMEL_TOGGLE_SADDLE = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.sequencermod.camel_toggle_saddle",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_U,
				"key.categories.sequencermod"
		));

		// Тики клиента: UI и верблюд, + поддержка аксолотля
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client == null) return;

			while (OPEN_UI.wasPressed()) {
				if (client.currentScreen == null) {
					client.setScreen(new SequencerMainScreen());
				}
			}

			while (openSequencerKey.wasPressed()) {
				if (client.player != null) {
					SequencerNetworkingClient.requestSequencesC2S();
					client.setScreen(new SequencerScreen(Text.literal("Sequencer")));
				}
			}

			while (CAMEL_TOGGLE_SADDLE.wasPressed()) {
				if (client.player == null) break;
				var st = SimpleMorphs.getCamelState(client.player);
				if (st == null) {
					client.player.sendMessage(Text.literal("Верблюд не активен (морф)"), false);
					break;
				}
				boolean next = !st.isSaddled();
				st.setSaddled(next);
				st.setShowReins(next);
				client.player.sendMessage(Text.literal("Верблюд: " + (next ? "седло включено" : "седло выключено")), false);
			}

			// Поддержка аксолотля (клики/состояния)
			AxolotlMorphSupport.clientTick();
		});

		// Проверка наличия клиентского миксин-конфига
		try (var is = SequencerModClient.class.getClassLoader().getResourceAsStream("sequencermod.client.mixins.json")) {
			System.out.println("[SequencerMod] mixin config present on classpath: " + (is != null));
		} catch (Exception ignore) {}

		// Клиентская сеть и морф-синхронизация (единые точки)
		SequencerNetworkingClient.initClient();
		MorphClientSync.bootstrap();
		seq.sequencermod.net.client.PlayerSizeClientSync.bootstrap();
		MorphClientSync_AxolotlHook.register();

		// Регистрации поддержек морфов (перенесены сюда, чтобы не дублировались)
		AxolotlMorphSupport.clientInitKeybinding();
		CamelMorphSupport.initKeybinds();
		AllayMorphSupport.init();

		// Модели
		EntityModelLayerRegistry.registerModelLayer(AxolotlMorphRenderer.LAYER, AxolotlMorphRenderer::createModelData);

		// Клиентские команды
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("sequi").executes(ctx -> {
				var mc = MinecraftClient.getInstance();
				if (mc.player == null) return 0;
				SequencerNetworkingClient.requestSequencesC2S();
				mc.setScreen(new SequencerScreen(Text.literal("Sequencer")));
				return 1;
			}));

			dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("morphc")
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("id", net.minecraft.command.argument.IdentifierArgumentType.identifier()).executes(ctx -> {
						var mc = MinecraftClient.getInstance();
						if (mc.player == null) return 0;
						Identifier id = ctx.getArgument("id", Identifier.class);
						seq.sequencermod.net.client.MorphClientSync.setLocalMorph(mc.player.getUuid(), id);
						mc.player.sendMessage(Text.literal("MorphC -> " + id), false);
						return 1;
					}))
			);

			dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("unmorphc").executes(ctx -> {
				var mc = MinecraftClient.getInstance();
				if (mc.player == null) return 0;
				seq.sequencermod.net.client.MorphClientSync.clearLocalMorph(mc.player.getUuid());
				mc.player.sendMessage(Text.literal("MorphC cleared"), false);
				return 1;
			}));
		});

		// Сообщение при входе
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			System.out.println("[SequencerMod] Client joined world");
			SequencerNetworkingClient.CLIENT_SEQUENCES.clear();
			if (client.player != null) {
				client.player.sendMessage(Text.literal("SequencerMod: G — Sequencer, H — Morph UI. Тест морфа: /morphc minecraft:zombie"), false);
			}
		});
	}


}