package seq.sequencermod.core;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

/**
 * Глобальная ссылка на активный MinecraftServer.
 * Заполняется при старте сервера и очищается при остановке.
 */
public final class ServerRef {
    private static volatile MinecraftServer CURRENT;

    private ServerRef() {}

    public static void init() {
        // Сохраняем сервер, когда он полностью запущен
        ServerLifecycleEvents.SERVER_STARTED.register(server -> CURRENT = server);
        // Очищаем ссылку при остановке
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            if (CURRENT == server) CURRENT = null;
        });
    }

    public static MinecraftServer get() {
        return CURRENT;
    }
}