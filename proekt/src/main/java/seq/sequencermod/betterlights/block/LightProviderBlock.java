/*
 * GENERATED-SKELETON
 */
package seq.sequencermod.betterlights.block;

import net.minecraft.block.Block;
import net.minecraft.block.AbstractBlock;

/**
 * FQCN: seq.sequencermod.betterlights.block.LightProviderBlock
 * Subsystem: betterlights
 * Source set: main (COMMON)
 *
 * Behavior:
 * - Invisible air-like block; no collision/drop; replaceable; luminance 15.
 */
public final class LightProviderBlock extends Block {

    public LightProviderBlock() {
        super(AbstractBlock.Settings.create().luminance(s -> 15).noCollision());
    }
}