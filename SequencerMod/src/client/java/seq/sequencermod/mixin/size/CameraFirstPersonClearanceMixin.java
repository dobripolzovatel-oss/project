package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import seq.sequencermod.size.config.MicroRenderConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Мини‑клиренс камеры в первом лице.
 * Если геометрия слишком близко спереди, отодвигаем камеру назад
 * вдоль взгляда на микродистанцию, чтобы near не «резал» блок.
 */
@Environment(EnvType.CLIENT)
@Mixin(Camera.class)
public abstract class CameraFirstPersonClearanceMixin {

    @Shadow private float yaw;
    @Shadow private float pitch;

    @Inject(
            method = "update(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;ZZF)V",
            at = @At("TAIL")
    )
    private void sequencer$ensureFirstPersonClearance(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (MicroRenderConfig.DEBUG_VANILLA_CAMERA) return;

        if (thirdPerson) return;
        if (!(focusedEntity instanceof PlayerEntity p)) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc.world;
        if (world == null) return;

        double sizeH = p.getBoundingBox().getYLength();
        if (sizeH >= 0.06) return; // только для tiny/микро

        // Целевой безопасный зазор для near
        final double TARGET_CLEAR = 1.2e-4; // 0.00012 блока
        final double MAX_BACK_STEP = 0.02;  // не дальше 2 см
        final double PROBE = 0.6;           // длина луча вперёд

        Camera cam = (Camera) (Object) this;
        Vec3d eye = cam.getPos();
        Vec3d dir = Vec3d.fromPolar(this.pitch, this.yaw).normalize();

        Vec3d start = eye;
        Vec3d end = start.add(dir.multiply(PROBE));
        HitResult hit = world.raycast(new RaycastContext(
                start, end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                p
        ));

        double front = PROBE;
        if (hit != null && hit.getType() != HitResult.Type.MISS) {
            front = Math.max(0.0, hit.getPos().distanceTo(eye));
        }

        if (front < TARGET_CLEAR) {
            double delta = Math.min(TARGET_CLEAR - front, MAX_BACK_STEP);
            Vec3d newPos = eye.subtract(dir.multiply(delta));
            ((CameraPosAccessor) cam).setPos(newPos);
        }
    }
}