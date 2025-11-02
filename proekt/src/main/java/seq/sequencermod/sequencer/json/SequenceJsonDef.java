package seq.sequencermod.sequencer.json;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON-модель секвенции. Поддерживает "id" и alias "entity" для шага morph.
 */
public final class SequenceJsonDef {
    public List<Step> steps = new ArrayList<>();

    public static final class Vec3 {
        public double x = 0.0;
        public double y = 0.0;
        public double z = 0.0;
    }

    public static final class Step {
        public String type = "";

        // morph: допускаем "id" и "entity"
        @SerializedName(value = "id", alternate = {"entity"})
        public String id;

        // delay
        public int ticks = 0;

        // morph
        public int duration = 0;

        // message
        public String text;

        // play_sound
        public float volume = 1.0f;
        public float pitch = 1.0f;

        // run_command
        public String command;
        public String as;

        // particle
        public int count = 1;
        public double speed = 0.0;
        public Vec3 offset;
    }
}