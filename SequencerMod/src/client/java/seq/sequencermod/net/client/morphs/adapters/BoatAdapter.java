package seq.sequencermod.net.client.morphs.adapters;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import seq.sequencermod.net.client.morphs.BaseAdapter;

/**
 * Старый адаптер лодки (использовался для зеркала).
 * Сейчас лодочный морф рендерится через BoatMorphRenderer (render-only),
 * поэтому этот адаптер оставлен для совместимости, но фактически
 * не участвует в рендере, когда активен морф лодки.
 *
 * Можно удалить регистрацию из MorphAdapterRegistry при желании.
 */
public class BoatAdapter extends BaseAdapter<BoatEntity> {
    @Override
    public void syncFromPlayer(AbstractClientPlayerEntity p, BoatEntity e) {
        // Минимальный sync на случай, если где-то всё же попадём в mirror-ветку.
        syncCommon(p, e, true, false);
        try { e.setOnGround(false); } catch (Throwable ignored) {}
    }

    @Override
    public boolean shouldClientTick(BoatEntity e) {
        // mirror лодки нам не нужен — не тикаем
        return false;
    }
}