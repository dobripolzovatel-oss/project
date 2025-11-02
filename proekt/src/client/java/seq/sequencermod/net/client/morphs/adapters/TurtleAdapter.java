package seq.sequencermod.net.client.morphs.adapters;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.passive.TurtleEntity;
import seq.sequencermod.net.client.morphs.BaseAdapter;

public class TurtleAdapter extends BaseAdapter<TurtleEntity> {
    @Override
    public void syncFromPlayer(AbstractClientPlayerEntity p, TurtleEntity e) {
        // Без скорости, чтобы не «тянуло»
        syncCommon(p, e, true, false);
        try { e.setOnGround(false); } catch (Throwable ignored) {}
    }
}