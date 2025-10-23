package seq.sequencermod;

import java.util.ArrayList;
import java.util.List;

public class SequencePreset {
    public String name;
    public final List<Step> steps = new ArrayList<>();

    public SequencePreset() { this("preset"); }
    public SequencePreset(String name) { this.name = name == null ? "preset" : name; }

    public static class Step {
        public String entityId;
        public int durationTicks;

        public Step() { }
        public Step(String entityId, int durationTicks) {
            this.entityId = entityId == null ? "" : entityId;
            this.durationTicks = Math.max(1, durationTicks);
        }
    }
}