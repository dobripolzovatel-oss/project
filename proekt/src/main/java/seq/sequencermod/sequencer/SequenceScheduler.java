package seq.sequencermod.sequencer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Простой планировщик задач по серверным тикам.
 * Позволяет запускать отложенные шаги секвенций для конкретных игроков.
 */
public final class SequenceScheduler {
    private static final class Task {
        int ticksLeft;
        final Consumer<MinecraftServer> action;
        final UUID player;

        Task(UUID player, int ticksLeft, Consumer<MinecraftServer> action) {
            this.player = player;
            this.ticksLeft = ticksLeft;
            this.action = action;
        }
    }

    private static final Map<Integer, Task> TASKS = new ConcurrentHashMap<>();
    private static volatile int NEXT_ID = 1;
    private static volatile boolean INITIALIZED = false;

    private SequenceScheduler() {}

    public static void init() {
        if (INITIALIZED) return;
        INITIALIZED = true;
        ServerTickEvents.END_SERVER_TICK.register(server -> tick(server));
    }

    private static void tick(MinecraftServer server) {
        Iterator<Map.Entry<Integer, Task>> it = TASKS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Task> e = it.next();
            Task t = e.getValue();
            if (--t.ticksLeft <= 0) {
                try {
                    t.action.accept(server);
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
                it.remove();
            }
        }
    }

    /**
     * Запланировать задачу через ticks тиков.
     */
    public static int schedule(UUID player, int ticks, Consumer<MinecraftServer> action) {
        int id = NEXT_ID++;
        TASKS.put(id, new Task(player, Math.max(0, ticks), action));
        return id;
    }

    /**
     * Отменить все задачи игрока (например, при stop секвенции).
     */
    public static void cancelFor(UUID player) {
        TASKS.entrySet().removeIf(e -> e.getValue().player.equals(player));
    }
}