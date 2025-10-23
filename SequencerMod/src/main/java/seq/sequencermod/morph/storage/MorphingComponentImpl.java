/*
 * GENERATED-SKELETON
 */
package seq.sequencermod.morph.storage;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import seq.sequencermod.morph.api.IMorphingComponent;
import seq.sequencermod.morph.api.Morph;

/**
 * FQCN: seq.sequencermod.morph.storage.MorphingComponentImpl
 * Subsystem: metamorph
 * Source set: main (COMMON)
 */
public final class MorphingComponentImpl implements IMorphingComponent {
    private Morph current;

    @Override
    public Morph getCurrentMorph() {
        return current;
    }

    @Override
    public void setCurrentMorphServer(ServerPlayerEntity player, Morph morph) {
        throw new UnsupportedOperationException("Skeleton");
    }

    @Override
    public void writeToNbt(NbtCompound out) {
        throw new UnsupportedOperationException("Skeleton");
    }

    @Override
    public void readFromNbt(NbtCompound in) {
        throw new UnsupportedOperationException("Skeleton");
    }
}