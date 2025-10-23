package seq.sequencermod.net.client.morphs.runtime;

import java.util.UUID;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import seq.sequencermod.net.client.morphs.MorphCapabilities;
import seq.sequencermod.net.client.morphs.animation.LimbAnimatorCompat;
import seq.sequencermod.net.client.morphs.animation.MorphAnimState;
import seq.sequencermod.net.client.morphs.animation.MorphAnimationGraph;
import seq.sequencermod.net.client.morphs.motion.FlyMoveController;
import seq.sequencermod.net.client.morphs.motion.GroundMoveController;

/**
 * Пер-игроковое «физическое» и «анимационное» состояние морфа.
 * Не двигает серверного игрока, но даёт рендерам 1:1 yaw/pitch/limb/roll/state.
 */
public final class MorphMotionRuntime {
    public final UUID playerId;
    public MorphCapabilities caps;

    private final FlyMoveController fly = new FlyMoveController();
    private final GroundMoveController ground = new GroundMoveController();
    private final LimbAnimatorCompat limbs = new LimbAnimatorCompat();
    private final MorphAnimationGraph graph = new MorphAnimationGraph();

    private float renderYaw, renderPitch, renderRoll;
    private MorphAnimState state = MorphAnimState.IDLE;
    private float speedHoriz;

    public MorphMotionRuntime(UUID playerId, MorphCapabilities caps) {
        this.playerId = playerId;
        this.caps = caps;
    }

    public void setCaps(MorphCapabilities caps) {
        this.caps = caps;
    }

    public void clientTick(AbstractClientPlayerEntity p) {
        Vec3d v = p.getVelocity();
        speedHoriz = (float)Math.sqrt(v.x * v.x + v.z * v.z);

        boolean useFly = caps.canFly && (p.getAbilities().flying || p.hasVehicle());
        if (useFly) {
            fly.tick(v, p.getYaw(), p.getPitch(), caps.turnRateDeg);
            renderYaw = fly.getRenderYaw();
            renderPitch = fly.getRenderPitch();
            renderRoll = fly.getRenderRoll();
            // в воздухе — вносим меньший вклад в шаг
            limbs.tick(speedHoriz, true);
        } else {
            ground.tick(v, p.getYaw(), p.getPitch(), caps.turnRateDeg);
            renderYaw = ground.getRenderYaw();
            renderPitch = ground.getRenderPitch();
            renderRoll = 0.0f;
            limbs.tick(speedHoriz, !p.isOnGround());
        }

        state = graph.pick(p, caps.canFly, caps.canSwim, speedHoriz);
    }

    // Accessors для рендеров
    public float getRenderYaw() { return renderYaw; }
    public float getRenderPitch() { return renderPitch; }
    public float getRenderRoll() { return renderRoll; }
    public MorphAnimState getState() { return state; }
    public float getLimbSwing() { return limbs.getLimbSwing(); }
    public float getLimbSwingAmount(float tickDelta) { return limbs.getLimbSwingAmount(tickDelta); }
    public float getSpeedHoriz() { return speedHoriz; }
}