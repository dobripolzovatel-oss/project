package seq.sequencermod.net.client.morphs.adapters;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import seq.sequencermod.net.client.morphs.BaseAdapter;

public class DefaultLivingAdapter extends BaseAdapter<LivingEntity> {
    @Override
    public void syncFromPlayer(AbstractClientPlayerEntity p, LivingEntity e) {
        // Базовый случай: копируем yaw/pitch и скорость (нативные анимации)
        syncCommon(p, e, true, true);
    }

    // ВАЖНО: по умолчанию не тикаем живых на клиенте, чтобы не включать их физику/самодвижение.
    // Для анимаций большинству хватает age++, который мы увеличиваем в SimpleMorphs.
    @Override
    public boolean shouldClientTick(LivingEntity e) {
        return false;
    }
}