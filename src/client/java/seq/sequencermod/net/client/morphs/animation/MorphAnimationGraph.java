package seq.sequencermod.net.client.morphs.animation;

import net.minecraft.client.network.AbstractClientPlayerEntity;

public final class MorphAnimationGraph {

    public MorphAnimState pick(AbstractClientPlayerEntity p, boolean canFly, boolean canSwim, float speedHoriz) {
        boolean onGround = p.isOnGround();
        boolean inWater = p.isTouchingWater();
        boolean crouching = p.isSneaking();
        boolean sprint = p.isSprinting();
        boolean flying = (canFly && (p.getAbilities().flying || p.hasVehicle())); // упрощённо

        if (flying) {
            return speedHoriz > 0.02f ? MorphAnimState.FLY : MorphAnimState.IDLE;
        }
        if (canSwim && (p.isSwimming() || inWater)) {
            return MorphAnimState.SWIM;
        }
        if (!onGround && !inWater) {
            return MorphAnimState.FALL;
        }
        if (speedHoriz < 0.01f) {
            return crouching ? MorphAnimState.CROUCH_IDLE : MorphAnimState.IDLE;
        }
        if (crouching) {
            return MorphAnimState.CROUCH_WALK;
        }
        return sprint ? MorphAnimState.RUN : MorphAnimState.WALK;
    }
}