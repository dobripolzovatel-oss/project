package seq.sequencermod.size;

/**
 * width / height - хитбокс.
 * eyeHeight / modelScale == null -> авто-режим.
 * manualEye / manualScale говорят миксинам, можно ли авто-пере计算 (false = пересчитывать).
 */
public class PlayerSizeData {
    public final float width;
    public final float height;
    public final Float eyeHeight;
    public final Float modelScale;
    public final boolean manualEye;
    public final boolean manualScale;

    public PlayerSizeData(float width, float height, Float eyeHeight, Float modelScale) {
        this(width, height, eyeHeight, modelScale,
                eyeHeight != null,
                modelScale != null);
    }

    public PlayerSizeData(float width, float height,
                          Float eyeHeight, Float modelScale,
                          boolean manualEye, boolean manualScale) {
        this.width = width;
        this.height = height;
        this.eyeHeight = eyeHeight;
        this.modelScale = modelScale;
        this.manualEye = manualEye;
        this.manualScale = manualScale;
    }

    public PlayerSizeData withEye(Float eye, boolean manual) {
        return new PlayerSizeData(width, height, eye, modelScale, manual, manualScale);
    }

    public PlayerSizeData withScale(Float scale, boolean manual) {
        return new PlayerSizeData(width, height, eyeHeight, scale, manualEye, manual);
    }

    @Override
    public String toString() {
        return "PlayerSizeData{" +
                "w=" + width +
                ", h=" + height +
                ", eye=" + eyeHeight + (manualEye ? ":manual" : ":auto") +
                ", scale=" + modelScale + (manualScale ? ":manual" : ":auto") +
                '}';
    }
}