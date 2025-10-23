package seq.sequencermod.mixin.morph;

import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Пустой миксин-заглушка.
 * Раньше тут была логика MorphShapes/MorphHolder, но она больше не используется.
 * Миксин не активирован в mixins.json и не влияет на поведение.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    // no-op
}