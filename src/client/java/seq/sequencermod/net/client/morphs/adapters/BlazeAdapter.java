package seq.sequencermod.net.client.morphs.adapters;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.mob.BlazeEntity;
import seq.sequencermod.net.client.morphs.BaseAdapter;

public class BlazeAdapter extends BaseAdapter<BlazeEntity> {
    @Override
    public void syncFromPlayer(AbstractClientPlayerEntity p, BlazeEntity e) {
        // Раньше: setVelocity(true) иногда давал «перетягивание».
        // Сейчас: не навязываем yaw/headYaw и НЕ копируем скорость.
        syncCommon(p, e, false, false);
        try { e.setOnGround(false); } catch (Throwable ignored) {}
    }
}