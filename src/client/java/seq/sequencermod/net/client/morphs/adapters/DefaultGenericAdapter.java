package seq.sequencermod.net.client.morphs.adapters;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import seq.sequencermod.net.client.morphs.BaseAdapter;

public class DefaultGenericAdapter extends BaseAdapter<Entity> {
    @Override
    public void syncFromPlayer(AbstractClientPlayerEntity p, Entity e) {
        // Больше не копируем скорость даже для «неживых»
        syncCommon(p, e, true, false);
    }
}