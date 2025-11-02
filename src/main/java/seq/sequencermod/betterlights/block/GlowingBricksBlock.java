/*
 * GENERATED-SKELETON
 */
package seq.sequencermod.betterlights.block;

import net.minecraft.block.Block;
import net.minecraft.block.AbstractBlock;

/**
 * FQCN: seq.sequencermod.betterlights.block.GlowingBricksBlock
 * Subsystem: betterlights
 * Source set: main (COMMON)
 *
 * Behavior:
 * - Stone-like material, hardness ~1.0F, luminance 15.
 */
public final class GlowingBricksBlock extends Block {

    public GlowingBricksBlock() {
        super(AbstractBlock.Settings.create().luminance(state -> 15).strength(1.0f));
    }
}