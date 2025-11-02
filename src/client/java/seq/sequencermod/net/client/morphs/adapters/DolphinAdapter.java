package seq.sequencermod.net.client.morphs.adapters;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.passive.DolphinEntity;
import seq.sequencermod.net.client.morphs.BaseAdapter;

public class DolphinAdapter extends BaseAdapter<DolphinEntity> {
    @Override
    public void syncFromPlayer(AbstractClientPlayerEntity p, DolphinEntity e) {
        // Вообще без скорости — убираем любые «перетяжки»
        syncCommon(p, e, true, false);
        try { e.setOnGround(false); } catch (Throwable ignored) {}
    }
}