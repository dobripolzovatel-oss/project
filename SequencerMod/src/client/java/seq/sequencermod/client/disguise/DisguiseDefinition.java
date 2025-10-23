package seq.sequencermod.client.disguise;

import net.minecraft.util.Identifier;
import org.joml.Vector3f;

/**
 * Минимальная заглушка для совместимости: только то, что требуется JsonLoader'у.
 */
public final class DisguiseDefinition {
    public enum Kind { JSON_BLOCK, JSON_ITEM }

    public final Kind kind;
    public final Identifier resource;
    public final Vector3f scale;

    public DisguiseDefinition(Kind kind, Identifier resource, Vector3f scale) {
        this.kind = kind;
        this.resource = resource;
        this.scale = scale;
    }
}