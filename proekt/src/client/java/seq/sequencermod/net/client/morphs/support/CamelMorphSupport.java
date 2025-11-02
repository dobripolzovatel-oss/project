package seq.sequencermod.net.client.morphs.support;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import seq.sequencermod.net.client.MorphClientSync;
import seq.sequencermod.net.client.SimpleMorphs;
import seq.sequencermod.net.client.morphs.render.camel.CamelMorphState;

public final class CamelMorphSupport {
    private static KeyBinding sitToggle;
    private static KeyBinding dashKey;
    private static KeyBinding skinCycleKey;
    private static KeyBinding saddleToggle;

    private static boolean prevDashPressed = false;

    public static void initKeybinds() {
        sitToggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.sequencer.camel.sit_toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "key.categories.gameplay"
        ));
        dashKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.sequencer.camel.dash",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "key.categories.gameplay"
        ));
        skinCycleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.sequencer.camel.cycle_skin",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_I,
                "key.categories.gameplay"
        ));
        saddleToggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.sequencer.camel.toggle_saddle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                "key.categories.gameplay"
        ));

        // ПКМ по кактусу → проиграть звук поедания у морфа-верблюда
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!world.isClient) return ActionResult.PASS;
            if (hit == null) return ActionResult.PASS;
            if (!world.getBlockState(hit.getBlockPos()).isOf(Blocks.CACTUS)) return ActionResult.PASS;

            Identifier type = MorphClientSync.getMorphType(player.getUuid());
            if (type == null || !"minecraft".equals(type.getNamespace()) || !"camel".equals(type.getPath())) {
                return ActionResult.PASS;
            }

            if (!(player instanceof AbstractClientPlayerEntity acp)) {
                return ActionResult.PASS;
            }

            CamelMorphState state = SimpleMorphs.getCamelState(acp);
            if (state != null) {
                state.playEatCactus(acp);
            }
            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) { prevDashPressed = false; return; }
            CamelMorphState state = SimpleMorphs.getCamelState(client.player);
            if (state == null) { prevDashPressed = false; return; }

            while (sitToggle.wasPressed()) {
                state.requestToggleSit();
            }

            boolean cur = dashKey.isPressed();
            if (cur && !prevDashPressed) {
                state.requestDashStart();
            }
            if (!cur && prevDashPressed) {
                state.requestDashRelease(client.player);
            }
            prevDashPressed = cur;

            while (skinCycleKey.wasPressed()) {
                state.cycleNextSkin();
            }

            while (saddleToggle.wasPressed()) {
                boolean next = !state.isSaddled();
                state.setSaddled(next);
                state.setShowReins(next);
            }
        });
    }
}