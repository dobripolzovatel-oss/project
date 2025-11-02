package seq.sequencermod.net.client.morphs.adapters;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.passive.BeeEntity;
import seq.sequencermod.net.client.morphs.BaseAdapter;

public class BeeAdapter extends BaseAdapter<BeeEntity> {
    @Override
    public void syncFromPlayer(AbstractClientPlayerEntity p, BeeEntity e) {
        // no-op: не вмешиваемся. Окружение/полет подготовит SimpleMorphsLiving.
    }

    @Override
    public boolean shouldClientTick(BeeEntity e) {
        // Даём ванильной логике тикать и обновлять крылья
        return true;
    }
}