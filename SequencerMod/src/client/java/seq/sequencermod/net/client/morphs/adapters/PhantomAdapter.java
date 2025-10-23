package seq.sequencermod.net.client.morphs.adapters;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.mob.PhantomEntity;
import seq.sequencermod.net.client.morphs.BaseAdapter;

public class PhantomAdapter extends BaseAdapter<PhantomEntity> {
    @Override
    public void syncFromPlayer(AbstractClientPlayerEntity p, PhantomEntity e) {
        // Копируем позу/углы с игрока, НО скорость не задаём — фантом не будет «плыть» сам по себе.
        syncCommon(p, e, true, false);
        try { e.setNoGravity(true); } catch (Throwable ignored) {}
        try { e.setOnGround(false); } catch (Throwable ignored) {}
        // При желании можно глушить AI (обычно не требуется, клиентский tick без сервера не «уводит»):
        // try { e.setAiDisabled(true); } catch (Throwable ignored) {}
    }

    @Override
    public boolean shouldClientTick(PhantomEntity e) {
        // ВАЖНО: разрешаем tick() — у фантома крылья/хвост завязаны на его внутреннюю анимацию.
        return true;
    }
}