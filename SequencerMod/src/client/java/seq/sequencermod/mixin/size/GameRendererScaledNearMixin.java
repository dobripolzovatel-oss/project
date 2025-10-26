package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.render.RenderPassFlags;
import seq.sequencermod.size.util.SizeCalc;

@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererScaledNearMixin {

    @Shadow private Camera camera;

    private static float minNearForScale(float s) {
        if (s < 1.0e-5f) return 0.0010f;
        if (s < 1.0e-4f) return 0.0015f;
        if (s < 5.0e-4f) return 0.0020f;
        if (s < 1.0e-3f) return 0.0025f;
        if (s < 1.0e-2f) return 0.0045f;
        if (s < 0.10f)    return 0.0080f;
        return 0.0100f;
    }

    // УБРАНО: limitFarForNear — больше не урезаем far, оставляем ванильный view distance.

    @Inject(method = "getBasicProjectionMatrix(D)Lorg/joml/Matrix4f;", at = @At("HEAD"), cancellable = true)
    private void sequencer$scaledNearAndFov(double fovDegOriginal, CallbackInfoReturnable<Matrix4f> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) return;

        final float vanillaNear = 0.05f;

        // Масштаб игрока
        float s = 1.0f;
        PlayerEntity p = mc.player;
        if (p != null) {
            float h = Math.max(SizeCalc.EPS, p.getDimensions(p.getPose()).height);
            s = Math.max(0.00001f, h / 1.8f);
        }

        Perspective persp = mc.options != null ? mc.options.getPerspective() : Perspective.FIRST_PERSON;
        boolean firstPerson = persp != null && persp.isFirstPerson();

    float near = vanillaNear;
    float far  = ((GameRenderer)(Object)this).getViewDistance();

        // Мягкая FOV‑компенсация только для мира (не для руки)
        double fovDeg = fovDegOriginal;
        if (firstPerson && !RenderPassFlags.isHand()) {
            double mult = 1.0 - 0.18 * (1.0 - Math.sqrt(s));
            if (mult < 0.82) mult = 0.82;
            fovDeg = fovDegOriginal * mult;
        }

        if (p != null) {
            // Базовый near от масштаба
            float candidate = vanillaNear * s;
            float minNear = minNearForScale(s);
            near = Math.max(minNear, Math.min(vanillaNear, candidate));

            // Контакт‑адаптация near только если камера действительно ВНУТРИ коллизионного shape твердого блока.
            Vec3d camPos = (this.camera != null ? this.camera.getPos() : p.getEyePos());
            BlockPos bp = BlockPos.ofFloored(camPos);
            BlockState st = p.getWorld().getBlockState(bp);

            // Игнорируем воздух и блоки без коллизии (трава/листья без collision).
            if (!st.isAir()) {
                VoxelShape shape = st.getCollisionShape(p.getWorld(), bp, ShapeContext.absent());
                if (!shape.isEmpty()) {
                    // Границы объединённого shape в мировых координатах
                    var bb = shape.getBoundingBox();
                    double minX = bp.getX() + bb.minX;
                    double minY = bp.getY() + bb.minY;
                    double minZ = bp.getZ() + bb.minZ;
                    double maxX = bp.getX() + bb.maxX;
                    double maxY = bp.getY() + bb.maxY;
                    double maxZ = bp.getZ() + bb.maxZ;

                    // Небольшой eps, чтобы не триггериться "над гранью"
                    final double eps = 1.0e-5;

                    boolean insideX = camPos.x > minX + eps && camPos.x < maxX - eps;
                    boolean insideY = camPos.y > minY + eps && camPos.y < maxY - eps;
                    boolean insideZ = camPos.z > minZ + eps && camPos.z < maxZ - eps;

                    if (insideX && insideY && insideZ) {
                        double dx = Math.min(camPos.x - minX, maxX - camPos.x);
                        double dy = Math.min(camPos.y - minY, maxY - camPos.y);
                        double dz = Math.min(camPos.z - minZ, maxZ - camPos.z);
                        double minEdge = Math.min(dx, Math.min(dy, dz));

                        // Поднимаем near мягко только при реальном "внутри"
                        if (minEdge < 0.00010) {
                            near = Math.max(near, firstPerson ? 0.050f : 0.055f);
                        } else if (minEdge < 0.00030) {
                            near = Math.max(near, firstPerson ? 0.040f : 0.045f);
                        } else if (minEdge < 0.00080) {
                            near = Math.max(near, firstPerson ? 0.030f : 0.035f);
                        } else if (minEdge < 0.00150) {
                            near = Math.max(near, firstPerson ? 0.020f : 0.025f);
                        } else if (minEdge < 0.00300) {
                            near = Math.max(near, firstPerson ? 0.0150f : 0.0180f);
                        } else if (minEdge < 0.00450) {
                            near = Math.max(near, firstPerson ? 0.0125f : 0.0150f);
                        }
                    }
                }
            }
        }

        // Слишком маленький near резко портит глубину: удерживаем разумное соотношение far/near.
    final float maxDepthRatio = 120000.0f; // ≈24x ванильного 5120 — достаточно для микро-игроков без полос на небе
    final float minFarClamp   = 64.0f;     // нижняя граница дальности (4 чанка)
        if (near > 0.0f) {
            float ratio = far / near;
            if (ratio > maxDepthRatio) {
                float clampedFar = near * maxDepthRatio;
                if (clampedFar < minFarClamp) clampedFar = minFarClamp;
                if (clampedFar < far) {
                    far = clampedFar;
                }
            }
        }

    // Матрица проекции (far может быть мягко урезан для стабильной глубины)
        int fbw = mc.getWindow().getFramebufferWidth();
        int fbh = mc.getWindow().getFramebufferHeight();
        float aspect = (fbw > 0 && fbh > 0) ? (float) fbw / (float) fbh : 1.0f;

        float fovRad = (float) (fovDeg * Math.PI / 180.0);
        Matrix4f proj = new Matrix4f().setPerspective(fovRad, aspect, near, far);
        cir.setReturnValue(proj);
    }
}