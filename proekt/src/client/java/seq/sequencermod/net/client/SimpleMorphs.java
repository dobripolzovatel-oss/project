package seq.sequencermod.net.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import seq.sequencermod.net.client.morphs.render.phantom.PhantomMorphRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.*;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.EvokerFangsEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import seq.sequencermod.net.client.morphs.MorphAdapter;
import seq.sequencermod.net.client.morphs.MorphAdapterRegistry;
import seq.sequencermod.net.client.morphs.MorphRuntimeFlags;
import seq.sequencermod.net.client.morphs.render.allay.AllayMorphRenderer;
import seq.sequencermod.net.client.morphs.render.axolotl.AxolotlMorphRenderer;
import seq.sequencermod.net.client.morphs.render.blaze.BlazeMorphRenderer;
import seq.sequencermod.net.client.morphs.render.boat.BoatMorphRenderer;
import seq.sequencermod.net.client.morphs.render.camel.CamelMorphRenderer;
import seq.sequencermod.net.client.morphs.render.camel.CamelMorphState;
import seq.sequencermod.net.client.morphs.runtime.BoatMorphRuntime;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

@Environment(EnvType.CLIENT)
public final class SimpleMorphs {
    private SimpleMorphs() {}

    private static boolean inited = false;

    private static final Map<UUID, Entity> MIRRORS = new HashMap<>();
    private static final Map<Entity, Long> LAST_TICK = new WeakHashMap<>();

    private static final Map<UUID, PhantomMorphRenderer> PHANTOM_RENDERERS = new HashMap<>();
    private static final Map<UUID, AllayMorphRenderer>   ALLAY_RENDERERS   = new HashMap<>();
    private static final Map<UUID, AxolotlMorphRenderer> AXOLOTL_RENDERERS = new HashMap<>();
    private static final Map<UUID, BlazeMorphRenderer>   BLAZE_RENDERERS   = new HashMap<>();
    private static final Map<UUID, BoatMorphRenderer>    BOAT_RENDERERS    = new HashMap<>();
    private static final Map<UUID, CamelMorphRenderer>   CAMEL_RENDERERS   = new HashMap<>();

    public static void bootstrap() {
        if (inited) return;
        inited = true;
        MorphAdapterRegistry.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientWorld world = client.world;
            if (world == null) {
                MIRRORS.clear();
                LAST_TICK.clear();
                PHANTOM_RENDERERS.clear();
                ALLAY_RENDERERS.clear();
                AXOLOTL_RENDERERS.clear();
                BLAZE_RENDERERS.clear();
                BOAT_RENDERERS.clear();
                CAMEL_RENDERERS.clear();
                return;
            }

            Map<UUID, AbstractClientPlayerEntity> playersById = new HashMap<>();
            for (AbstractClientPlayerEntity p : world.getPlayers()) playersById.put(p.getUuid(), p);

            Set<UUID> active = new HashSet<>();

            for (AbstractClientPlayerEntity p : world.getPlayers()) {
                Identifier typeId = MorphClientSync.getMorphType(p.getUuid());

                if (is(typeId, "boat") || is(typeId, "chest_boat")) {
                    BoatMorphRuntime rt = BoatMorphRuntime.get(p.getUuid());
                    rt.setChest("chest_boat".equals(typeId.getPath()));
                    rt.clientTick(p);
                    ensureBoatRenderer(p);
                    MIRRORS.remove(p.getUuid());
                    active.add(p.getUuid());
                    continue;
                }

                if (is(typeId, "allay")) {
                    try { ensureAllayRenderer(p).tick(p); } catch (Throwable ignored) {}
                    MIRRORS.remove(p.getUuid());
                    MorphRuntimeFlags.setAllayMorph(p, true);
                    active.add(p.getUuid());
                    continue;
                } else {
                    MorphRuntimeFlags.setAllayMorph(p, false);
                }

                if (is(typeId, "phantom")) {
                    try { ensurePhantomRenderer(p); } catch (Throwable ignored) {}
                    MIRRORS.remove(p.getUuid());
                    active.add(p.getUuid());
                    continue;
                }

                if (is(typeId, "blaze")) {
                    try { ensureBlazeRenderer(p).tick(p); } catch (Throwable ignored) {}
                    MIRRORS.remove(p.getUuid());
                    active.add(p.getUuid());
                    continue;
                }

                if (is(typeId, "axolotl")) {
                    active.add(p.getUuid());
                    continue;
                }

                // Camel — строго render-only: держим свой рендерер, без реальной CamelEntity и без пассажирства
                if (is(typeId, "camel")) {
                    try { ensureCamelRenderer(p).tick(p); } catch (Throwable ignored) {}
                    MIRRORS.remove(p.getUuid());
                    active.add(p.getUuid());
                    continue;
                }

                if (typeId == null) {
                    MIRRORS.remove(p.getUuid());
                    continue;
                }
                active.add(p.getUuid());
            }

            MIRRORS.keySet().removeIf(uuid -> !active.contains(uuid));

            cleanupRendererMap(BOAT_RENDERERS, playersById, "boat", "chest_boat");
            cleanupRendererMap(ALLAY_RENDERERS, playersById, "allay");
            cleanupRendererMap(AXOLOTL_RENDERERS, playersById, "axolotl");
            cleanupRendererMap(BLAZE_RENDERERS, playersById, "blaze");
            cleanupRendererMap(PHANTOM_RENDERERS, playersById, "phantom");
            cleanupRendererMap(CAMEL_RENDERERS, playersById, "camel");

            for (Map.Entry<UUID, Entity> entry : MIRRORS.entrySet()) {
                UUID who = entry.getKey();
                Entity mirror = entry.getValue();
                AbstractClientPlayerEntity p = playersById.get(who);
                if (mirror == null || p == null) continue;

                MorphAdapter<Entity> adapter = MorphAdapterRegistry.get(mirror.getType());
                try { adapter.syncFromPlayer(p, mirror); } catch (Throwable ignored) {}
                try { adapter.onClientTick(mirror); } catch (Throwable ignored) {}

                boolean doTick = false;
                try { doTick = adapter.shouldClientTick(mirror); } catch (Throwable ignored) {}

                if (doTick) {
                    try { mirror.tick(); } catch (Throwable ignored) {}
                } else {
                    try { mirror.age++; } catch (Throwable ignored) {}
                }

                if (mirror instanceof LivingEntity le) {
                    if (!(le instanceof ArmorStandEntity)) {
                        String typeKey = String.valueOf(Registries.ENTITY_TYPE.getId(le.getType()));
                        if (!typeKey.endsWith(":camel")) {
                            SimpleMorphsLiving.stabilizeLiving(p, le, world);
                        }
                    }
                } else if (mirror instanceof PersistentProjectileEntity
                        || mirror.getType() == EntityType.TRIDENT
                        || mirror.getType() == EntityType.WITHER_SKULL
                        || mirror.getType() == EntityType.SHULKER_BULLET
                        || mirror.getType() == EntityType.LLAMA_SPIT) {
                    SimpleMorphsProjectiles.stabilizeProjectile(p, mirror);
                } else if (mirror instanceof EvokerFangsEntity f) {
                    SimpleMorphsSpecial.keepFangsVisible(f);
                }

                try { adapter.afterClientTick(mirror); } catch (Throwable ignored) {}
            }
        });
    }

    private static boolean is(Identifier id, String path) {
        return id != null && "minecraft".equals(id.getNamespace()) && path.equals(id.getPath());
    }

    private static void cleanupRendererMap(Map<UUID, ?> map,
                                           Map<UUID, AbstractClientPlayerEntity> playersById,
                                           String... validPaths) {
        Set<String> val = new HashSet<>(Arrays.asList(validPaths));
        map.entrySet().removeIf(e -> {
            AbstractClientPlayerEntity p = playersById.get(e.getKey());
            Identifier t = (p == null) ? null : MorphClientSync.getMorphType(p.getUuid());
            return p == null || t == null || !"minecraft".equals(t.getNamespace()) || !val.contains(t.getPath());
        });
    }

    public static void renderForPlayer(AbstractClientPlayerEntity player,
                                       Identifier typeId,
                                       float tickDelta,
                                       MatrixStack matrices,
                                       VertexConsumerProvider buffers,
                                       int light) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc.world;
        if (world == null) return;

        if (is(typeId, "boat") || is(typeId, "chest_boat")) {
            BoatMorphRuntime rt = BoatMorphRuntime.get(player.getUuid());
            BoatMorphRenderer r = ensureBoatRenderer(player);
            r.render(player, rt, BoatEntity.Type.OAK, rt.isChest(), tickDelta, matrices, buffers, light);
            return;
        }
        if (is(typeId, "phantom")) {
            try {
                PhantomMorphRenderer r = ensurePhantomRenderer(player);
                r.tick(player);
                r.render(player, tickDelta, matrices, buffers, light);
            } catch (Throwable ignored) {}
            return;
        }
        if (is(typeId, "allay")) {
            try {
                AllayMorphRenderer r = ensureAllayRenderer(player);
                r.render(player, tickDelta, matrices, buffers, light);
                r.renderShadow(player, matrices, buffers, tickDelta);
            } catch (Throwable ignored) {}
            return;
        }
        if (is(typeId, "blaze")) {
            try {
                BlazeMorphRenderer r = ensureBlazeRenderer(player);
                r.render(player, tickDelta, matrices, buffers, light);
                r.renderShadow(player, matrices, buffers, tickDelta);
            } catch (Throwable ignored) {}
            return;
        }
        if (is(typeId, "camel")) {
            try {
                CamelMorphRenderer r = ensureCamelRenderer(player);
                r.render(player, tickDelta, matrices, buffers, light);
            } catch (Throwable ignored) {}
            return;
        }
        if (is(typeId, "axolotl")) {
            Entity mirror = ensureMirror(world, player.getUuid(), typeId);
            if (mirror == null) return;
            MorphAdapter<Entity> adapter = MorphAdapterRegistry.get(mirror.getType());
            try { adapter.syncFromPlayer(player, mirror); } catch (Throwable ignored) {}
            try { adapter.onClientTick(mirror); } catch (Throwable ignored) {}
            try { adapter.beforeRender(player, mirror, tickDelta); } catch (Throwable ignored) {}

            AxolotlMorphRenderer r = ensureAxolotlRenderer(player);

            boolean ok = tryInvokeMethod(
                    r, "render",
                    new Class<?>[]{AbstractClientPlayerEntity.class, float.class, MatrixStack.class, VertexConsumerProvider.class, int.class},
                    player, tickDelta, matrices, buffers, light
            );
            if (!ok) {
                float yawNow = MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw);
                tryInvokeMethod(
                        r, "render",
                        new Class<?>[]{AbstractClientPlayerEntity.class, float.class, float.class, MatrixStack.class, VertexConsumerProvider.class, int.class},
                        player, yawNow, tickDelta, matrices, buffers, light
                );
            }

            try { adapter.afterRender(player, mirror, tickDelta); } catch (Throwable ignored) {}
            return;
        }

        // Generic
        Entity mirror = ensureMirror(world, player.getUuid(), typeId);
        if (mirror == null) return;

        MorphAdapter<Entity> adapter = MorphAdapterRegistry.get(mirror.getType());
        try { adapter.syncFromPlayer(player, mirror); } catch (Throwable ignored) {}

        boolean isArmorStand = mirror instanceof ArmorStandEntity;
        if (!isArmorStand) {
            try { mirror.setPitch(MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch())); } catch (Throwable ignored) {}
        }
        float yawNow = MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw);
        if (isArmorStand) {
            try { mirror.setYaw(yawNow); } catch (Throwable ignored) {}
            trySetField(mirror, "prevYaw", yawNow);
            if (mirror instanceof LivingEntity le) {
                trySetField(le, "bodyYaw", yawNow);
                trySetField(le, "prevBodyYaw", yawNow);
                try { le.setHeadYaw(yawNow); } catch (Throwable ignored) {}
                trySetField(le, "prevHeadYaw", yawNow);
            }
            try { mirror.setPitch(0f); } catch (Throwable ignored) {}
            trySetField(mirror, "prevPitch", 0f);
            trySetField(mirror, "prevX", mirror.getX());
            trySetField(mirror, "prevY", mirror.getY());
            trySetField(mirror, "prevZ", mirror.getZ());
        }

        try { adapter.beforeRender(player, mirror, tickDelta); } catch (Throwable ignored) {}
        float renderYaw = isArmorStand ? yawNow
                : MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw);
        mc.getEntityRenderDispatcher().render(mirror, 0, 0, 0, renderYaw, tickDelta, matrices, buffers, light);
        try { adapter.afterRender(player, mirror, tickDelta); } catch (Throwable ignored) {}
    }

    // Публичный доступ к состоянию верблюда для хоткеев
    public static CamelMorphState getCamelState(AbstractClientPlayerEntity player) {
        CamelMorphRenderer r = ensureCamelRenderer(player);
        return r != null ? r.getState() : null;
    }

    /* ---------- Renderer creators ---------- */

    private static BoatMorphRenderer ensureBoatRenderer(AbstractClientPlayerEntity player) {
        BoatMorphRenderer r = BOAT_RENDERERS.get(player.getUuid());
        if (r == null) {
            r = new BoatMorphRenderer();
            BOAT_RENDERERS.put(player.getUuid(), r);
        }
        return r;
    }

    private static PhantomMorphRenderer ensurePhantomRenderer(AbstractClientPlayerEntity player) {
        PhantomMorphRenderer r = PHANTOM_RENDERERS.get(player.getUuid());
        if (r == null) {
            EntityModelLoader loader = MinecraftClient.getInstance().getEntityModelLoader();
            r = new PhantomMorphRenderer(loader);
            r.reset(player);
            PHANTOM_RENDERERS.put(player.getUuid(), r);
        }
        return r;
    }

    private static AllayMorphRenderer ensureAllayRenderer(AbstractClientPlayerEntity player) {
        AllayMorphRenderer r = ALLAY_RENDERERS.get(player.getUuid());
        if (r == null) {
            EntityModelLoader loader = MinecraftClient.getInstance().getEntityModelLoader();
            r = new AllayMorphRenderer(loader);
            r.reset(player);
            ALLAY_RENDERERS.put(player.getUuid(), r);
        }
        return r;
    }

    private static BlazeMorphRenderer ensureBlazeRenderer(AbstractClientPlayerEntity player) {
        BlazeMorphRenderer r = BLAZE_RENDERERS.get(player.getUuid());
        if (r == null) {
            EntityModelLoader loader = MinecraftClient.getInstance().getEntityModelLoader();
            r = new BlazeMorphRenderer(loader);
            r.reset(player);
            BLAZE_RENDERERS.put(player.getUuid(), r);
        }
        return r;
    }

    private static CamelMorphRenderer ensureCamelRenderer(AbstractClientPlayerEntity player) {
        CamelMorphRenderer r = CAMEL_RENDERERS.get(player.getUuid());
        if (r == null) {
            EntityModelLoader loader = MinecraftClient.getInstance().getEntityModelLoader();
            r = new CamelMorphRenderer(loader); // конструктор-адаптер добавлен в CamelMorphRenderer
            CAMEL_RENDERERS.put(player.getUuid(), r);
        }
        return r;
    }

    private static AxolotlMorphRenderer ensureAxolotlRenderer(AbstractClientPlayerEntity player) {
        AxolotlMorphRenderer r = AXOLOTL_RENDERERS.get(player.getUuid());
        if (r == null) {
            EntityModelLoader loader = MinecraftClient.getInstance().getEntityModelLoader();
            ModelPart root = loader.getModelPart(seq.sequencermod.net.client.morphs.render.axolotl.AxolotlMorphRenderer.LAYER);
            r = new AxolotlMorphRenderer(root);
            AXOLOTL_RENDERERS.put(player.getUuid(), r);
        }
        return r;
    }

    /* ---------- Mirror + reflection helpers ---------- */

    private static Entity ensureMirror(ClientWorld world, UUID playerId, Identifier typeId) {
        Entity current = MIRRORS.get(playerId);
        EntityType<?> desiredType = Registries.ENTITY_TYPE.get(typeId);
        if (desiredType == null) return null;

        if (current == null || current.getType() != desiredType || current.getWorld() != world) {
            Entity created = desiredType.create(world);
            if (created == null) return null;

            created.noClip = true;
            try { created.setNoGravity(true); } catch (Throwable ignored) {}
            try { created.setInvisible(false); } catch (Throwable ignored) {}
            try { created.setSilent(true); } catch (Throwable ignored) {}

            if (created instanceof LivingEntity le) {
                try { if (le.getHealth() <= 0f) le.setHealth(Math.max(1f, le.getMaxHealth())); } catch (Throwable ignored) {}
            }

            assignStableId(created, playerId);
            ensureRenderableBasics(String.valueOf(Registries.ENTITY_TYPE.getId(created.getType())), created);

            MorphAdapter<Entity> adapter = MorphAdapterRegistry.get(created.getType());
            try { adapter.onCreate(created, playerId); } catch (Throwable ignored) {}

            MIRRORS.put(playerId, created);
            current = created;
        }
        return current;
    }

    private static void ensureRenderableBasics(String entityId, Entity e) {
        try { e.setNoGravity(true); } catch (Throwable ignored) {}
        try { e.setInvisible(false); } catch (Throwable ignored) {}

        if (e instanceof ThrownItemEntity thrown) {
            ItemStack st = null;
            try { st = thrown.getStack(); } catch (Throwable ignored) {}
            if (st == null || st.isEmpty()) {
                ItemStack def = defaultItemForThrown(entityId);
                tryInvokeSetItem(thrown, def);
                trySetThrownItemViaNbt(thrown, def);
            }
        }
        if (e instanceof PersistentProjectileEntity proj) {
            tryInvokeMethod(proj, "setPickupType",
                    new Class<?>[]{PersistentProjectileEntity.PickupPermission.class},
                    PersistentProjectileEntity.PickupPermission.DISALLOWED);
        }
        if (e instanceof BoatEntity boat) {
            try { if (boat.getVariant() == null) boat.setVariant(BoatEntity.Type.OAK); } catch (Throwable ignored) {}
        }
        if (e instanceof EndCrystalEntity ec) {
            try { ec.setShowBottom(true); } catch (Throwable ignored) {}
        }
        if (e instanceof AreaEffectCloudEntity aec) {
            try {
                if (aec.getDuration() < 1_000_000) aec.setDuration(2_000_000);
                if (aec.getWaitTime() != 0) aec.setWaitTime(0);
                if (aec.getRadius() <= 0.1f) aec.setRadius(1.5f);
            } catch (Throwable ignored) {}
        }
        if (e instanceof DisplayEntity display) {
            ensureItemDisplayStack(display, new ItemStack(Items.DIAMOND));
        } else if (e.getType() == EntityType.TEXT_DISPLAY && e instanceof DisplayEntity.TextDisplayEntity td) {
            tryInvokeMethod(td, "setText", new Class<?>[]{Text.class}, Text.literal("TextDisplay (morph)"));
        }
    }

    private static ItemStack defaultItemForThrown(String entityId) {
        if (entityId == null) return new ItemStack(Items.SNOWBALL);
        if (entityId.endsWith("ender_pearl")) return new ItemStack(Items.ENDER_PEARL);
        if (entityId.endsWith("snowball")) return new ItemStack(Items.SNOWBALL);
        if (entityId.endsWith("egg")) return new ItemStack(Items.EGG);
        if (entityId.endsWith("experience_bottle")) return new ItemStack(Items.EXPERIENCE_BOTTLE);
        if (entityId.endsWith("potion") || entityId.endsWith("splash_potion")) return new ItemStack(Items.SPLASH_POTION);
        if (entityId.endsWith("lingering_potion")) return new ItemStack(Items.LINGERING_POTION);
        return new ItemStack(Items.SNOWBALL);
    }

    private static void tryInvokeSetItem(ThrownItemEntity target, ItemStack stack) {
        if (target == null) return;
        try {
            Method m = ThrownItemEntity.class.getDeclaredMethod("setItem", ItemStack.class);
            m.setAccessible(true);
            m.invoke(target, stack);
            return;
        } catch (Throwable ignored) {}
        trySetField(target, "item", stack);
        trySetField(target, "stack", stack);
    }

    private static void trySetThrownItemViaNbt(ThrownItemEntity e, ItemStack stack) {
        try {
            NbtCompound root = new NbtCompound();
            NbtCompound item = new NbtCompound();
            stack.writeNbt(item);
            root.put("Item", item);
            e.readNbt(root);
        } catch (Throwable ignored) {}
    }

    private static boolean tryInvokeMethod(Object target, String name, Class<?>[] paramTypes, Object... args) {
        if (target == null || name == null) return false;
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                Method m = c.getDeclaredMethod(name, paramTypes);
                m.setAccessible(true);
                m.invoke(target, args);
                return true;
            } catch (NoSuchMethodException ignored) {
            } catch (Throwable t) { return false; }
            c = c.getSuperclass();
        }
        return false;
    }

    private static void trySetField(Object target, String field, Object value) {
        if (target == null || field == null) return;
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(field);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (Throwable ignored) {}
            c = c.getSuperclass();
        }
    }

    private static void ensureItemDisplayStack(DisplayEntity e, ItemStack stack) {
        boolean ok = tryInvokeMethod(e, "setItemStack", new Class<?>[]{ItemStack.class}, stack);
        if (ok) return;
        try {
            NbtCompound root = new NbtCompound();
            NbtCompound item = new NbtCompound();
            stack.writeNbt(item);
            root.put("item", item);
            e.readNbt(root);
        } catch (Throwable ignored) {
            try {
                NbtCompound root2 = new NbtCompound();
                NbtCompound item2 = new NbtCompound();
                stack.writeNbt(item2);
                root2.put("Item", item2);
                e.readNbt(root2);
            } catch (Throwable ignored2) {}
        }
    }

    private static void assignStableId(Entity e, UUID playerId) {
        int stable = (int) (playerId.getMostSignificantBits() ^ playerId.getLeastSignificantBits());
        if (stable == 0) stable = 1;
        trySetField(e, "id", stable);
    }
}