package seq.sequencermod.net.client.morphs;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;

// Импорт адаптеров
import seq.sequencermod.net.client.morphs.adapters.*;
// Аксолотль: используем новый адаптер

import java.util.HashMap;
import java.util.Map;

public final class MorphAdapterRegistry {
    private MorphAdapterRegistry() {}

    private static final Map<EntityType<?>, MorphAdapter<?>> REG = new HashMap<>();
    private static final MorphAdapter<?> DEFAULT_LIVING = new DefaultLivingAdapter();
    private static final MorphAdapter<?> DEFAULT_GENERIC = new DefaultGenericAdapter();

    private static final boolean RENDER_ONLY_ALLAY = true;

    public static void init() {
        if (!REG.isEmpty()) return;

        REG.put(EntityType.BLAZE, new BlazeAdapter());
        // Пчела и летучая мышь идут через дефолтный адаптер, чтобы не было дополнительных твиков
        // REG.put(EntityType.BEE, new BeeAdapter());
        // REG.put(EntityType.BAT, new BatAdapter());

        if (!RENDER_ONLY_ALLAY) {
            // REG.put(EntityType.ALLAY, new AllayAdapter());
        }
        REG.put(EntityType.VEX, new VexAdapter());
        REG.put(EntityType.ARMOR_STAND, new ArmorStandAdapter());
        REG.put(EntityType.DOLPHIN, new DolphinAdapter());
        REG.put(EntityType.SALMON, new FishAdapter());
        REG.put(EntityType.TROPICAL_FISH, new FishAdapter());
        REG.put(EntityType.ENDERMITE, new EndermiteAdapter());
        REG.put(EntityType.SILVERFISH, new SilverfishAdapter());
        REG.put(EntityType.TURTLE, new TurtleAdapter());
        REG.put(EntityType.AXOLOTL, new AxolotlMorphAdapter());
        REG.put(EntityType.BOAT, new BoatAdapter());
        REG.put(EntityType.CHEST_BOAT, new BoatAdapter());
        REG.put(EntityType.EVOKER_FANGS, new EvokerFangsAdapter());
        REG.put(EntityType.ARROW, new ProjectileAdapter());
        REG.put(EntityType.SPECTRAL_ARROW, new ProjectileAdapter());
        REG.put(EntityType.TRIDENT, new ProjectileAdapter());
        REG.put(EntityType.WITHER_SKULL, new ProjectileAdapter());
        REG.put(EntityType.SHULKER_BULLET, new ProjectileAdapter());
        REG.put(EntityType.LLAMA_SPIT, new ProjectileAdapter());
        REG.put(EntityType.PHANTOM, new PhantomAdapter());

        // === CAMEL ===
        // REG.put(EntityType.CAMEL, new CamelMorphAdapter());
    }

    @SuppressWarnings("unchecked")
    public static <E extends Entity> MorphAdapter<E> get(EntityType<?> type) {
        MorphAdapter<?> a = REG.get(type);
        if (a != null) return (MorphAdapter<E>) a;

        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.world != null) {
                Entity probe = type.create(mc.world);
                if (probe instanceof LivingEntity) {
                    return (MorphAdapter<E>) DEFAULT_LIVING;
                }
            }
        } catch (Throwable ignored) {}

        return (MorphAdapter<E>) DEFAULT_GENERIC;
    }
}