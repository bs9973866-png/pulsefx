package dev.pulsefx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Module {
    public enum Category { VISUALS, HUD, UTILITIES, COSMETICS, GROUPS }

    public final String name, desc;
    public final Category cat;
    public final boolean defEnabled;
    public boolean enabled, soon;
    public final List<Setting> settings = new ArrayList<>();
    public Consumer<MinecraftClient> onTick;
    public BiConsumer<DrawContext, RenderTickCounter> onHud;

    public Module(String name, String desc, Category cat, boolean def) {
        this.name = name; this.desc = desc; this.cat = cat;
        this.defEnabled = def; this.enabled = def;
    }

    public Module add(Setting s) { settings.add(s); return this; }
    public Module tick(Consumer<MinecraftClient> t) { onTick = t; return this; }
    public Module hud(BiConsumer<DrawContext, RenderTickCounter> h) { onHud = h; return this; }
    public Module soon() { soon = true; enabled = false; return this; }

    public Setting get(String id) {
        for (Setting s : settings) if (s.id.equals(id)) return s;
        throw new IllegalArgumentException("No setting " + id + " in " + name);
    }
    public boolean bool(String id) { return get(id).b; }
    public float num(String id) { return get(id).f; }
    public int mode(String id) { return get(id).idx; }
    public int rgb(String id) { return get(id).rgb(); }

    public int maxTab() {
        int t = 0;
        for (Setting s : settings) t = Math.max(t, s.tab);
        return t;
    }

    public void reset() {
        enabled = defEnabled;
        for (Setting s : settings) s.reset();
    }
}
