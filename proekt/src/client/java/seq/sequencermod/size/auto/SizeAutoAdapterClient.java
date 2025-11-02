package seq.sequencermod.size.auto;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import seq.sequencermod.size.PlayerClientSizes;
import seq.sequencermod.size.PlayerSizeData;

/**
 * Клиентская часть автоматики. ДОЛЖНА лежать в src/client/java, чтобы не тянуть
 * client-классы в общий код.
 */
@Environment(EnvType.CLIENT)
public final class SizeAutoAdapterClient {
    private SizeAutoAdapterClient(){}

    public static void onSizeChangedClient(ClientPlayerEntity cp) {
        PlayerSizeData d = PlayerClientSizes.get(cp.getUuid());
        if (d == null) {
            SizeDerivedStore.clear(cp.getUuid());
            return;
        }
        SizeAutoAdapter.recomputeLocal(cp, d);
    }
}