package seq.sequencermod.morph.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import seq.sequencermod.morph.runtime.PlayerSizeServerStore;
import seq.sequencermod.network.MorphPackets;

public final class PlayerSizeCommands {
    private PlayerSizeCommands() {}

    private static final float MIN_W   = 0.001f;
    private static final float MIN_H   = 0.001f;
    private static final float MIN_EYE = 0.001f;
    private static final float EYE_MARGIN = 0.0005f;

    public static void register() {
        System.out.println("[SequencerSize][Morph] /setsize registered (min -> 0.001).");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {

            dispatcher.register(
                    CommandManager.literal("setsize")
                            .then(CommandManager.argument("width", FloatArgumentType.floatArg(MIN_W))
                                    .then(CommandManager.argument("height", FloatArgumentType.floatArg(MIN_H))
                                            .executes(ctx -> {
                                                ServerPlayerEntity p = ctx.getSource().getPlayer();
                                                if (p == null) return 0;
                                                float rw = FloatArgumentType.getFloat(ctx, "width");
                                                float rh = FloatArgumentType.getFloat(ctx, "height");
                                                final float w = rw < MIN_W ? MIN_W : rw;
                                                final float h = rh < MIN_H ? MIN_H : rh;

                                                PlayerSizeServerStore.set(p.getUuid(), w, h, null);
                                                safeRecalc(p);
                                                broadcastSize(p, true, w, h, null);
                                                final String msg = "setsize -> w=" + w + " h=" + h;
                                                ctx.getSource().sendFeedback(() -> Text.literal(msg), true);
                                                return 1;
                                            })
                                            .then(CommandManager.argument("eye", FloatArgumentType.floatArg(MIN_EYE))
                                                    .executes(ctx -> {
                                                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                                                        if (p == null) return 0;
                                                        float rw = FloatArgumentType.getFloat(ctx, "width");
                                                        float rh = FloatArgumentType.getFloat(ctx, "height");
                                                        float re = FloatArgumentType.getFloat(ctx, "eye");
                                                        final float w = rw < MIN_W ? MIN_W : rw;
                                                        final float h = rh < MIN_H ? MIN_H : rh;
                                                        float tEye = re < MIN_EYE ? MIN_EYE : re;
                                                        if (tEye > h) tEye = h - EYE_MARGIN;
                                                        if (tEye < MIN_EYE) tEye = MIN_EYE;
                                                        final float eye = tEye;

                                                        PlayerSizeServerStore.set(p.getUuid(), w, h, eye);
                                                        safeRecalc(p);
                                                        broadcastSize(p, true, w, h, eye);
                                                        final String msg = "setsize -> w=" + w + " h=" + h + " eye=" + eye;
                                                        ctx.getSource().sendFeedback(() -> Text.literal(msg), true);
                                                        return 1;
                                                    })
                                            )
                                    )
                            )
            );

            dispatcher.register(
                    CommandManager.literal("clearsize")
                            .executes(ctx -> {
                                ServerPlayerEntity p = ctx.getSource().getPlayer();
                                if (p == null) return 0;
                                PlayerSizeServerStore.clear(p.getUuid());
                                safeRecalc(p);
                                broadcastSize(p, false, 0f, 0f, null);
                                final String msg = "clearsize -> reset";
                                ctx.getSource().sendFeedback(() -> Text.literal(msg), true);
                                return 1;
                            })
            );
        });
    }

    private static void safeRecalc(ServerPlayerEntity p) {
        try { p.calculateDimensions(); } catch (Throwable ignored) {}
    }

    private static void broadcastSize(ServerPlayerEntity changed, boolean has, float w, float h, Float eye) {
        PacketByteBuf out = MorphPackets.buildPlayerSizeSyncS2C(changed.getUuid(), has, w, h, eye);
        for (var sp : changed.server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(sp, MorphPackets.S2C_PLAYER_SIZE_SYNC, PacketByteBufs.copy(out));
        }
    }
}