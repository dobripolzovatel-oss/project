package seq.sequencermod.sequencer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import seq.sequencermod.sequencer.json.SequenceJsonDef;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Грузит JSON-секвенции из data/sequencermod/sequences/*.json
 * Перезагрузка на /reload.
 */
public final class SequenceJsonLoader {

    private static final String MODID = "sequencermod";
    private static final String FOLDER = "sequences";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile boolean INITIALIZED = false;

    private SequenceJsonLoader() {}

    public static void init() {
        if (INITIALIZED) return;
        INITIALIZED = true;

        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return new Identifier(MODID, "sequence_loader");
            }

            @Override
            public void reload(ResourceManager manager) {
                Map<String, SequenceJsonDef> loaded = new HashMap<>();

                var found = manager.findResources(FOLDER, id -> id.getPath().endsWith(".json"));
                System.out.println("[sequencermod] SequenceJsonLoader: found " + found.size() + " resources in '" + FOLDER + "' across namespaces");

                found.forEach((id, res) -> {
                    try {
                        if (!MODID.equals(id.getNamespace())) {
                            // Логируем, чтобы было видно, где лежат «чужие» ресурсы
                            System.out.println("[sequencermod] skip (other ns): " + id);
                            return;
                        }
                        String key = fileNameToId(id);
                        SequenceJsonDef def = readJson(res);
                        if (def != null && def.steps != null) {
                            loaded.put(key, def);
                            System.out.println("[sequencermod] loaded sequence: " + key + " from " + id);
                        } else {
                            System.out.println("[sequencermod] empty/invalid sequence: " + id);
                        }
                    } catch (Exception e) {
                        System.err.println("[sequencermod] Failed to load sequence " + id + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                });

                if (loaded.isEmpty()) {
                    System.out.println("[sequencermod] No sequences found. Put JSON files into data/sequencermod/sequences/*.json and run /reload");
                }
                SequenceRegistry.setLoaded(loaded);
            }
        });
    }

    private static SequenceJsonDef readJson(Resource resource) throws Exception {
        try (var in = resource.getInputStream();
             var r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return GSON.fromJson(r, SequenceJsonDef.class);
        }
    }

    // sequences/zombie_then_spider.json -> "zombie_then_spider"
    private static String fileNameToId(Identifier id) {
        String path = id.getPath(); // sequences/xxx.json
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        if (name.endsWith(".json")) name = name.substring(0, name.length() - 5);
        return name;
    }
}