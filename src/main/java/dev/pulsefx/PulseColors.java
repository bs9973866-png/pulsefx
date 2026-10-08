package dev.pulsefx;

import net.minecraft.util.math.MathHelper;

public final class PulseColors {
    public static final String[] NAMES = {"Фиолетовый", "Синий", "Голубой", "Зелёный", "Жёлтый",
            "Оранжевый", "Красный", "Розовый", "Белый", "Радуга"};
    private static final int[] RGB = {0x8B5CF6, 0x4F7CFF, 0x38D9F5, 0x4ADE80, 0xFACC15,
            0xFB923C, 0xEF4444, 0xF472B6, 0xFFFFFF, 0};

    public static final String[] SKY = {"Pulse", "Nebula", "Aurora", "Galaxy", "Sunset", "Lava"};

    private PulseColors() { }

    public static int hsv(float h, float s, float v) {
        return MathHelper.hsvToRgb(((h % 1f) + 1f) % 1f, s, v);
    }

    public static int rainbow(float offset) {
        double t = (System.currentTimeMillis() % 100000L) / 4000.0;
        return hsv((float) ((t + offset) % 1.0), 0.65f, 1f);
    }

    public static int palette(int idx) {
        return idx >= RGB.length - 1 ? rainbow(0f) : RGB[idx];
    }

    public static int argb(int alpha, int rgb) { return (alpha << 24) | (rgb & 0xFFFFFF); }

    public static int lerp(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /** RGB colour of the custom sky for the World Customizer style. */
    public static int sky() {
        Module w = Modules.WORLD;
        double t = (System.currentTimeMillis() % 1000000L) / 1000.0 * w.num("speed");
        float s = (float) Math.sin(t * 0.5) * 0.5f + 0.5f;
        return switch (w.mode("style")) {
            case 0 -> hsv((float) ((t * 0.05) % 1.0), 0.55f, 0.95f);
            case 1 -> hsv(0.78f + 0.12f * s, 0.65f, 0.75f);
            case 2 -> hsv(0.33f + 0.17f * s, 0.6f, 0.8f);
            case 3 -> hsv(0.64f + 0.12f * s, 0.7f, 0.55f);
            case 4 -> lerp(0xFF8A3C, 0xFF4FA3, s);
            default -> hsv(0.07f * s, 0.9f, 0.85f);
        };
    }
}
