package seq.sequencermod.net.client.morphs.runtime;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import seq.sequencermod.net.client.morphs.render.camel.CamelMorphState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deprecated shim: больше не используется.
 * Оставлен для временной совместимости, ничего не делает.
 */
@Deprecated
public final class CamelRenderRuntimeBridge {
    private static final Map<UUID, CamelRenderRuntimeBridge> BY = new ConcurrentHashMap<>();

    public static CamelRenderRuntimeBridge get(UUID id) {
        return BY.computeIfAbsent(id, CamelRenderRuntimeBridge::new);
    }

    public static void remove(UUID id) { BY.remove(id); }
    public static void clearAll() { BY.clear(); }

    // Ранее указывалось состояние из CamelMorphRuntime; теперь всегда null.
    public final CamelMorphState state;
    private final UUID id;

    private CamelRenderRuntimeBridge(UUID id) {
        this.id = id;
        this.state = null; // заглушка
    }

    public void tick(AbstractClientPlayerEntity player) {
        // no-op
    }

    // Раньше мост рисовал, теперь рендер выполняется CamelMorphRenderer — оставляем no-op.
    public void render(AbstractClientPlayerEntity player,
                       float tickDelta,
                       MatrixStack matrices,
                       VertexConsumerProvider buffers,
                       int light) {
        // no-op
    }
}