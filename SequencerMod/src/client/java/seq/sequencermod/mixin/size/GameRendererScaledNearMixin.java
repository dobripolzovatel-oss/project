package seq.sequencermod.mixin.size;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import seq.sequencermod.render.RenderPassFlags;
import seq.sequencermod.size.util.SizeCalc;

/**
 * Ультра-микро адаптация near/far:
 * - сверхмалый near в обычном кадре (вплоть до ~0.0010..0.0015),
 * - при прямом касании грани на 1 кадр поднимаем near до 0.035..0.050,
 * - far жёстко ограничиваем в контакте для устойчивой глубины.
 * FOV-компенсация по-прежнему НЕ касается рендера руки (hand pass).
 */
@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererScaledNearMixin {

    @Shadow private Camera camera;

    private static float minNearForScale(float s) {
        // Уточнённый минимум near под экстремальные масштабы
        if (s < 1.0e-5f) return 0.0010f; // ultra-ultra-tiny
        if (s < 1.0e-4f) return 0.0015f; // ultra-tiny
        if (s < 5.0e-4f) return 0.0020f;
        if (s < 1.0e-3f) return 0.0025f; // micro
        if (s < 1.0e-2f) return 0.0045f;
        if (s < 0.10f)    return 0.0080f;
        return 0.0100f;
    }

    private static float limitFarForNear(float far, float near) {
        // Чем выше near в контакте — тем меньше допускаем far на кадр
        if (near >= 0.0500f) return Math.min(far, 48.0f);
        if (near >= 0.0350f) return Math.min(far, 64.0f);
        if (near >= 0.0250f) return Math.min(far, 96.0f);
        if (near >= 0.0180f) return Math.min(far, 128.0f);
        if (near <  0.0030f) return Math.min(far, 96.0f);   // очень малый near — тоже поджимаем
        if (near <  0.0045f) return Math.min(far, 128.0f);
        if (near <  0.0085f) return Math.min(far, 768.0f);
        if (near <  0.0120f) return Math.min(far, 1024.0f);
        return far;
    }

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

        // Мягкая FOV-компенсация только для мира (не для руки)
        double fovDeg = fovDegOriginal;
        if (firstPerson && !RenderPassFlags.isHand()) {
            double mult = 1.0 - 0.18 * (1.0 - Math.sqrt(s)); // 1..~0.82
            if (mult < 0.82) mult = 0.82;
            fovDeg = fovDegOriginal * mult;
        }

        if (p != null) {
            // Базовый near от масштаба
            float candidate = vanillaNear * s;
            float minNear = minNearForScale(s);
            near = Math.max(minNear, Math.min(vanillaNear, candidate));

            // Контакт-адаптация near (позиция КАМЕРЫ)
            Vec3d camPos = (this.camera != null ? this.camera.getPos() : p.getEyePos());
            BlockPos bp = BlockPos.ofFloored(camPos);
            BlockState st = p.getWorld().getBlockState(bp);

            if (!st.isAir()) {
                double fx = camPos.x - bp.getX();
                double fy = camPos.y - bp.getY();
                double fz = camPos.z - bp.getZ();
                double dx = Math.min(fx, 1.0 - fx);
                double dy = Math.min(fy, 1.0 - fy);
                double dz = Math.min(fz, 1.0 - fz);
                double minEdge = Math.min(dx, Math.min(dy, dz));

                // Для экстремально маленьких масштабов поднимаем near сильнее в момент касания
                if (minEdge < 0.00010) {                          // практически в грани
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

            // Поддерживаем устойчивую глубину
            far = limitFarForNear(far, near);
        }

        // Матрица проекции
        int fbw = mc.getWindow().getFramebufferWidth();
        int fbh = mc.getWindow().getFramebufferHeight();
        float aspect = (fbw > 0 && fbh > 0) ? (float) fbw / (float) fbh : 1.0f;

        float fovRad = (float) (fovDeg * Math.PI / 180.0);
        Matrix4f proj = new Matrix4f().setPerspective(fovRad, aspect, near, far);
        cir.setReturnValue(proj);
    }
}