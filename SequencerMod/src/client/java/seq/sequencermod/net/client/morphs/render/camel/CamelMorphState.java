package seq.sequencermod.net.client.morphs.render.camel;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;

public interface CamelMorphState {
    // lifecycle
    default void reset(AbstractClientPlayerEntity player) {}
    void tick(AbstractClientPlayerEntity player);
    void setPartialTicks(float partial);

    // limb animation inputs
    float getLimbSpeed(float tickDelta); // limbDistance
    float getLimbPos(float tickDelta);   // limbAngle

    // global time for animations
    float age();
    float globalSpeedMul();

    // sitting
    boolean isSitting();
    float getSitProgress(); // 0..1
    void requestToggleSit();

    // dash (long jump)
    boolean isDashing();
    float getDashProgress(); // 0..1 visual
    void requestDashStart();
    void requestDashRelease(AbstractClientPlayerEntity player);
    int getJumpCooldown();

    // TEXTURES (UI + hotkey U)
    Identifier getSkinTexture();
    void setSkinTexture(Identifier id);
    void cycleNextSkin();
    List<Identifier> getAvailableSkins();

    // Model extras visibility (vanilla saddle/bridle/reins toggles)
    boolean isSaddled();
    void setSaddled(boolean value);

    boolean showReins();
    void setShowReins(boolean value);

    // sounds
    void playEatCactus(AbstractClientPlayerEntity player);
}