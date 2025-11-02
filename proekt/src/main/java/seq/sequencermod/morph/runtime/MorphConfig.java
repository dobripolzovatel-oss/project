package seq.sequencermod.morph.runtime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Простая JSON-конфигурация.
 * Файл: config/morph.json
 * По умолчанию allowNonLivingMorphs = true.
 */
public final class MorphConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String DEFAULT_PATH = "config/morph.json";

    public boolean allowNonLivingMorphs = true;

    public static MorphConfig loadOrCreate() {
        return loadOrCreate(DEFAULT_PATH);
    }

    public static MorphConfig loadOrCreate(String path) {
        File file = new File(path);
        if (!file.exists()) {
            MorphConfig cfg = new MorphConfig();
            cfg.save(path);
            return cfg;
        }
        try (Reader r = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            MorphConfig cfg = GSON.fromJson(r, MorphConfig.class);
            if (cfg == null) cfg = new MorphConfig();
            return cfg;
        } catch (IOException e) {
            e.printStackTrace();
            MorphConfig cfg = new MorphConfig();
            cfg.save(path);
            return cfg;
        }
    }

    public void save() { save(DEFAULT_PATH); }

    public void save(String path) {
        File file = new File(path);
        file.getParentFile().mkdirs();
        try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            GSON.toJson(this, w);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}