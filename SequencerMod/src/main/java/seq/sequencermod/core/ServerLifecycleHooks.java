package seq.sequencermod.core;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import seq.sequencermod.core.debug.DebugTaps;
import seq.sequencermod.server.morph.MorphServer;
import seq.sequencermod.morph.runtime.MorphSizeLookupServer;
import seq.sequencermod.morph.runtime.PlayerSizeServerStore; // <-- добавлено

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Хуки жизненного цикла сервера.
 */
public final class ServerLifecycleHooks implements ModInitializer {
    private static Consumer<ServerPlayerEntity> onPlayerDisconnect = p -> {
        if (DebugTaps.active.get()) {
            DebugTaps.logf("DISCONNECT: %s (%s)", p.getGameProfile().getName(), p.getUuid());
        }
        // Очистка персональных размеров и морфа
        try {
            MorphSizeLookupServer.clear(p.getUuid());
            PlayerSizeServerStore.clear(p.getUuid()); // <-- добавлено
        } catch (Throwable t) {
            t.printStackTrace();
        }
        try {
            MorphServer.syncToAll(p, null);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    };

    private static Consumer<MinecraftServer> onServerStopping = s -> {
        if (DebugTaps.active.get()) {
            DebugTaps.log("SERVER_STOPPING");
        }
        try {
            MorphSizeLookupServer.invalidateCache();
            PlayerSizeServerStore.invalidate(); // <-- добавлено
        } catch (Throwable t) {
            t.printStackTrace();
        }
    };

    // Необязательный комбинированный хук
    private static BiConsumer<MinecraftServer, ServerPlayerEntity> onPlayerDisconnectWithServer = (s, p) -> {};

    public static void setOnPlayerDisconnect(Consumer<ServerPlayerEntity> handler) {
        onPlayerDisconnect = Objects.requireNonNull(handler);
    }

    public static void setOnPlayerDisconnect(BiConsumer<MinecraftServer, ServerPlayerEntity> handler) {
        onPlayerDisconnectWithServer = Objects.requireNonNull(handler);
    }

    public static void setOnServerStopping(Consumer<MinecraftServer> handler) {
        onServerStopping = Objects.requireNonNull(handler);
    }

    @Override
    public void onInitialize() {
        ServerRef.init();
        MorphServer.bootstrap();

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.player;
            try {
                onPlayerDisconnect.accept(player);
            } catch (Throwable t) {
                t.printStackTrace();
            }
            try {
                onPlayerDisconnectWithServer.accept(server, player);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            try {
                onServerStopping.accept(server);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
    }
}