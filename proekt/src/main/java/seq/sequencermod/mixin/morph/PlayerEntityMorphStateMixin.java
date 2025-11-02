package seq.sequencermod.mixin.morph;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import seq.sequencermod.morph.api.MorphAccess;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMorphStateMixin implements MorphAccess {

    @Unique
    private Identifier sequencer$currentMorphTypeId;

    @Override
    public Identifier sequencer$getMorphTypeId() {
        return this.sequencer$currentMorphTypeId;
    }

    @Override
    public void sequencer$setMorphTypeId(Identifier id) {
        this.sequencer$currentMorphTypeId = id;
    }
}