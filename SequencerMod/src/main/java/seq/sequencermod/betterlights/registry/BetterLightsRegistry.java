/*
 * GENERATED-SKELETON
 */
package seq.sequencermod.betterlights.registry;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import seq.sequencermod.core.SequencerMod;
import seq.sequencermod.betterlights.block.GlowingBricksBlock;

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

    public static final Block GLOWING_BRICKS = Registry.register(
        Registries.BLOCK,
        id("glowing_bricks"),
        new GlowingBricksBlock()
    );

    public static final Item GLOWING_BRICKS_ITEM = Registry.register(
        Registries.ITEM,
        id("glowing_bricks"),
        new BlockItem(GLOWING_BRICKS, new Item.Settings())
    );

    public static void init() {
        // Called to ensure static initializers run
        SequencerMod.LOG.info("BetterLightsRegistry initialized");
    }
}