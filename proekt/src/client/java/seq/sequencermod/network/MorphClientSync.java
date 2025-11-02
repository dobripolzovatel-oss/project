package seq.sequencermod.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * DEPRECATED: используйте seq.sequencermod.net.client.MorphClientSync.
 * Класс оставлен для обратной совместимости и делегирует вызовы в новый пакет.
 */
@Deprecated
@Environment(EnvType.CLIENT)
public final class MorphClientSync {
    private MorphClientSync() {}

    public static void bootstrap() {
        seq.sequencermod.net.client.MorphClientSync.bootstrap();
    }

    public static boolean shouldHidePlayerModel(UUID playerUuid, boolean isLocalPlayer, boolean isFirstPerson) {
        return seq.sequencermod.net.client.MorphClientSync.shouldHidePlayerModel(playerUuid, isLocalPlayer, isFirstPerson);
    }

    public static Identifier getMorphType(UUID playerUuid) {
        return seq.sequencermod.net.client.MorphClientSync.getMorphType(playerUuid);
    }
}