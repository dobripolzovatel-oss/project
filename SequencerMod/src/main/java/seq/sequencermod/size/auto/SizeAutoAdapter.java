package seq.sequencermod.size.auto;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import seq.sequencermod.size.PlayerSizeData;
import seq.sequencermod.size.PlayerSizeServerStore;

/**
 * Точки входа автоматики (общая часть). Вызывай после /setsize и /clearsize на сервере.
 * Без client-импортов — безопасно для main-источника.
 */
public final class SizeAutoAdapter {
    private SizeAutoAdapter() {}

    // Server: пересчитать и положить в стор (и при желании отправить клиенту)
    public static void onSizeChangedServer(ServerPlayerEntity sp) {
        PlayerSizeData d = PlayerSizeServerStore.get(sp.getUuid());
        if (d == null) {
            SizeDerivedStore.clear(sp.getUuid());
            return;
        }
        SizeDerived derived = SizeRules.compute(sp, d);
        SizeDerivedStore.set(sp.getUuid(), derived);
    }

    // Универсальный локальный пересчёт (пригодится и на клиенте, и в тестах)
    public static void recomputeLocal(PlayerEntity p, PlayerSizeData d) {
        if (d == null) {
            SizeDerivedStore.clear(p.getUuid());
            return;
        }
        SizeDerived derived = SizeRules.compute(p, d);
        SizeDerivedStore.set(p.getUuid(), derived);
    }
}