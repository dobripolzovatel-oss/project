package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import seq.sequencermod.size.util.ReachCalc;

@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererClampReachMixin {

    @Shadow @Final private Camera camera; // добавили @Final, чтобы убрать предупреждение

    @Inject(method = "updateTargetedEntity(F)V", at = @At("TAIL"))
    private void sequencer$clampReach(float tickDelta, CallbackInfo ci) {
        // существующая логика как была…
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        PlayerEntity p = mc.player;
        if (p == null) return;

        HitResult hit = mc.crosshairTarget;
        if (hit == null) return;

        boolean creative = p.getAbilities().creativeMode;
        float allowedBlock = ReachCalc.blockReachFor(p, creative, creative ? 5.0f : 4.5f);
        float allowedEntity = ReachCalc.entityReachFor(p);

        Vec3d camPos = (this.camera != null) ? this.camera.getPos() : p.getCameraPosVec(tickDelta);
        Vec3d look = (this.camera != null)
                ? Vec3d.fromPolar(this.camera.getPitch(), this.camera.getYaw())
                : p.getRotationVec(tickDelta);

        double distSq = camPos.squaredDistanceTo(hit.getPos());

        switch (hit.getType()) {
            case ENTITY -> {
                double maxSq = (double) allowedEntity * (double) allowedEntity;
                if (distSq > maxSq) {
                    mc.crosshairTarget = BlockHitResult.createMissed(
                            camPos, Direction.getFacing(look.x, look.y, look.z), BlockPos.ofFloored(camPos));
                }
            }
            case BLOCK -> {
                double maxSq = (double) allowedBlock * (double) allowedBlock;
                if (distSq > maxSq) {
                    mc.crosshairTarget = BlockHitResult.createMissed(
                            camPos, Direction.getFacing(look.x, look.y, look.z), BlockPos.ofFloored(camPos));
                }
            }
            default -> {}
        }
    }
}