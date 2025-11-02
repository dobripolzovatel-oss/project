package seq.sequencermod.net.client.morphs.adapters;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.mob.VexEntity;
import seq.sequencermod.net.client.morphs.BaseAdapter;

public class VexAdapter extends BaseAdapter<VexEntity> {
    @Override
    public void syncFromPlayer(AbstractClientPlayerEntity p, VexEntity e) {
        // Без скорости
        syncCommon(p, e, true, false);
        try { e.setOnGround(false); } catch (Throwable ignored) {}
    }
}