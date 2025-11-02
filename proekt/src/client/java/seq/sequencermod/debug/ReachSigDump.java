package seq.sequencermod.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ReachSigDump {
    private static boolean done = false;

    public static void dumpOnce() {
        if (done) return;
        done = true;
        dump("net.minecraft.client.network.ClientPlayerInteractionManager");
        dump("net.minecraft.client.render.GameRenderer");
        dump("net.minecraft.client.MinecraftClient");
    }

    private static void dump(String className) {
        try {
            Class<?> cls = Class.forName(className);
            System.out.println("[ReachSigDump] Methods of " + className + ":");
            for (var m : cls.getDeclaredMethods()) {
                var sb = new StringBuilder();
                sb.append("  ").append(m.getName()).append("(");
                var ps = m.getParameterTypes();
                for (int i = 0; i < ps.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(ps[i].getName());
                }
                sb.append(") : ").append(m.getReturnType().getName());
                System.out.println(sb);
            }
        } catch (Throwable t) {
            System.out.println("[ReachSigDump] FAILED for " + className + ": " + t);
        }
    }
}