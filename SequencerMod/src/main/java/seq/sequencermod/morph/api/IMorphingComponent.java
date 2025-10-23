/*
 * GENERATED-SKELETON
 */
package seq.sequencermod.morph.api;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * FQCN: seq.sequencermod.morph.api.IMorphingComponent
 * Subsystem: metamorph
 * Source set: main (COMMON)
 *
 * Purpose:
 * - Provide player-attached morph state and NBT persistence.
 */
public interface IMorphingComponent {

    Morph getCurrentMorph();

    /**
     * Server-only: applies morph with full validation and triggers broadcast.
     */
    void setCurrentMorphServer(ServerPlayerEntity player, Morph morph);

    void writeToNbt(NbtCompound out);

    void readFromNbt(NbtCompound in);
}