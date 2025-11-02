package seq.sequencermod.client.player;

import seq.sequencermod.client.ui.SequencePreset;

public class SimpleSequencePlayerClient {
    public static class Info {
        public boolean running;
        public boolean paused;
    }
    private static final Info info = new Info();

    public static void play(SequencePreset preset, int startDelayTicks, boolean infinite, int loops) {
        info.running = true;
        info.paused = false;
        // Заглушка: реальной проигровки пока нет
    }
    public static void stop() {
        info.running = false;
        info.paused = false;
    }
    public static void pause() { if (info.running) info.paused = true; }
    public static void resume() { if (info.running) info.paused = false; }
    public static Info getInfo() { return info; }
}