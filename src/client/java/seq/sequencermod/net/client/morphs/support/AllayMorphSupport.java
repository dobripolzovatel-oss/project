package seq.sequencermod.net.client.morphs.support;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import seq.sequencermod.net.client.MorphClientSync;

public final class AllayMorphSupport {
    private AllayMorphSupport() {}

    // Звук "короткого" празднования (через реестр — совместимо с разными маппингами)
    private static final Identifier ID_CELEBRATE_SHORT = new Identifier("minecraft", "entity.allay.celebrate_short");

    public static void init() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!world.isClient) return ActionResult.PASS;
            if (hit == null) return ActionResult.PASS;
            if (!world.getBlockState(hit.getBlockPos()).isOf(Blocks.NOTE_BLOCK)) return ActionResult.PASS;

            Identifier type = MorphClientSync.getMorphType(player.getUuid());
            if (type == null || !"minecraft".equals(type.getNamespace()) || !"allay".equals(type.getPath())) {
                return ActionResult.PASS;
            }

            SoundEvent se = resolve(ID_CELEBRATE_SHORT);
            world.playSound(player.getX(), player.getY(), player.getZ(), se, SoundCategory.NEUTRAL, 1.0f, 1.0f, false);

            // Не блокируем ванильную логику нотного блока
            return ActionResult.PASS;
        });
    }

    private static SoundEvent resolve(Identifier id) {
        SoundEvent se = Registries.SOUND_EVENT.get(id);
        // Фолбэк на любой доступный звук Аллея, если конкретного нет в версии
        return se != null ? se : Registries.SOUND_EVENT.get(new Identifier("minecraft", "entity.allay.ambient_without_item"));
    }
}