package seq.sequencermod.morph.runtime;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Map;

public final class MorphDimensionsLoader extends JsonDataLoader implements IdentifiableResourceReloadListener {
    private static final Logger LOG = LoggerFactory.getLogger("Sequencer|Server");
    private static final Identifier FABRIC_ID = new Identifier("sequencermod", "morph_dimensions");

    public MorphDimensionsLoader() {
        super(new GsonBuilder().setLenient().create(), "morph_dimensions");
    }

    @Override
    public Identifier getFabricId() {
        return FABRIC_ID;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> data, ResourceManager manager, Profiler profiler) {
        MorphDimensionsRegistry.clear();
        int count = 0;

        for (Map.Entry<Identifier, JsonElement> e : data.entrySet()) {
            Identifier resId = e.getKey();
            JsonElement rootEl = e.getValue();
            if (!rootEl.isJsonObject()) continue;

            JsonObject root = rootEl.getAsJsonObject();

            Identifier typeId = root.has("type") ? new Identifier(root.get("type").getAsString()) : null;
            if (typeId == null) {
                LOG.warn("MorphDimensionsLoader: '{}' missing 'type' field, skipping", resId);
                continue;
            }

            if (root.has("default") && root.get("default").isJsonObject()) {
                JsonObject def = root.getAsJsonObject("default");
                EntityDimensions dims = readDims(def);
                if (dims != null) {
                    MorphDimensionsRegistry.putDefault(typeId, dims);
                }
            }

            if (root.has("poses") && root.get("poses").isJsonObject()) {
                JsonObject poses = root.getAsJsonObject("poses");
                for (Map.Entry<String, JsonElement> pe : poses.entrySet()) {
                    String poseKey = pe.getKey();
                    JsonElement val = pe.getValue();
                    if (!val.isJsonObject()) continue;

                    EntityPose pose = parsePose(poseKey);
                    if (pose == null) continue;

                    EntityDimensions dims = readDims(val.getAsJsonObject());
                    if (dims != null) {
                        MorphDimensionsRegistry.putPose(typeId, pose, dims);
                    }
                }
            }

            count++;
        }

        LOG.info("MorphDimensionsLoader: loaded {} morph dimension files", count);
    }

    private static EntityDimensions readDims(JsonObject obj) {
        if (!obj.has("width") || !obj.has("height")) return null;
        float w = obj.get("width").getAsFloat();
        float h = obj.get("height").getAsFloat();
        boolean fixed = obj.has("fixed") && obj.get("fixed").getAsBoolean();
        return fixed ? EntityDimensions.fixed(w, h) : EntityDimensions.changing(w, h);
    }

    private static EntityPose parsePose(String key) {
        if (key == null) return null;
        String k = key.trim().toUpperCase(Locale.ROOT);
        try {
            return EntityPose.valueOf(k);
        } catch (IllegalArgumentException ignored) {
            switch (k) {
                case "SNEAKING": return EntityPose.CROUCHING;
                case "ELYTRA": return EntityPose.FALL_FLYING;
                default: return null;
            }
        }
    }
}