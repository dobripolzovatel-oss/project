package seq.sequencermod.net.client.morphs;

import net.minecraft.block.Blocks; // <-- добавлен импорт для пузырьковой колонны
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import seq.sequencermod.net.client.morphs.runtime.AxolotlMorphRuntime;

/**
 * Адаптер морфа аксолотля.
 */
public final class AxolotlMorphAdapter implements MorphAdapter<Entity> {

    public static final Identifier AXOLOTL_ID = new Identifier("minecraft", "axolotl");

    @Override
    public void onCreate(Entity mirror, java.util.UUID playerId) {
        if (mirror instanceof AxolotlEntity ax) {
            try { ax.setVariant(AxolotlEntity.Variant.LUCY); } catch (Throwable ignored) {}
            mirror.setNoGravity(true);
            mirror.noClip = true;
        }
    }

    @Override
    public void syncFromPlayer(AbstractClientPlayerEntity player, Entity mirror) {
        mirror.setPos(player.getX(), player.getY(), player.getZ());
        try {
            if (mirror instanceof AxolotlEntity ax) {
                if (ax.isBaby()) ax.setBreedingAge(0);
            }
        } catch (Throwable ignored) {}
        mirror.setYaw(player.getBodyYaw());
        mirror.setPitch(player.getPitch());
    }

    @Override
    public void onClientTick(Entity mirror) {
        if (!(mirror.getWorld() instanceof ClientWorld cw)) return;
        if (!(mirror instanceof AxolotlEntity)) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return;

        AbstractClientPlayerEntity owner = mc.world.getPlayers().stream()
                .filter(p -> {
                    int stable = (int) (p.getUuid().getMostSignificantBits() ^ p.getUuid().getLeastSignificantBits());
                    if (stable == 0) stable = 1;
                    return mirror.getId() == stable;
                })
                .findFirst().orElse(null);
        if (owner == null) return;

        AxolotlMorphRuntime rt = AxolotlMorphRuntime.get(owner.getUuid(), true);

        // Tick play dead
        rt.tickPlayDead();

        // Влажность
        boolean wet = isWetLikeAxolotl(cw, mirror);
        rt.tickAir(wet);

        // Применить variant
        if (mirror instanceof AxolotlEntity ax) {
            try {
                AxolotlEntity.Variant[] variants = AxolotlEntity.Variant.values();
                int idx = Math.min(rt.getVariant(), variants.length - 1);
                if (ax.getVariant() != variants[idx]) {
                    ax.setVariant(variants[idx]);
                }
            } catch (Throwable ignored) {}
        }
    }

    @Override
    public void beforeRender(AbstractClientPlayerEntity player, Entity mirror, float tickDelta) {}

    @Override
    public void afterRender(AbstractClientPlayerEntity player, Entity mirror, float tickDelta) {}

    @Override
    public boolean shouldClientTick(Entity mirror) {
        return false;
    }

    private boolean isWetLikeAxolotl(ClientWorld world, Entity e) {
        try {
            if (e.isTouchingWater()) return true;
            if (world.isRaining() && world.isSkyVisible(BlockPos.ofFloored(e.getX(), e.getY() + 1, e.getZ()))) return true;
            // Заменяем приватный Entity.isInsideBubbleColumn() на проверку блока
            BlockPos pos = BlockPos.ofFloored(e.getX(), e.getY(), e.getZ());
            if (world.getBlockState(pos).isOf(Blocks.BUBBLE_COLUMN)) return true;
        } catch (Throwable ignored) {}
        return false;
    }
}