/*
 * GENERATED-SKELETON
 */
package seq.sequencermod.betterlights.registry;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import seq.sequencermod.core.SequencerMod;
import seq.sequencermod.betterlights.block.GlowingBricksBlock;
import net.minecraft.block.Block;

/**
 * FQCN: seq.sequencermod.betterlights.registry.BetterLightsRegistry
 * Subsystem: betterlights
 * Source set: main (COMMON)
 *
 * Purpose:
 * - Register blocks/items/blockentities and creative tab entries.
 */
public final class BetterLightsRegistry {
    private BetterLightsRegistry() {}

    private static Identifier id(String path) {
        return new Identifier(SequencerMod.MOD_ID, path);
    }

    // пример использования
    // Identifier GLOWING_BRICKS_ID = id("glowing_bricks");
}