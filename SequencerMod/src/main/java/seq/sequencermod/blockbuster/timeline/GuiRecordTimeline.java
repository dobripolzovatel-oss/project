/*
 * GENERATED-SKELETON
 */
package seq.sequencermod.blockbuster.timeline;

/**
 * FQCN: seq.sequencermod.blockbuster.timeline.GuiRecordTimeline
 * Subsystem: blockbuster
 * Source set: main (COMMON) for data structures; client UI in client package (Предположение: split if needed).
 *
 * Purpose:
 * - Data model and contracts for timeline tracks/clips and operations (insert/delete/split/duplicate/nudge/snap).
 */
public final class GuiRecordTimeline {

    public static final class Clip {
        public int start;
        public int duration;
    }

    public void insertClip(Clip clip) {
        throw new UnsupportedOperationException("Skeleton");
    }
}