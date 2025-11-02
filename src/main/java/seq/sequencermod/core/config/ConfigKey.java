/*
 * GENERATED-SKELETON
 */
package seq.sequencermod.core.config;

import java.util.Objects;

/**
 * FQCN: seq.sequencermod.core.config.ConfigKey
 * Subsystem: core
 * Source set: main (COMMON)
 *
 * Purpose:
 * - Immutable descriptor of a config key with metadata, constraints, and scope.
 */
public final class ConfigKey<T> {
    public enum Scope { CLIENT, SERVER, BOTH }

    private final String id;
    private final Class<T> type;
    private final T defaultValue;
    private T currentValue;
    private final boolean syncable;
    private final Scope scope;

    public ConfigKey(String id, Class<T> type, T defaultValue, boolean syncable, Scope scope) {
        this.id = Objects.requireNonNull(id);
        this.type = Objects.requireNonNull(type);
        this.defaultValue = defaultValue;
        this.currentValue = defaultValue;
        this.syncable = syncable;
        this.scope = Objects.requireNonNull(scope);
    }

    public String id() { return id; }
    public Class<T> type() { return type; }
    public T defaultValue() { return defaultValue; }
    public T currentValue() { return currentValue; }
    public boolean syncable() { return syncable; }
    public Scope scope() { return scope; }

    public void setCurrentValue(T value) {
        // Server-side validation expected before calling
        this.currentValue = value;
    }
}