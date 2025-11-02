package seq.sequencermod.morph.api;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public interface MorphAccess {
    @Nullable Identifier sequencer$getMorphTypeId();

    // Новый: сервер (и клиент при синке) должен уметь установить морф
    void sequencer$setMorphTypeId(@Nullable Identifier id);
}