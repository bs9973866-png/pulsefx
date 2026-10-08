package dev.pulsefx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Saves module states, settings and friends to .minecraft/config/pulsefx.json */
public final class Store {
    public static class Data {
        public Map<String, String> v = new LinkedHashMap<>();
        public List<String> friends = new ArrayList<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Data data = new Data();

    private Store() { }

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("pulsefx.json"); }
    private static Path slot(int n) { return FabricLoader.getInstance().getConfigDir().resolve("pulsefx-slot" + n + ".json"); }

    public static void load() {
        try {
            if (Files.exists(file())) {
                Data d = GSON.fromJson(Files.readString(file()), Data.class);
                if (d != null) data = d;
                if (data.v == null) data.v = new LinkedHashMap<>();
                if (data.friends == null) data.friends = new ArrayList<>();
            }
        } catch (Exception ignored) { }
        applyAll();
    }

    public static void applyAll() {
        for (Module m : Modules.ALL) {
            String e = data.v.get(m.name + "/on");
            if (e != null && !m.soon) m.enabled = Boolean.parseBoolean(e);
            for (Setting s : m.settings) {
                String v = data.v.get(m.name + "/" + s.id);
                if (v != null) s.deserialize(v);
            }
        }
    }

    private static void collect() {
        for (Module m : Modules.ALL) {
            data.v.put(m.name + "/on", String.valueOf(m.enabled));
            for (Setting s : m.settings) data.v.put(m.name + "/" + s.id, s.serialize());
        }
    }

    public static void save() {
        try {
            collect();
            Files.writeString(file(), GSON.toJson(data));
        } catch (Exception ignored) { }
    }

    public static boolean saveSlot(int n) {
        try {
            collect();
            Files.writeString(slot(n), GSON.toJson(data));
            return true;
        } catch (Exception e) { return false; }
    }

    public static boolean loadSlot(int n) {
        try {
            if (!Files.exists(slot(n))) return false;
            Data d = GSON.fromJson(Files.readString(slot(n)), Data.class);
            if (d == null || d.v == null) return false;
            data.v = d.v;
            applyAll();
            save();
            return true;
        } catch (Exception e) { return false; }
    }

    public static void resetAll() {
        for (Module m : Modules.ALL) m.reset();
        save();
    }

    // ---- friends ----
    public static boolean isFriend(String name) {
        for (String f : data.friends) if (f.equalsIgnoreCase(name)) return true;
        return false;
    }
    public static void addFriend(String name) { if (!isFriend(name)) { data.friends.add(name); save(); } }
    public static void removeFriend(String name) { data.friends.removeIf(f -> f.equalsIgnoreCase(name)); save(); }
}
