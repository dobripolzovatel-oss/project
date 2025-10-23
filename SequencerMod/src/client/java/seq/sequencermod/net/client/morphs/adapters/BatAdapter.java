package seq.sequencermod.net.client.morphs.adapters;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.passive.BatEntity;
import seq.sequencermod.net.client.morphs.BaseAdapter;

public class BatAdapter extends BaseAdapter<BatEntity> {
    @Override
    public void syncFromPlayer(AbstractClientPlayerEntity p, BatEntity e) {
        // no-op: не вмешиваемся. Окружение/полет подготовит SimpleMorphsLiving.
    }

    @Override
    public boolean shouldClientTick(BatEntity e) {
        // Даём ванильной логике тикать и обновлять крылья
        return true;
    }
}