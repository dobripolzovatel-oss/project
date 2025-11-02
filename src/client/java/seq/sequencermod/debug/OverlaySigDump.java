package seq.sequencermod.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class OverlaySigDump {
    private static boolean done = false;

    public static void dumpOnce() {
        if (done) return;
        done = true;
        try {
            Class<?> cls = Class.forName("net.minecraft.client.gui.hud.InGameOverlayRenderer");
            System.out.println("[OverlaySigDump] Methods of InGameOverlayRenderer:");
            for (var m : cls.getDeclaredMethods()) {
                var sb = new StringBuilder();
                sb.append("  ").append(m.getName()).append("(");
                Class<?>[] ps = m.getParameterTypes();
                for (int i = 0; i < ps.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(ps[i].getName());
                }
                sb.append(") : ").append(m.getReturnType().getName());
                System.out.println(sb);
            }
        } catch (Throwable t) {
            System.out.println("[OverlaySigDump] FAILED: " + t);
        }
    }
}