package seq.sequencermod.net.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import seq.sequencermod.net.client.morph.MorphEngine;

/**
 * Без изменений по логике для аксолотля (ему не требуется).
 * Оставлено здесь для целостности.
 */
final class SimpleMorphsProjectiles {
    private SimpleMorphsProjectiles() {}

    static void stabilizeProjectile(AbstractClientPlayerEntity p, Entity e) {
        if (e instanceof PersistentProjectileEntity || e instanceof TridentEntity) {
            return;
        }
        try { e.setNoGravity(true); } catch (Throwable ignored) {}
        try { e.setOnGround(false); } catch (Throwable ignored) {}
        try { e.setVelocity(0, 0, 0); } catch (Throwable ignored) {}

        try { e.setYaw(p.bodyYaw); } catch (Throwable ignored) {}
        try { e.setPitch(p.getPitch()); } catch (Throwable ignored) {}
        MorphEngine.trySetField(e, "prevYaw", p.prevYaw);
        MorphEngine.trySetField(e, "prevPitch", p.prevPitch);
    }
}