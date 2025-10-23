package seq.sequencermod.net.client.morphs.adapters;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import seq.sequencermod.net.client.morphs.BaseAdapter;

public class FishAdapter extends BaseAdapter<LivingEntity> {
    @Override
    public void syncFromPlayer(AbstractClientPlayerEntity p, LivingEntity e) {
        // Вообще без скорости
        syncCommon(p, e, true, false);
        try { e.setOnGround(false); } catch (Throwable ignored) {}
    }
}