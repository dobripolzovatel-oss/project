package seq.sequencermod.net.morph;

import net.minecraft.util.Identifier;
import seq.sequencermod.core.SequencerMod;

public final class MorphNet {
    public static final Identifier C2S_REQUEST_MORPH = new Identifier(SequencerMod.MOD_ID, "morph_request");
    public static final Identifier S2C_MORPH_SYNC = new Identifier(SequencerMod.MOD_ID, "morph_sync");
    private MorphNet() {}
}