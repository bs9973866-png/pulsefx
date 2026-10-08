package dev.pulsefx;

/** One option of a module: switch, slider, mode list or colour. */
public class Setting {
    public enum Type { BOOL, FLOAT, MODE, COLOR }

    public final String id, name;
    public final Type type;
    public int tab = 0;
    public boolean b, defB;
    public float f, min, max, defF;
    public int idx, defIdx;
    public String[] modes;
    public String fmt = "%.1f";

    private Setting(String id, String name, Type type) {
        this.id = id; this.name = name; this.type = type;
    }

    public static Setting bool(String id, String name, boolean def) {
        Setting s = new Setting(id, name, Type.BOOL);
        s.b = s.defB = def;
        return s;
    }

    public static Setting num(String id, String name, float min, float max, float def, String fmt) {
        Setting s = new Setting(id, name, Type.FLOAT);
        s.min = min; s.max = max; s.f = s.defF = def; s.fmt = fmt;
        return s;
    }

    public static Setting mode(String id, String name, int def, String... modes) {
        Setting s = new Setting(id, name, Type.MODE);
        s.modes = modes; s.idx = s.defIdx = def;
        return s;
    }

    public static Setting color(String id, String name, int def) {
        Setting s = new Setting(id, name, Type.COLOR);
        s.modes = PulseColors.NAMES; s.idx = s.defIdx = def;
        return s;
    }

    public Setting tab(int t) { this.tab = t; return this; }

    public int rgb() { return PulseColors.palette(idx); }

    public void reset() { b = defB; f = defF; idx = defIdx; }

    public String serialize() {
        return switch (type) {
            case BOOL -> String.valueOf(b);
            case FLOAT -> String.valueOf(f);
            default -> String.valueOf(idx);
        };
    }

    public void deserialize(String v) {
        try {
            switch (type) {
                case BOOL -> b = Boolean.parseBoolean(v);
                case FLOAT -> f = Math.max(min, Math.min(max, Float.parseFloat(v)));
                default -> idx = Math.max(0, Math.min(modes.length - 1, Integer.parseInt(v)));
            }
        } catch (Exception ignored) { }
    }
}
