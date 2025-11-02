/*
 * GENERATED-SKELETON
 * MC 1.20.1 / Fabric Loader 0.14.22 / Fabric API 0.86.1+1.20.1 / Yarn 1.20.1+build.10 / Loom 1.3.10 / Java 17
 */
package seq.sequencermod.core.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

/**
 * FQCN: seq.sequencermod.core.network.CoreNetwork
 * Subsystem: core
 * Source set: main (COMMON)
 *
 * Purpose:
 * - Register server-side networking handlers.
 *
 * Note:
 * - Client handlers must be registered from a client-only class in src/client
 *   to avoid referencing client APIs from the common source set.
 */
public final class CoreNetwork {
    public static final String NAMESPACE = "sequencermod";

    private CoreNetwork() {}

    public static void registerServerReceivers() {
        // Example:
        // ServerPlayNetworking.registerGlobalReceiver(id("core/request_configs"), (server, player, handler, buf, responseSender) -> { ... });
        throw new UnsupportedOperationException("Skeleton: register server packet receivers");
    }

    public static Identifier id(String path) {
        return new Identifier(NAMESPACE, path);
    }
}