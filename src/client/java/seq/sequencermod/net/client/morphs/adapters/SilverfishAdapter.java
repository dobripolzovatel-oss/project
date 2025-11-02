package seq.sequencermod.net.client.morphs.adapters;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import seq.sequencermod.net.client.morphs.BaseAdapter;

public class SilverfishAdapter extends BaseAdapter<SilverfishEntity> {
    @Override
    public void syncFromPlayer(AbstractClientPlayerEntity p, SilverfishEntity e) {
        // Не копируем скорость
        syncCommon(p, e, true, false);
    }
}