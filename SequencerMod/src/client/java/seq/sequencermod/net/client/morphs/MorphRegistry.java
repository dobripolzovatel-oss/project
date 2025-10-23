package seq.sequencermod.net.client.morphs;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import seq.sequencermod.net.client.MorphClientSync;
import seq.sequencermod.net.client.SimpleMorphs; // ADDED: delegate camel state to SimpleMorphs
import seq.sequencermod.net.client.morphs.render.blaze.BlazeMorphRenderer;
import seq.sequencermod.net.client.morphs.render.phantom.PhantomMorphRenderer;
import seq.sequencermod.net.client.morphs.render.axolotl.AxolotlMorphRenderer;
import seq.sequencermod.net.client.morphs.render.boat.BoatMorphRenderer;
import seq.sequencermod.net.client.morphs.render.allay.AllayMorphRenderer;
import seq.sequencermod.net.client.morphs.runtime.BoatMorphRuntime;
import seq.sequencermod.net.client.morphs.runtime.MorphMotionRuntime;

// Camel
import seq.sequencermod.net.client.morphs.render.camel.CamelMorphRenderer;
import seq.sequencermod.net.client.morphs.render.camel.CamelMorphState;

public final class MorphRegistry {

    public interface Factory extends Function<AbstractClientPlayerEntity, ClientMorphRenderer> {}

    private static final Map<Identifier, Factory> FACTORIES = new HashMap<>();
    private static final Map<Identifier, MorphCapabilities> CAPS = new HashMap<>();
    private static final Map<UUID, Active> ACTIVE = new HashMap<>();

    private record Active(Identifier type, ClientMorphRenderer renderer, MorphMotionRuntime motion) {}

    public static void register(Identifier id, MorphCapabilities caps, Factory factory) {
        CAPS.put(id, caps);
        FACTORIES.put(id, factory);
    }

    public static void bootstrapBuiltins() {
        // PHANTOM
        register(new Identifier("minecraft","phantom"), MorphCapabilities.phantom(), player -> {
            EntityModelLoader loader = MinecraftClient.getInstance().getEntityModelLoader();
            PhantomMorphRenderer r = new PhantomMorphRenderer(loader);
            r.reset(player);
            return (p, td, ms, bufs, light) -> r.render(p, td, ms, bufs, light);
        });

        // BLAZE
        register(new Identifier("minecraft","blaze"), MorphCapabilities.blaze(), player -> {
            EntityModelLoader loader = MinecraftClient.getInstance().getEntityModelLoader();
            BlazeMorphRenderer r = new BlazeMorphRenderer(loader);
            r.reset(player);
            return new ClientMorphRenderer() {
                @Override public void tick(AbstractClientPlayerEntity p) { r.tick(p); }
                @Override public void render(AbstractClientPlayerEntity p, float td, MatrixStack ms, VertexConsumerProvider bufs, int light) {
                    r.render(p, td, ms, bufs, light);
                }
            };
        });

        // AXOLOTL
        register(new Identifier("minecraft","axolotl"), MorphCapabilities.axolotl(), player -> {
            var part = MinecraftClient.getInstance().getEntityModelLoader().getModelPart(AxolotlMorphRenderer.LAYER);
            AxolotlMorphRenderer r = new AxolotlMorphRenderer(part);
            return (p, td, ms, bufs, light) -> r.render(p, td, ms, bufs, light);
        });

        // CAMEL (pass EntityModelLoader and wrap into ClientMorphRenderer)
        register(new Identifier("minecraft","camel"), MorphCapabilities.camel(), player -> {
            EntityModelLoader loader = MinecraftClient.getInstance().getEntityModelLoader();
            CamelMorphRenderer r = new CamelMorphRenderer(loader);
            return new ClientMorphRenderer() {
                @Override public void tick(AbstractClientPlayerEntity p) { r.tick(p); }
                @Override public void render(AbstractClientPlayerEntity p, float td, MatrixStack ms, VertexConsumerProvider bufs, int light) {
                    r.render(p, td, ms, bufs, light);
                }
                @Override public void renderShadow(AbstractClientPlayerEntity p, MatrixStack ms, VertexConsumerProvider bufs, float td) {
                    try { r.renderShadow(p, ms, bufs, td); } catch (Throwable ignored) {}
                }
            };
        });

        // BOAT
        register(new Identifier("minecraft","boat"), MorphCapabilities.boat(), player -> {
            BoatMorphRenderer r = new BoatMorphRenderer();
            return new ClientMorphRenderer() {
                @Override public void tick(AbstractClientPlayerEntity p) { BoatMorphRuntime.get(p.getUuid()).clientTick(p); }
                @Override public void render(AbstractClientPlayerEntity p, float td, MatrixStack ms, VertexConsumerProvider bufs, int light) {
                    r.render(p, BoatMorphRuntime.get(p.getUuid()), BoatEntity.Type.OAK, false, td, ms, bufs, light);
                }
            };
        });
        register(new Identifier("minecraft","chest_boat"), MorphCapabilities.boat(), player -> {
            BoatMorphRenderer r = new BoatMorphRenderer();
            return new ClientMorphRenderer() {
                @Override public void tick(AbstractClientPlayerEntity p) { BoatMorphRuntime.get(p.getUuid()).clientTick(p); }
                @Override public void render(AbstractClientPlayerEntity p, float td, MatrixStack ms, VertexConsumerProvider bufs, int light) {
                    r.render(p, BoatMorphRuntime.get(p.getUuid()), BoatEntity.Type.OAK, true, td, ms, bufs, light);
                }
            };
        });

        // ALLAY
        register(new Identifier("minecraft","allay"), MorphCapabilities.allay(), player -> {
            EntityModelLoader loader = MinecraftClient.getInstance().getEntityModelLoader();
            AllayMorphRenderer r = new AllayMorphRenderer(loader);
            r.reset(player);
            return new ClientMorphRenderer() {
                @Override public void tick(AbstractClientPlayerEntity p) { r.tick(p); }
                @Override public void render(AbstractClientPlayerEntity p, float td, MatrixStack ms, VertexConsumerProvider bufs, int light) {
                    r.render(p, td, ms, bufs, light);
                }
            };
        });
    }

    public static ClientMorphRenderer getOrCreate(AbstractClientPlayerEntity player, Identifier type) {
        Active a = ACTIVE.get(player.getUuid());
        if (a != null && a.type.equals(type)) {
            return a.renderer;
        }
        var caps = CAPS.getOrDefault(type, new MorphCapabilities(false,false,false,0.1f,0,0,10,1));
        var factory = FACTORIES.get(type);
        ClientMorphRenderer renderer = factory != null
                ? factory.apply(player)
                : (p,td,ms,bufs,light) -> {};
        var motion = new MorphMotionRuntime(player.getUuid(), caps);
        ACTIVE.put(player.getUuid(), new Active(type, renderer, motion));
        return renderer;
    }

    public static MorphMotionRuntime getMotion(AbstractClientPlayerEntity player) {
        Active a = ACTIVE.get(player.getUuid());
        if (a == null) return null;
        return a.motion;
    }

    public static void updateMotionCaps(AbstractClientPlayerEntity player, Identifier type) {
        Active a = ACTIVE.get(player.getUuid());
        if (a == null) return;
        MorphCapabilities caps = CAPS.get(type);
        if (caps != null) a.motion.setCaps(caps);
    }

    public static CamelMorphState getCamelState(AbstractClientPlayerEntity p) {
        // Delegate to SimpleMorphs to avoid depending on removed CamelMorphRuntime
        return SimpleMorphs.getCamelState(p);
    }

    // Вызывать ровно 1 раз за клиентский тик
    public static void clientTick(MinecraftClient mc) {
        if (mc == null || mc.world == null) return;
        for (var p : mc.world.getPlayers()) {
            Identifier type = MorphClientSync.getMorphType(p.getUuid());
            if (type == null) continue;
            var renderer = getOrCreate(p, type); // гарантирует motion
            var motion = getMotion(p);
            try {
                if (motion != null) motion.clientTick(p);
            } catch (Throwable ignored) {}
            try {
                renderer.tick(p);
            } catch (Throwable ignored) {}
        }
    }

    public static void clearFor(AbstractClientPlayerEntity player) {
        ACTIVE.remove(player.getUuid());
    }

    public static void clearAll() {
        ACTIVE.clear();
    }
}