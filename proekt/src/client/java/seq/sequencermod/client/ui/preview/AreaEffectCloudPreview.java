package seq.sequencermod.client.ui.preview;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public final class AreaEffectCloudPreview {
    private final MinecraftClient mc;
    private final int centerX;
    private final int centerY;
    private final float baseScale;

    private AreaEffectCloudEntity cloud;
    private boolean enabled = true;

    private float radius = 2.0f;
    private Integer colorArgb = null;
    private ParticleEffect particle = ParticleTypes.ENTITY_EFFECT;

    public AreaEffectCloudPreview(MinecraftClient mc, int centerX, int centerY, float baseScale) {
        this.mc = mc;
        this.centerX = centerX;
        this.centerY = centerY;
        this.baseScale = baseScale;
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setRadius(float radius) { this.radius = Math.max(0.25f, radius); if (cloud != null) cloud.setRadius(this.radius); }
    public void setColor(Integer argb) { this.colorArgb = argb; if (cloud != null && argb != null) cloud.setColor(argb & 0x00FFFFFF); }

    public void setDragonBreath(boolean dragon) {
        this.particle = dragon ? ParticleTypes.DRAGON_BREATH : ParticleTypes.ENTITY_EFFECT;
        if (cloud != null) cloud.setParticleType(this.particle);
    }

    private void ensureCloud() {
        if (mc == null || mc.world == null || mc.player == null) return;
        if (cloud != null) return;
        cloud = new AreaEffectCloudEntity(mc.world, mc.player.getX(), mc.player.getY(), mc.player.getZ());
        cloud.setRadius(radius);
        cloud.setWaitTime(0);
        cloud.setDuration(6000);
        cloud.setRadiusOnUse(0);
        cloud.setRadiusGrowth(0);
        cloud.setParticleType(this.particle);
        if (this.colorArgb != null) cloud.setColor(this.colorArgb & 0x00FFFFFF);
    }

    public void tick() {
        if (!enabled) return;
        ensureCloud();
        if (cloud == null) return;
        try {
            cloud.tick();
            if (cloud.getDuration() < 200) cloud.setDuration(6000);
            float wobble = (MathHelper.sin((mc.world.getTime() % 100) / 100.0f * (float)Math.PI * 2) * 0.1f);
            cloud.setRadius(Math.max(0.25f, this.radius + wobble));
        } catch (Throwable ignored) {}
    }

    public void render(MatrixStack matrices, float tickDelta) {
        if (!enabled) return;
        ensureCloud();
        if (cloud == null) return;
        float yaw = (mc.world != null ? (mc.world.getTime() + tickDelta) : 0f) * 2.0f;
        float pitch = 25.0f;
        float scale = baseScale / Math.max(1.0f, radius + 0.5f);
        GuiEntityRenderer.renderEntityInGui(matrices, centerX, centerY, scale, yaw, pitch, cloud, tickDelta);
    }
}