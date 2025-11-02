package seq.sequencermod.net.client.morphs.render.camel;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import seq.sequencermod.mixin.accessor.EntityAccessor;
import seq.sequencermod.mixin.accessor.PlayerEntityAccessor;

final class CamelMorphStateImpl implements CamelMorphState {
    // global time
    private float age;
    private float globalSpeedMul = 1.0f;
    private float partial;

    // limb animator inputs
    private float limbSwing, limbSwingPrev;
    private float limbAmount, limbAmountPrev;

    // sitting
    private boolean targetSit = false;
    private float sit, sitPrev; // 0..1
    private static final int SIT_IN_TICKS = 40;   // vanilla
    private static final int STAND_IN_TICKS = 52; // vanilla
    private static final float SIT_SPEED   = 1f / SIT_IN_TICKS;
    private static final float STAND_SPEED = 1f / STAND_IN_TICKS;

    // dash (long jump)
    private boolean charging;     // идёт заряд
    private int chargeTicks;      // 0..30
    private static final int CHARGE_MAX_TICKS = 30; // CamelEntity.field_41764

    private int dashTicks;        // активная фаза для анимации
    private int dashCooldown;     // CamelEntity.field_40132 = 55

    private static final int DASH_ACTIVE_TICKS = 10;
    private static final int DASH_COOLDOWN_TICKS = 55;

    // idle/ambient
    private int idleAnimCooldown;
    private int ambientCooldown;
    private final Random rnd = new Random();

    private boolean deathPlayed = false;

    // step
    private float stepAccum;
    private static final float STEP_STRIDE = 0.55f;

    // hurt edge
    private int lastHurtTime;

    // TEXTURES
    private final List<Identifier> skins = new ArrayList<>();
    private int skinIndex = 0;

    // Vanilla-like AnimationState set
    final AnimationState sittingTransitionAnimationState = new AnimationState();
    final AnimationState sittingAnimationState           = new AnimationState();
    final AnimationState standingTransitionAnimationState= new AnimationState();
    final AnimationState idlingAnimationState            = new AnimationState();
    final AnimationState dashingAnimationState           = new AnimationState();

    // Model extras visibility
    private boolean saddled = false;
    private boolean reins   = false;

    @Override
    public void reset(AbstractClientPlayerEntity player) {
        age = 0f;
        limbSwing = limbSwingPrev = 0f;
        limbAmount = limbAmountPrev = 0f;
        sit = sitPrev = 0f; targetSit = false;

        charging = false; chargeTicks = 0;
        dashTicks = 0; dashCooldown = 0;

        idleAnimCooldown = 0;
        ambientCooldown = rnd.nextInt(200) + 200;
        stepAccum = 0f;
        lastHurtTime = 0;

        // default skins: ванильный camel
        skins.clear();
        skins.add(new Identifier("minecraft", "textures/entity/camel/camel.png"));
        skinIndex = 0;

        saddled = false;
        reins = false;

        deathPlayed = false;

        stopAllAnimStates();
    }

    @Override
    public void playEatCactus(AbstractClientPlayerEntity player) {
        playAt(player, SoundEvents.ENTITY_CAMEL_EAT, 1.0f, 1.0f);
    }

    private void stopAllAnimStates() {
        sittingTransitionAnimationState.stop();
        sittingAnimationState.stop();
        standingTransitionAnimationState.stop();
        idlingAnimationState.stop();
        dashingAnimationState.stop();
    }



    @Override
    public void tick(AbstractClientPlayerEntity p) {
        age += 1.0f;

        // Звук смерти: один раз при 0 HP, сбрасывается после оживления
        if (!deathPlayed && (p.isDead() || p.getHealth() <= 0.0f)) {
            playAt(p, SoundEvents.ENTITY_CAMEL_DEATH, 1.0f, 1.0f);
            deathPlayed = true;
        } else if (deathPlayed && p.getHealth() > 0.0f && !p.isDead()) {
            // Игрок ожил/возродился — позволим снова проигрывать в будущем
            deathPlayed = false;
        }

        // limbDistance and limbAngle
        double vx = p.getVelocity().x;
        double vz = p.getVelocity().z;
        float horizSpeed = (float)Math.sqrt(vx * vx + vz * vz);

        float speedForAnim = Math.min(horizSpeed * 4.0f, 1.0f);
        limbAmountPrev = limbAmount;
        limbAmount += (speedForAnim - limbAmount) * 0.3f;

        limbSwingPrev = limbSwing;
        float phaseMul = p.isOnGround() ? 1.0f : 0.6f;
        limbSwing += speedForAnim * phaseMul;

        // idle anim trigger
        if (idleAnimCooldown <= 0) {
            idleAnimCooldown = rnd.nextInt(40) + 80;
            idlingAnimationState.start((int)age);
        } else {
            idleAnimCooldown--;
        }

        // ambient (не сидит, не заряжается)
        if (!isSitting() && !charging) {
            if (ambientCooldown <= 0) {
                playAt(p, SoundEvents.ENTITY_CAMEL_AMBIENT, 1.0f, 1.0f);
                ambientCooldown = rnd.nextInt(400) + 400;
            } else {
                ambientCooldown--;
            }
        } else {
            ambientCooldown = Math.max(ambientCooldown, 40);
        }

        // шаги
        if (p.isOnGround() && !isSitting()) {
            if (horizSpeed > 0.01f) {
                stepAccum += horizSpeed;
                if (stepAccum >= STEP_STRIDE) {
                    stepAccum -= STEP_STRIDE;
                    BlockPos under = p.getBlockPos().down();
                    BlockState st = p.getWorld().getBlockState(under);
                    BlockSoundGroup grp = st.getSoundGroup();
                    if (grp == BlockSoundGroup.SAND) {
                        playAt(p, SoundEvents.ENTITY_CAMEL_STEP_SAND, 1.0f, 1.0f);
                    } else {
                        playAt(p, SoundEvents.ENTITY_CAMEL_STEP, 1.0f, 1.0f);
                    }
                }
            } else {
                stepAccum *= 0.8f;
                if (stepAccum < 0.01f) stepAccum = 0f;
            }
        } else {
            stepAccum = 0f;
        }

        // charge progress
        if (charging) {
            if (dashCooldown == 0 && !isSitting()) {
                chargeTicks = Math.min(CHARGE_MAX_TICKS, chargeTicks + 1);
            } else {
                charging = false;
                chargeTicks = 0;
            }
        }

        // dash visual and cooldown + "dash ready"
        int prevCd = dashCooldown;
        if (dashCooldown > 0) dashCooldown--;
        if (prevCd > 0 && dashCooldown == 0) {
            playAt(p, SoundEvents.ENTITY_CAMEL_DASH_READY, 1.0f, 1.0f);
        }

        if (dashTicks > 0) dashTicks--;
        dashingAnimationState.setRunning(isDashing(), (int)age);

        // sitting transitions
        sitPrev = sit;
        if (targetSit) {
            if (sit < 1f) {
                sittingTransitionAnimationState.startIfNotRunning((int)age);
                sit = Math.min(1f, sit + SIT_SPEED);
            } else {
                sittingTransitionAnimationState.stop();
                sittingAnimationState.startIfNotRunning((int)age);
            }
            standingTransitionAnimationState.stop();
        } else {
            if (sit > 0f) {
                standingTransitionAnimationState.startIfNotRunning((int)age);
                sittingTransitionAnimationState.stop();
                sittingAnimationState.stop();
                sit = Math.max(0f, sit - STAND_SPEED);
            } else {
                standingTransitionAnimationState.stop();
                sittingTransitionAnimationState.stop();
                sittingAnimationState.stop();
            }
        }

        // hurt звук по фронту
        if (p.hurtTime > 0 && lastHurtTime == 0) {
            playAt(p, SoundEvents.ENTITY_CAMEL_HURT, 1.0f, 1.0f);
        }
        lastHurtTime = p.hurtTime;
    }

    private static void playAt(AbstractClientPlayerEntity p, SoundEvent snd, float vol, float pitch) {
        if (p == null || p.getWorld() == null) return;
        p.getWorld().playSound(p.getX(), p.getY(), p.getZ(), snd, SoundCategory.NEUTRAL, vol, pitch, false);
    }

    @Override
    public void setPartialTicks(float partial) {
        this.partial = MathHelper.clamp(partial, 0f, 1f);
    }

    @Override
    public float getLimbSpeed(float tickDelta) {
        return MathHelper.lerp(tickDelta, limbAmountPrev, limbAmount);
    }

    @Override
    public float getLimbPos(float tickDelta) {
        return MathHelper.lerp(tickDelta, limbSwingPrev, limbSwing);
    }

    @Override public float age() { return age; }
    @Override public float globalSpeedMul() { return globalSpeedMul; }

    @Override public boolean isSitting() { return targetSit && sit >= 0.999f; }
    @Override public float getSitProgress() { return MathHelper.lerp(partial, sitPrev, sit); }

    @Override
    public void requestToggleSit() {
        targetSit = !targetSit;
        // В момент посадки отменяем заряд
        if (targetSit) {
            charging = false;
            chargeTicks = 0;

            playAt(lastPlayerRef, SoundEvents.ENTITY_CAMEL_SIT, 1.0f, 1.0f);
            sittingTransitionAnimationState.start((int)age);
            standingTransitionAnimationState.stop();
        } else {
            playAt(lastPlayerRef, SoundEvents.ENTITY_CAMEL_STAND, 1.0f, 1.0f);
            standingTransitionAnimationState.start((int)age);
            sittingTransitionAnimationState.stop();
            sittingAnimationState.stop();
        }
    }

    private AbstractClientPlayerEntity lastPlayerRef;

    @Override
    public void requestDashStart() {
        if (dashCooldown != 0) return;
        if (isSitting()) return;
        charging = true;
        chargeTicks = 0;
    }

    @Override
    public void requestDashRelease(AbstractClientPlayerEntity player) {
        if (!charging) return;
        charging = false;

        lastPlayerRef = player;

        // strength 0..1
        float strength = MathHelper.clamp(chargeTicks / (float)CHARGE_MAX_TICKS, 0.0f, 1.0f);
        chargeTicks = 0;
        if (strength <= 0.01f) return;

        // Vanilla physics factors
        double moveSpeed = player.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        float velMul = ((PlayerEntityAccessor)(Object)player).sequencer$invokeGetVelocityMultiplier();
        float jumpVelMult = ((EntityAccessor)(Object)player).sequencer$invokeGetJumpVelocityMultiplier();

        float yawRad = player.getYaw() * ((float)Math.PI / 180f);
        double dirX = -MathHelper.sin(yawRad);
        double dirZ =  MathHelper.cos(yawRad);
        double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (len > 0) { dirX /= len; dirZ /= len; }

        double forward = (22.2222f * strength) * moveSpeed * velMul;
        double jumpD = (0.42f * jumpVelMult) + player.getJumpBoostVelocityModifier();
        double up = (1.4285f * strength) * jumpD;

        Vec3d add = new Vec3d(dirX * forward, up, dirZ * forward);
        Vec3d newVel = player.getVelocity().add(add);
        player.setVelocity(newVel);
        try { player.velocityDirty = true; } catch (Throwable ignored) {}

        playAt(player, SoundEvents.ENTITY_CAMEL_DASH, 1.0f, 1.0f);

        dashTicks = DASH_ACTIVE_TICKS;
        dashCooldown = DASH_COOLDOWN_TICKS;
        dashingAnimationState.start((int)age);
    }

    @Override public boolean isDashing() { return dashTicks > 0; }

    @Override
    public float getDashProgress() {
        if (dashTicks <= 0) return 0f;
        return 1f - (dashTicks / (float)DASH_ACTIVE_TICKS);
    }

    @Override
    public int getJumpCooldown() {
        return dashCooldown;
    }

    // TEXTURES

    @Override
    public Identifier getSkinTexture() {
        if (skins.isEmpty()) {
            return new Identifier("minecraft", "textures/entity/camel/camel.png");
        }
        return skins.get(MathHelper.clamp(skinIndex, 0, skins.size() - 1));
    }

    @Override
    public void setSkinTexture(Identifier id) {
        if (id == null) return;
        if (!skins.contains(id)) {
            // добавим, только если реально существует
            if (resourceExists(id)) {
                skins.add(id);
                skinIndex = skins.size() - 1;
            }
        } else {
            skinIndex = skins.indexOf(id);
        }
    }

    @Override
    public void cycleNextSkin() {
        if (skins.isEmpty()) return;
        skinIndex = (skinIndex + 1) % skins.size();
    }

    @Override
    public List<Identifier> getAvailableSkins() {
        return Collections.unmodifiableList(skins);
    }

    private static boolean resourceExists(Identifier tex) {
        try {
            ResourceManager rm = MinecraftClient.getInstance().getResourceManager();
            // RenderLayer ожидает путь вида ".../camel.png"
            Resource res = rm.getResource(tex).orElse(null);
            return res != null;
        } catch (Throwable ignored) { return false; }
    }

    // Visibility

    @Override public boolean isSaddled() { return saddled; }
    @Override public void setSaddled(boolean value) { this.saddled = value; }

    @Override public boolean showReins() { return reins; }
    @Override public void setShowReins(boolean value) { this.reins = value; }
}