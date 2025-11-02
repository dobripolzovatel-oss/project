/*
 * TEMP IMPLEMENTATION to avoid UOE during testing
 */
package seq.sequencermod.extendedreach;

import net.minecraft.server.network.ServerPlayerEntity;

public final class BlockReach {
    private static volatile float reach = 5.0f; // визуал и логика должны совпадать

    private BlockReach() {}

    public static double getReach(ServerPlayerEntity player) {
        return reach;
    }

    public static double getReachSq(ServerPlayerEntity player) {
        double r = getReach(player);
        return r * r;
    }

    public static void setReachFromServer(float newReach) {
        reach = newReach;
    }
}