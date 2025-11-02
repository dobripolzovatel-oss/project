/*
 * GENERATED-SKELETON
 */
package seq.sequencermod.morph.api;

import java.util.List;

/**
 * FQCN: seq.sequencermod.morph.api.MorphManager
 * Subsystem: metamorph
 * Source set: main (COMMON)
 *
 * Purpose:
 * - Server validation of morphs; registry of morph factories (including ChameleonFactory integration).
 */
public final class MorphManager {

    public interface Factory {
        boolean supports(String key);
        Morph createDefault(String key);
    }

    public static final MorphManager INSTANCE = new MorphManager();

    private final List<Factory> factories = new java.util.concurrent.CopyOnWriteArrayList<>();

    public void addFactory(Factory factory) { factories.add(factory); }

    public Morph create(String key) {
        throw new UnsupportedOperationException("Skeleton");
    }
}