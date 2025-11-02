package seq.sequencermod.net.client.morphs.adapters;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.mob.EndermiteEntity;
import seq.sequencermod.net.client.morphs.BaseAdapter;

public class EndermiteAdapter extends BaseAdapter<EndermiteEntity> {
    @Override
    public void syncFromPlayer(AbstractClientPlayerEntity p, EndermiteEntity e) {
        // Только углы, без скорости
        syncCommon(p, e, true, false);
    }
}