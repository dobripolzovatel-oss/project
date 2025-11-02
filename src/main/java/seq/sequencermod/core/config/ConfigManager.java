/*
 * GENERATED-SKELETON
 */
package seq.sequencermod.core.config;

import net.minecraft.nbt.NbtCompound;

import java.nio.file.Path;
import java.util.Optional;

/**
 * FQCN: seq.sequencermod.core.config.ConfigManager
 * Subsystem: core
 * Source set: main (COMMON)
 *
 * Purpose:
 * - Load/save JSON/JSON5 config files, manage server-authoritative snapshot and client sync apply.
 *
 * Serialization:
 * - JSON lenient parsing with comments stripped (Предположение).
 * - Snapshot NBT schema: { "v": int, "keys": List<Compound> } with bounded size (see CONFIGS.md).
 */
public final class ConfigManager {

    public void loadAll(Path configRoot) {
        throw new UnsupportedOperationException("Skeleton");
    }

    public void saveAll() {
        throw new UnsupportedOperationException("Skeleton");
    }

    public NbtCompound toSnapshotNbt() {
        throw new UnsupportedOperationException("Skeleton");
    }

    public void applySnapshotNbt(NbtCompound nbt) {
        throw new UnsupportedOperationException("Skeleton");
    }

    public Optional<ConfigKey<?>> getKey(String id) {
        throw new UnsupportedOperationException("Skeleton");
    }
}