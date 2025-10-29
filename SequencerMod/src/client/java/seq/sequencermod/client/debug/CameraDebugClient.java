package seq.sequencermod.client.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

import static seq.sequencermod.size.config.MicroRenderConfig.DEFAULT_FOV;
import static seq.sequencermod.size.config.MicroRenderConfig.FAR_PLANE_MARGIN;

@Environment(EnvType.CLIENT)
public final class CameraDebugClient {
    private static boolean enabled = false;
    private static KeyBinding toggleKey;

    private CameraDebugClient() {}

    public static void init() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.sequencermod.camdebug",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C,
                "key.categories.sequencermod"
        ));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (toggleKey != null && toggleKey.wasPressed()) {
                long h = MinecraftClient.getInstance().getWindow().getHandle();
                boolean ctrl = InputUtil.isKeyPressed(h, GLFW.GLFW_KEY_LEFT_CONTROL) || InputUtil.isKeyPressed(h, GLFW.GLFW_KEY_RIGHT_CONTROL);
                boolean alt  = InputUtil.isKeyPressed(h, GLFW.GLFW_KEY_LEFT_ALT)     || InputUtil.isKeyPressed(h, GLFW.GLFW_KEY_RIGHT_ALT);
                if (ctrl && alt) {
                    enabled = !enabled;
                    if (mc.player != null) mc.player.sendMessage(Text.literal("[CamDebug] " + (enabled ? "ON" : "OFF")), true);
                }
            }
        });

        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            if (!enabled) return;
            var mc = MinecraftClient.getInstance();
            var cam = ctx.camera();
            var world = mc.world;
            if (mc.player == null || world == null || cam == null) return;

            MatrixStack matrices = ctx.matrixStack();
            VertexConsumerProvider consumers = ctx.consumers(); if (consumers == null) return;

            Vec3d cpos = cam.getPos();
            double camX = cpos.x, camY = cpos.y, camZ = cpos.z;

            matrices.push();
            matrices.translate(-camX, -camY, -camZ);
            VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());

            double half = 0.02;
            WorldRenderer.drawBox(matrices, lines, camX - half, camY - half, camZ - half, camX + half, camY + half, camZ + half, 1, 1, 0, 1);

            BlockPos eyePos = BlockPos.ofFloored(camX, camY, camZ);
            renderTop(world, matrices, consumers, eyePos,       0.8f, 0.8f, 0.8f, 1);
            renderTop(world, matrices, consumers, eyePos.down(), 0.4f, 0.9f, 0.4f, 1);

            matrices.pop();
        });

        HudRenderCallback.EVENT.register((DrawContext context, float tickDelta) -> {
            if (!enabled) return;
            var mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.world == null || mc.gameRenderer == null) return;

            PlayerEntity p = mc.player;
            Vec3d cpos = mc.gameRenderer.getCamera().getPos();
            double camY = cpos.y;
            BlockPos eyePos = BlockPos.ofFloored(cpos.x, cpos.y, cpos.z);
            Double topYCur = topY(mc.world, eyePos);
            Double topYBelow = topY(mc.world, eyePos.down());
            Perspective persp = mc.options.getPerspective();

            // Вычисляем проекционные параметры
            int fbw = Math.max(1, mc.getWindow().getFramebufferWidth());
            int fbh = Math.max(1, mc.getWindow().getFramebufferHeight());
            float aspect = (float) fbw / (float) fbh;
            
            double fov = DEFAULT_FOV;
            try {
                var cam = mc.gameRenderer.getCamera();
                // Пытаемся получить FOV через рефлексию или используем стандартное значение
                fov = mc.options.getFov().getValue().doubleValue();
            } catch (Exception ignored) {}
            
            float viewDist = mc.gameRenderer.getViewDistance();
            float vanillaNear = 0.05f;
            float vanillaFar = Math.max(512f, viewDist + FAR_PLANE_MARGIN);

            float x = 6, y = 6, dy = 10;
            var tr = mc.textRenderer;
            context.drawText(tr, Text.literal(String.format("CamDebug: %s  (%s)", enabled, persp)), (int)x, (int)y, 0xFFFF00, true); y += dy;
            context.drawText(tr, Text.literal(String.format("camY=%.5f  eyeY=%.5f  h=%.5f", camY, p.getStandingEyeHeight(), p.getDimensions(p.getPose()).height)), (int)x, (int)y, 0xFFFFFF, true); y += dy;
            if (topYCur != null)   { context.drawText(tr, Text.literal(String.format("topY(cur)=%.5f  d=%.5f",   topYCur,   camY - topYCur)),   (int)x, (int)y, 0xCCCCFF, true); y += dy; }
            if (topYBelow != null) { context.drawText(tr, Text.literal(String.format("topY(below)=%.5f  d=%.5f", topYBelow, camY - topYBelow)), (int)x, (int)y, 0xCCCCFF, true); y += dy; }
            
            // Проекционные параметры
            context.drawText(tr, Text.literal(String.format("FOV=%.1f°  aspect=%.3f  viewDist=%.1f", fov, aspect, viewDist)), (int)x, (int)y, 0xFFFFAA, true); y += dy;
            context.drawText(tr, Text.literal(String.format("[WORLD] near=%.4f  far=%.1f", vanillaNear, vanillaFar)), (int)x, (int)y, 0xAAFFAA, true); y += dy;
        });
    }

    private static void renderTop(World world, MatrixStack matrices, VertexConsumerProvider consumers, BlockPos pos, float r, float g, float b, float a) {
        BlockState bs = world.getBlockState(pos);
        var shape = bs.getCollisionShape(world, pos);
        if (shape.isEmpty()) return;
        double topLocal = shape.getMax(Direction.Axis.Y);
        double topY = pos.getY() + topLocal;
        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
        WorldRenderer.drawBox(matrices, lines, pos.getX(), topY, pos.getZ(), pos.getX() + 1, topY + 0.002, pos.getZ() + 1, r, g, b, a);
    }
    private static Double topY(World world, BlockPos pos) {
        var shape = world.getBlockState(pos).getCollisionShape(world, pos);
        if (shape.isEmpty()) return null;
        return pos.getY() + shape.getMax(Direction.Axis.Y);
    }
}
