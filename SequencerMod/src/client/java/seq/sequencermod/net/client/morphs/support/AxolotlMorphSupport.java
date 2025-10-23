package seq.sequencermod.net.client.morphs.support;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import seq.sequencermod.net.client.MorphClientSync;
import seq.sequencermod.net.client.morphs.runtime.AxolotlMorphRuntime;
import seq.sequencermod.network.MorphPackets; // <-- добавлен импорт

@Environment(EnvType.CLIENT)
public final class AxolotlMorphSupport {

    private static KeyBinding keyPlayDead;
    private static KeyBinding keyCycleVariant;

    private static boolean prevPlayDeadPressed = false;
    private static boolean prevVariantPressed  = false;

    private AxolotlMorphSupport() {}

    public static void clientInitKeybinding() {
        keyPlayDead = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.sequencer.axolotl.play_dead",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "key.categories.gameplay"
        ));

        keyCycleVariant = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.sequencer.axolotl.variant_cycle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "key.categories.gameplay"
        ));
    }

    public static void clientTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        PlayerEntity player = mc.player;

        Identifier morph = MorphClientSync.getMorphType(player.getUuid());
        if (morph == null || !"minecraft".equals(morph.getNamespace()) || !"axolotl".equals(morph.getPath()))
            return; // Не в морфе аксолотля

        // PLAY DEAD
        boolean nowDeadPressed = keyPlayDead.isPressed();
        if (nowDeadPressed && !prevPlayDeadPressed) {
            AxolotlMorphRuntime rt = AxolotlMorphRuntime.get(player.getUuid(), true);
            if (!rt.isPlayingDead()) {
                requestPlayDeadStart(player);
            } else {
                requestPlayDeadStop(player);
            }
        }
        prevPlayDeadPressed = nowDeadPressed;

        // Смена варианта
        boolean nowVarPressed = keyCycleVariant.isPressed();
        if (nowVarPressed && !prevVariantPressed) {
            cycleVariantClient(player);
        }
        prevVariantPressed = nowVarPressed;
    }

    private static void requestPlayDeadStart(PlayerEntity p) {
        AxolotlMorphRuntime rt = AxolotlMorphRuntime.get(p.getUuid(), true);
        rt.startPlayDeadLocal(200);

        if (ClientPlayNetworking.canSend(MorphPackets.C2S_AXOLOTL_PLAY_DEAD)) {
            ClientPlayNetworking.send(MorphPackets.C2S_AXOLOTL_PLAY_DEAD, MorphPackets.buildPlayDeadC2S(true));
        }
    }

    private static void requestPlayDeadStop(PlayerEntity p) {
        AxolotlMorphRuntime rt = AxolotlMorphRuntime.get(p.getUuid(), true);
        rt.stopPlayDeadLocal();

        if (ClientPlayNetworking.canSend(MorphPackets.C2S_AXOLOTL_PLAY_DEAD)) {
            ClientPlayNetworking.send(MorphPackets.C2S_AXOLOTL_PLAY_DEAD, MorphPackets.buildPlayDeadC2S(false));
        }
    }

    private static void cycleVariantClient(PlayerEntity p) {
        AxolotlMorphRuntime rt = AxolotlMorphRuntime.get(p.getUuid(), true);
        int next = (rt.getVariant() + 1) % 5; // 0..4
        rt.setVariant(next);
        if (ClientPlayNetworking.canSend(MorphPackets.C2S_AXOLOTL_SET_VARIANT)) {
            ClientPlayNetworking.send(MorphPackets.C2S_AXOLOTL_SET_VARIANT, MorphPackets.buildVariantC2S(next));
        }
    }
}