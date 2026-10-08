package dev.pulsefx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A toggleable module with its own settings. */
public final class Feature {
    public final String id, name, desc, icon;
    public final Category cat;
    public final boolean defEnabled;
    public boolean enabled;
    public final List<Setting> settings = new ArrayList<>();
    private final Map<String, Setting> byKey = new HashMap<>();

    public Feature(Category cat, String id, String name, String icon, String desc, boolean on, Setting... list) {
        this.cat = cat; this.id = id; this.name = name; this.icon = icon; this.desc = desc;
        this.defEnabled = on; this.enabled = on;
        for (Setting s : list) { settings.add(s); byKey.put(s.key, s); }
    }

    public boolean b(String key) { return byKey.get(key).on(); }
    public float n(String key) { return byKey.get(key).value; }
    public int m(String key) { return byKey.get(key).idx(); }

    /** Colour (RGB) for a colour setting; the last palette entry is the animated rainbow. */
    public int color(String key, float offset) {
        int i = byKey.get(key).idx();
        return i >= Modules.PALETTE.length ? PulseColors.rainbow(offset) : Modules.PALETTE[i];
    }

    public void reset() {
        enabled = defEnabled;
        for (Setting s : settings) s.reset();
    }
}
