package seq.sequencermod.morph.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import seq.sequencermod.morph.runtime.MorphRuntime;
import seq.sequencermod.morph.runtime.MorphSizeLookupServer;
import seq.sequencermod.network.MorphPackets;
import seq.sequencermod.sequencer.SeqCommands;
import seq.sequencermod.server.morph.MorphServer;

public final class MorphCommands {
    private MorphCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            // /morph <id>
            dispatcher.register(CommandManager.literal("morph")
                    .then(CommandManager.argument("id", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                ServerPlayerEntity player = ctx.getSource().getPlayer();
                                if (player == null) return 0;

                                String idStr = StringArgumentType.getString(ctx, "id");
                                Identifier id = Identifier.tryParse(idStr);
                                if (id == null || !Registries.ENTITY_TYPE.containsId(id)) {
                                    ctx.getSource().sendError(Text.literal("Unknown entity: " + idStr));
                                    return 0;
                                }

                                var err = MorphRuntime.applyMorph(player.getUuid(), idStr, u -> true);
                                if (err != null) {
                                    ctx.getSource().sendError(Text.literal("Morph failed: " + err));
                                    return 0;
                                }

                                // Сбросить персонифицированные размеры (если были) — обычный morph без размеров
                                MorphSizeLookupServer.clear(player.getUuid());

                                // Единый sync-пакет всем
                                MorphServer.syncToAll(player, id);
                                ctx.getSource().sendFeedback(() -> Text.literal("Morphed into " + idStr), true);
                                return 1;
                            })
                    )
            );

            // /unmorph
            dispatcher.register(CommandManager.literal("unmorph")
                    .executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        MorphRuntime.clearMorph(player.getUuid(), u -> true);
                        MorphSizeLookupServer.clear(player.getUuid());
                        MorphServer.syncToAll(player, null);
                        ctx.getSource().sendFeedback(() -> Text.literal("Unmorphed"), true);
                        return 1;
                    })
            );

            // Алиасы (старые)
            dispatcher.register(CommandManager.literal("morphic")
                    .then(CommandManager.argument("id", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                ServerPlayerEntity player = ctx.getSource().getPlayer();
                                if (player == null) return 0;

                                String idStr = StringArgumentType.getString(ctx, "id");
                                Identifier id = Identifier.tryParse(idStr);
                                if (id == null || !Registries.ENTITY_TYPE.containsId(id)) {
                                    ctx.getSource().sendError(Text.literal("Unknown entity: " + idStr));
                                    return 0;
                                }

                                var err = MorphRuntime.applyMorph(player.getUuid(), idStr, u -> true);
                                if (err != null) {
                                    ctx.getSource().sendError(Text.literal("Morph failed: " + err));
                                    return 0;
                                }
                                MorphSizeLookupServer.clear(player.getUuid());
                                MorphServer.syncToAll(player, id);
                                ctx.getSource().sendFeedback(() -> Text.literal("Morphed into " + idStr), true);
                                return 1;
                            })
                    )
            );
            dispatcher.register(CommandManager.literal("unmorphic")
                    .executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        MorphRuntime.clearMorph(player.getUuid(), u -> true);
                        MorphSizeLookupServer.clear(player.getUuid());
                        MorphServer.syncToAll(player, null);
                        ctx.getSource().sendFeedback(() -> Text.literal("Unmorphed"), true);
                        return 1;
                    })
            );

            // Новая команда: /morphsz <id> <width> <height> [eye]
            dispatcher.register(CommandManager.literal("morphsz")
                    .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                            .then(CommandManager.argument("width", FloatArgumentType.floatArg(0.2f, 4.0f))
                                    .then(CommandManager.argument("height", FloatArgumentType.floatArg(0.2f, 6.0f))
                                            .executes(ctx -> {
                                                ServerPlayerEntity player = ctx.getSource().getPlayer();
                                                if (player == null) return 0;
                                                Identifier id = IdentifierArgumentType.getIdentifier(ctx, "id");
                                                if (!Registries.ENTITY_TYPE.containsId(id)) {
                                                    ctx.getSource().sendError(Text.literal("Unknown entity: " + id));
                                                    return 0;
                                                }
                                                float w = FloatArgumentType.getFloat(ctx, "width");
                                                float h = FloatArgumentType.getFloat(ctx, "height");

                                                var err = MorphRuntime.applyMorph(player.getUuid(), id.toString(), u -> true);
                                                if (err != null) {
                                                    ctx.getSource().sendError(Text.literal("Morph failed: " + err));
                                                    return 0;
                                                }

                                                // Сохраняем runtime‑размеры (Variant B)
                                                MorphSizeLookupServer.set(player.getUuid(), id, w, h, null);

                                                // Пересчитываем хитбокс и шлём синк (MorphServer включает размеры, если реализовано)
                                                player.calculateDimensions();
                                                MorphServer.syncToAll(player, id);

                                                // На всякий случай сразу отправим размеры клиентам в том же формате, что и MorphServer
                                                PacketByteBuf out = PacketByteBufs.create();
                                                out.writeUuid(player.getUuid());
                                                out.writeBoolean(true);
                                                out.writeIdentifier(id);
                                                out.writeBoolean(true); // hasSize
                                                out.writeFloat(w);
                                                out.writeFloat(h);
                                                out.writeBoolean(false); // hasEye=false
                                                for (ServerPlayerEntity sp : player.server.getPlayerManager().getPlayerList()) {
                                                    ServerPlayNetworking.send(sp, MorphPackets.S2C_MORPH_SYNC, PacketByteBufs.copy(out));
                                                }

                                                ctx.getSource().sendFeedback(() -> Text.literal("Morphed (with size) into " + id + " w=" + w + " h=" + h), true);
                                                return 1;
                                            })
                                            .then(CommandManager.argument("eye", FloatArgumentType.floatArg(0.1f, 6.0f))
                                                    .executes(ctx -> {
                                                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                                                        if (player == null) return 0;
                                                        Identifier id = IdentifierArgumentType.getIdentifier(ctx, "id");
                                                        if (!Registries.ENTITY_TYPE.containsId(id)) {
                                                            ctx.getSource().sendError(Text.literal("Unknown entity: " + id));
                                                            return 0;
                                                        }
                                                        float w = FloatArgumentType.getFloat(ctx, "width");
                                                        float h = FloatArgumentType.getFloat(ctx, "height");
                                                        float eye = FloatArgumentType.getFloat(ctx, "eye");

                                                        var err = MorphRuntime.applyMorph(player.getUuid(), id.toString(), u -> true);
                                                        if (err != null) {
                                                            ctx.getSource().sendError(Text.literal("Morph failed: " + err));
                                                            return 0;
                                                        }

                                                        MorphSizeLookupServer.set(player.getUuid(), id, w, h, eye);
                                                        player.calculateDimensions();
                                                        MorphServer.syncToAll(player, id);

                                                        PacketByteBuf out = PacketByteBufs.create();
                                                        out.writeUuid(player.getUuid());
                                                        out.writeBoolean(true);
                                                        out.writeIdentifier(id);
                                                        out.writeBoolean(true); // hasSize
                                                        out.writeFloat(w);
                                                        out.writeFloat(h);
                                                        out.writeBoolean(true); // hasEye
                                                        out.writeFloat(eye);
                                                        for (ServerPlayerEntity sp : player.server.getPlayerManager().getPlayerList()) {
                                                            ServerPlayNetworking.send(sp, MorphPackets.S2C_MORPH_SYNC, PacketByteBufs.copy(out));
                                                        }

                                                        ctx.getSource().sendFeedback(() -> Text.literal("Morphed (with size) into " + id + " w=" + w + " h=" + h + " eye=" + eye), true);
                                                        return 1;
                                                    })
                                            )
                                    )
                            )
                    )
            );

            // /seq ...
            SeqCommands.register(dispatcher);
        });
    }
}