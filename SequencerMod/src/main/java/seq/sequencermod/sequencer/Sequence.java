package seq.sequencermod.sequencer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public final class Sequence {
    public final String id;
    public final boolean loop;
    public final List<Step> steps;

    public Sequence(String id, boolean loop, List<Step> steps) {
        this.id = id;
        this.loop = loop;
        this.steps = steps;
    }

    public static Sequence fromJson(JsonObject root) {
        String id = root.has("id") ? root.get("id").getAsString() : "";
        boolean loop = root.has("loop") && root.get("loop").getAsBoolean();
        List<Step> steps = new ArrayList<>();
        JsonArray arr = root.getAsJsonArray("steps");
        if (arr != null) {
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject o = el.getAsJsonObject();
                String type = o.get("type").getAsString();
                switch (type) {
                    case "wait" -> steps.add(Step.waitTicks(o.has("ticks") ? o.get("ticks").getAsInt() : 0));
                    case "morph" -> steps.add(Step.morph(
                            o.get("entity").getAsString(),
                            o.has("duration") ? o.get("duration").getAsInt() : 0));
                    case "clear_morph" -> steps.add(Step.clearMorph());
                    case "message" -> steps.add(Step.message(o.get("text").getAsString()));
                    case "sound" -> steps.add(Step.sound(o.get("sound").getAsString(),
                            o.has("volume") ? o.get("volume").getAsFloat() : 1.0f,
                            o.has("pitch") ? o.get("pitch").getAsFloat() : 1.0f));
                    case "effect" -> steps.add(Step.effect(
                            o.get("effect").getAsString(),
                            o.has("duration") ? o.get("duration").getAsInt() : 40,
                            o.has("amplifier") ? o.get("amplifier").getAsInt() : 0));
                    default -> {
                        // ignore unknown
                    }
                }
            }
        }
        return new Sequence(id, loop, steps);
    }

    public sealed interface Step permits Step.Wait, Step.Morph, Step.ClearMorph, Step.Message, Step.Sound, Step.Effect {
        record Wait(int ticks) implements Step {}
        record Morph(String entityId, int duration) implements Step {}
        record ClearMorph() implements Step {}
        record Message(String text) implements Step {}
        record Sound(String soundId, float volume, float pitch) implements Step {}
        record Effect(String effectId, int duration, int amplifier) implements Step {}

        static Wait waitTicks(int t) { return new Wait(t); }
        static Morph morph(String id, int d) { return new Morph(id, d); }
        static ClearMorph clearMorph() { return new ClearMorph(); }
        static Message message(String t) { return new Message(t); }
        static Sound sound(String s, float v, float p) { return new Sound(s, v, p); }
        static Effect effect(String e, int d, int a) { return new Effect(e, d, a); }
    }
}