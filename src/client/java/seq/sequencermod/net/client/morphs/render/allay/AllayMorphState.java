package seq.sequencermod.net.client.morphs.render.allay;

import net.minecraft.block.BlockState;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Random;

public class AllayMorphState {

    public int age;
    public float partialTicks = 0.0f;
    public float globalSpeedMul = 1.0f;

    private float limbSpeed = 0.0f;
    private float limbPrevSpeed = 0.0f;
    private float limbPos = 0.0f;

    public float rawSpeedPrev = 0.0f;
    public float rawSpeed = 0.0f;
    public float speedDivisor = 0.30f;

    public float armRaisePrev = 0.0f;
    public float armRaise = 0.0f;

    private int armCounterPrev = 0;
    private int armCounter = 0;

    private float armRaiseTarget = 0.0f;
    private boolean autoArmFromHeldItem = true;

    public boolean dancing = false;
    private int danceTick = 0;
    public boolean spinActive = false;

    public float spinProgressPrev = 0.0f;
    public float spinProgress = 0.0f;

    private int spinCounterPrev = 0;
    private int spinCounter = 0;

    public int jukeboxCheckRadius = 4;

    private final Random rnd = new Random();
    private int ambientCooldown = 0;
    private int lastHurtTime = 0;
    private boolean deathPlayed = false;
    private boolean prevDancing = false;
    private boolean prevSpinActive = false;
    private boolean prevMainHandNotEmpty = false;

    // Registry-based IDs for celebrate sounds (works even if constants are absent)
    private static final Identifier ID_CELEBRATE = new Identifier("minecraft", "entity.allay.celebrate");
    private static final Identifier ID_CELEBRATE_SHORT = new Identifier("minecraft", "entity.allay.celebrate_short");

    public void reset(AbstractClientPlayerEntity player) {
        age = 0;
        partialTicks = 0f;
        globalSpeedMul = 1.0f;

        limbSpeed = limbPrevSpeed = 0f;
        limbPos = 0f;
        rawSpeed = rawSpeedPrev = 0f;

        armCounterPrev = armCounter = 0;
        armRaisePrev = armRaise = 0f;
        armRaiseTarget = 0f;
        autoArmFromHeldItem = true;

        dancing = false;
        danceTick = 0;
        spinActive = false;
        spinCounterPrev = spinCounter = 0;
        spinProgressPrev = spinProgress = 0f;

        ambientCooldown = 0;
        lastHurtTime = 0;
        deathPlayed = false;
        prevDancing = false;
        prevSpinActive = false;
        prevMainHandNotEmpty = player != null && !player.getMainHandStack().isEmpty();
    }

    public void tick(AbstractClientPlayerEntity player) {
        age++;

        double dx = player.getX() - player.prevX;
        double dz = player.getZ() - player.prevZ;
        float posDelta = (float) Math.sqrt(dx * dx + dz * dz);

        float inputSpeed = Math.min(posDelta * 4.0f, 1.0f);
        limbPrevSpeed = limbSpeed;
        limbSpeed += (inputSpeed - limbSpeed) * 0.4f;
        limbPos += limbSpeed;

        rawSpeedPrev = limbPrevSpeed;
        rawSpeed = limbSpeed;

        boolean shouldRaise;
        if (autoArmFromHeldItem) {
            ItemStack main = player.getMainHandStack();
            shouldRaise = !main.isEmpty();
        } else {
            shouldRaise = armRaiseTarget >= 0.5f;
        }

        armCounterPrev = armCounter;
        if (shouldRaise) armCounter = Math.min(5, armCounter + 1);
        else armCounter = Math.max(0, armCounter - 1);

        armRaisePrev = armCounterPrev / 5.0f;
        armRaise = armCounter / 5.0f;

        if (dancing) {
            danceTick++;
        } else {
            danceTick = 0;
        }
        boolean inWindow = (danceTick % 55) < 15;

        spinCounterPrev = spinCounter;
        if (inWindow) {
            spinCounter = Math.min(15, spinCounter + 1);
        } else {
            spinCounter = Math.max(0, spinCounter - 1);
        }
        spinActive = inWindow;

        spinProgressPrev = spinCounterPrev / 15.0f;
        spinProgress = spinCounter / 15.0f;

        boolean hasItem = !player.getMainHandStack().isEmpty() || !player.getOffHandStack().isEmpty();
        if (ambientCooldown > 0) {
            ambientCooldown--;
        } else {
            playAt(player,
                    hasItem ? SoundEvents.ENTITY_ALLAY_AMBIENT_WITH_ITEM
                            : SoundEvents.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM,
                    1.0f, 1.0f);
            ambientCooldown = rnd.nextInt(200) + 200;
        }

        if (player.hurtTime > 0 && lastHurtTime == 0) {
            playAt(player, SoundEvents.ENTITY_ALLAY_HURT, 1.0f, 1.0f);
        }
        lastHurtTime = player.hurtTime;

        if (!deathPlayed && (player.isDead() || player.getHealth() <= 0.0f)) {
            playAt(player, SoundEvents.ENTITY_ALLAY_DEATH, 1.0f, 1.0f);
            deathPlayed = true;
        } else if (deathPlayed && !player.isDead() && player.getHealth() > 0.0f) {
            deathPlayed = false;
        }

        if (!prevDancing && dancing) {
            playAt(player, resolve(ID_CELEBRATE), 1.0f, 1.0f);
        }
        prevDancing = dancing;

        if (!prevSpinActive && spinActive) {
            playAt(player, resolve(ID_CELEBRATE_SHORT), 1.0f, 1.0f);
        }
        prevSpinActive = spinActive;

        boolean mainNowNotEmpty = !player.getMainHandStack().isEmpty();
        if (prevMainHandNotEmpty && !mainNowNotEmpty) {
            playAt(player, SoundEvents.ENTITY_ALLAY_ITEM_GIVEN, 1.0f, 1.0f);
        } else if (!prevMainHandNotEmpty && mainNowNotEmpty) {
            playAt(player, SoundEvents.ENTITY_ALLAY_ITEM_TAKEN, 1.0f, 1.0f);
        }
        prevMainHandNotEmpty = mainNowNotEmpty;
    }

    public void autoDetectDance(AbstractClientPlayerEntity player) {
        World w = player.getWorld();
        if (w == null) { setDancing(false); return; }
        BlockPos base = player.getBlockPos();
        boolean found = false;
        int r = jukeboxCheckRadius;
        outer:
        for (int x = -r; x <= r; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos p = base.add(x, y, z);
                    BlockState st = w.getBlockState(p);
                    if (st.getBlock() instanceof JukeboxBlock && st.get(JukeboxBlock.HAS_RECORD)) {
                        found = true;
                        break outer;
                    }
                }
            }
        }
        setDancing(found);
    }

    public void setDancing(boolean value) { this.dancing = value; }

    public void setArmRaiseTarget(float v) {
        this.armRaiseTarget = MathHelper.clamp(v, 0f, 1.0f);
    }

    public void setAutoArmFromHeldItem(boolean auto) { this.autoArmFromHeldItem = auto; }

    public float getLimbSpeed(float tickDelta) {
        return MathHelper.lerp(MathHelper.clamp(tickDelta, 0.0f, 1.0f), limbPrevSpeed, limbSpeed);
    }

    public float getLimbPos(float tickDelta) {
        float td = MathHelper.clamp(tickDelta, 0.0f, 1.0f);
        return limbPos - limbSpeed * (1.0f - td);
    }

    private static void playAt(AbstractClientPlayerEntity p, SoundEvent snd, float vol, float pitch) {
        if (p == null || p.getWorld() == null) return;
        p.getWorld().playSound(p.getX(), p.getY(), p.getZ(), snd, SoundCategory.NEUTRAL, vol, pitch, false);
    }

    private static SoundEvent resolve(Identifier id) {
        // Returns registered sound or the generic allay ambient as a safe fallback
        SoundEvent se = Registries.SOUND_EVENT.get(id);
        return se != null ? se : SoundEvents.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM;
    }
}