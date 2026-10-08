package dev.pulsefx;

import dev.pulsefx.Module.Category;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Registry of all modules and the logic of the world-effect modules. */
public final class Modules {
    public static final List<Module> ALL = new ArrayList<>();
    static long ticks;
    static LivingEntity target;
    static long targetTime;
    private static int deathTicks;

    private static Module reg(Module m) { ALL.add(m); return m; }
    public static void init() { }

    // ===================== VISUALS =====================
    public static final Module HIT_RANGE = reg(new Module("Hit Range", "Радиус атаки под игроком.", Category.VISUALS, false)
            .add(Setting.num("radius", "Радиус", 1f, 6f, 3f, "%.1f"))
            .add(Setting.color("color", "Цвет", 0).tab(1))
            .tick(Modules::hitRange));

    public static final Module CUSTOM_HAND = reg(new Module("Custom Hand", "Кастомная обводка рук.", Category.VISUALS, false).soon());

    public static final Module BODY_GLOW = reg(new Module("Body Glow", "Свечение персонажей.", Category.VISUALS, false)
            .add(Setting.bool("self", "Свечение себя (3-е лицо)", true))
            .add(Setting.bool("others", "Свечение других игроков", false))
            .add(Setting.color("color", "Цвет", 2).tab(1))
            .tick(Modules::bodyGlow));

    public static final Module TRAILS = reg(new Module("Trails", "Следы за игроком.", Category.VISUALS, false)
            .add(Setting.mode("mode", "Окраска", 0, "Градиент", "Один цвет", "Радуга"))
            .add(Setting.mode("physics", "Физика", 0, "Всплывают", "Падают", "Разлетаются"))
            .add(Setting.num("density", "Плотность", 1f, 6f, 2f, "%.0f"))
            .add(Setting.color("color", "Цвет 1", 0).tab(1))
            .add(Setting.color("color2", "Цвет 2", 7).tab(1))
            .tick(Modules::trails));

    public static final Module WORLD = reg(new Module("World Customizer", "Изменение неба.", Category.VISUALS, false)
            .add(Setting.mode("style", "Небо", 1, PulseColors.SKY))
            .add(Setting.num("speed", "Скорость", 0.2f, 3f, 1f, "%.1f")));

    public static final Module TARGET_ESP = reg(new Module("Target ESP", "Подсветка цели.", Category.VISUALS, false)
            .add(Setting.mode("mode", "Режим отображения", 0, "Кольца", "Спираль", "Нимб"))
            .add(Setting.num("thick", "Толщина", 1f, 4f, 2f, "%.1f"))
            .add(Setting.bool("combat", "Показывать только в бою", true))
            .add(Setting.color("color", "Цвет цели", 0).tab(1))
            .tick(Modules::targetEsp));

    public static final Module ITEM_PHYSICS = reg(new Module("Item Physics", "Физика выпавших предметов.", Category.VISUALS, false).soon());

    public static final Module HIT_BUBBLES = reg(new Module("Hit Bubbles", "Эффект при попадании.", Category.VISUALS, false)
            .add(Setting.num("count", "Количество", 6f, 60f, 24f, "%.0f")));

    public static final Module KILL_EFFECT = reg(new Module("Kill Effect", "Эффект убийства.", Category.VISUALS, false)
            .add(Setting.mode("mode", "Эффект", 0, "Звёзды", "Взрыв", "Кольца")));

    public static final Module WORLD_PARTICLES = reg(new Module("World Particles", "Частицы в мире.", Category.VISUALS, false)
            .add(Setting.mode("mode", "Режим", 0, "Звёзды", "Кольца", "Спирали", "Лучи"))
            .add(Setting.num("amount", "Количество", 1f, 12f, 4f, "%.0f"))
            .add(Setting.color("color", "Цвет", 0).tab(1))
            .tick(Modules::worldParticles));

    public static final Module STAR_GLOW = reg(new Module("Star Glow", "Звёздный эффект в Kill Effect.", Category.VISUALS, false));

    public static final Module PREDICTION = reg(new Module("Prediction", "След снарядов (жемчуг, стрелы).", Category.VISUALS, false)
            .add(Setting.num("length", "Длина", 10f, 60f, 30f, "%.0f"))
            .add(Setting.color("color", "Цвет", 3).tab(1))
            .tick(Modules::prediction));

    public static final Module ARROW_NAMETAG = reg(new Module("Arrow Nametag", "Название прилетевшей стрелы.", Category.VISUALS, false).soon());

    // ===================== HUD =====================
    public static final Module INFO = reg(new Module("Info Panel", "FPS, пинг, координаты.", Category.HUD, true)
            .add(Setting.bool("fps", "FPS", true))
            .add(Setting.bool("ping", "Пинг", true))
            .add(Setting.bool("coords", "Координаты", true))
            .add(Setting.bool("dir", "Направление", true))
            .add(Setting.bool("bg", "Фон", true))
            .hud(Hud::info));

    public static final Module CROSSHAIR = reg(new Module("Crosshair", "Кастомный прицел.", Category.HUD, true)
            .add(Setting.mode("style", "Стиль", 0, "Пульс", "Статичный", "Точка"))
            .add(Setting.num("size", "Длина", 2f, 10f, 4f, "%.0f"))
            .add(Setting.num("gap", "Отступ", 2f, 10f, 5f, "%.0f"))
            .add(Setting.color("color", "Цвет", 9).tab(1))
            .hud(Hud::crosshair));

    public static final Module KEYSTROKES = reg(new Module("Keystrokes", "Нажатые клавиши WASD.", Category.HUD, true)
            .add(Setting.color("color", "Цвет", 9).tab(1))
            .hud(Hud::keystrokes));

    public static final Module ARMOR_HUD = reg(new Module("Armor HUD", "Прочность брони.", Category.HUD, false).hud(Hud::armor));
    public static final Module INVENTORY_HUD = reg(new Module("Inventory HUD", "Показ инвентаря.", Category.HUD, false).hud(Hud::inventory));
    public static final Module HOTKEYS = reg(new Module("Hotkeys", "Назначенные клавиши.", Category.HUD, false).hud(Hud::hotkeys));
    public static final Module TOTEM = reg(new Module("Totem Bar", "Счётчик тотемов.", Category.HUD, false).hud(Hud::totems));
    public static final Module SATURATION = reg(new Module("Saturation HUD", "Уровень сытости.", Category.HUD, false).hud(Hud::saturation));
    public static final Module TARGET_HUD = reg(new Module("Target HUD", "Информация о цели.", Category.HUD, false).hud(Hud::targetHud));
    public static final Module TNT_TIMER = reg(new Module("TNT Timer", "Таймер до взрыва TNT.", Category.HUD, false).hud(Hud::tnt));
    public static final Module ARMOR_WARN = reg(new Module("Armor Notifier", "Уведомление о низкой прочности.", Category.HUD, false)
            .add(Setting.num("threshold", "Порог, %", 5f, 50f, 15f, "%.0f"))
            .hud(Hud::armorWarn));
    public static final Module TAB_CUSTOM = reg(new Module("Tab Customizer", "Настройка списка игроков.", Category.HUD, false).soon());

    // ===================== UTILITIES =====================
    public static final Module AUTO_RESPAWN = reg(new Module("Auto Respawn", "Автоматический респавн.", Category.UTILITIES, false)
            .add(Setting.num("delay", "Задержка, тики", 0f, 60f, 10f, "%.0f"))
            .tick(Modules::autoRespawn));
    public static final Module CHAT_HELPER = reg(new Module("Chat Helper", "Антиспам и история чата.", Category.UTILITIES, false).soon());
    public static final Module KEYBIND_MANAGER = reg(new Module("Keybind Manager", "Команды на клавиши.", Category.UTILITIES, false).soon());
    public static final Module SHULKER_PREVIEW = reg(new Module("Shulker Preview", "Просмотр содержимого шалкера.", Category.UTILITIES, false).soon());
    public static final Module INV_MANAGER = reg(new Module("Inventory Manager", "Наборы раскладки инвентаря.", Category.UTILITIES, false).soon());

    // ===================== COSMETICS =====================
    public static final Module CHINA_HAT = reg(new Module("China Hat", "Шляпа над головой.", Category.COSMETICS, false)
            .add(Setting.mode("shape", "Форма", 0, "Шляпа", "Нимб"))
            .add(Setting.bool("others", "Для других игроков", false))
            .add(Setting.color("color", "Цвет", 7).tab(1))
            .tick(Modules::chinaHat));

    // ===================== GROUPS =====================
    public static final Module TEAM_HUD = reg(new Module("Team HUD", "HP и броня друзей. /pulsefx friend add <ник>", Category.GROUPS, false).hud(Hud::team));
    public static final Module POINTERS = reg(new Module("Friend Pointers", "Указатели на друзей.", Category.GROUPS, false).hud(Hud::pointers));
    public static final Module GROUP_CHAT = reg(new Module("Group Chat", "Чат группы.", Category.GROUPS, false).soon());

    // ===================== dispatch =====================
    static void tickAll(MinecraftClient mc) {
        ticks++;
        if (mc.player == null || mc.world == null) return;
        if (target != null) {
            if (target.isDead() && ticks - targetTime < 400) {
                if (KILL_EFFECT.enabled) killEffect(mc.world, target);
                target = null;
            } else if (ticks - targetTime > 400) {
                target = null;
            }
        }
        for (Module m : ALL) {
            if (m.enabled && m.onTick != null) m.onTick.accept(mc);
        }
    }

    static void hudAll(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden || mc.getDebugHud().shouldShowDebugHud()) return;
        for (Module m : ALL) {
            if (m.enabled && m.onHud != null) m.onHud.accept(ctx, tc);
        }
    }

    static void onAttack(LivingEntity e) {
        target = e;
        targetTime = ticks;
        if (HIT_BUBBLES.enabled) {
            ClientWorld w = (ClientWorld) e.getWorld();
            int n = Math.round(HIT_BUBBLES.num("count"));
            var r = w.random;
            double x = e.getX(), y = e.getBodyY(0.6), z = e.getZ();
            for (int i = 0; i < n; i++) {
                double a = r.nextDouble() * Math.PI * 2;
                double s = 0.1 + r.nextDouble() * 0.12;
                w.addParticle(dust(PulseColors.rainbow(i / (float) n), 1.2f), x, y, z,
                        Math.cos(a) * s, 0.08 + r.nextDouble() * 0.1, Math.sin(a) * s);
            }
        }
    }

    /** Current fight target (last hit entity); with combatOnly=false also the entity under the crosshair. */
    static LivingEntity currentTarget(MinecraftClient mc, boolean combatOnly) {
        if (target != null && target.isAlive() && !target.isRemoved() && ticks - targetTime < 160) return target;
        if (!combatOnly && mc.targetedEntity instanceof LivingEntity le && le != mc.player && le.isAlive()) return le;
        return null;
    }

    // ===================== helpers =====================
    static DustParticleEffect dust(int rgb, float size) {
        return new DustParticleEffect(0xFF000000 | (rgb & 0xFFFFFF), size);
    }

    static void ring(ClientWorld w, double x, double y, double z, double r, int n, int rgb, float size, double vy) {
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            w.addParticle(dust(rgb, size), x + Math.cos(a) * r, y, z + Math.sin(a) * r, 0, vy, 0);
        }
    }

    // ===================== effects =====================
    private static void hitRange(MinecraftClient mc) {
        if (ticks % 2 != 0) return;
        var p = mc.player;
        ring(mc.world, p.getX(), p.getY() + 0.05, p.getZ(), HIT_RANGE.num("radius"), 36, HIT_RANGE.rgb("color"), 0.7f, 0);
    }

    private static void bodyGlow(MinecraftClient mc) {
        if (ticks % 3 != 0) return;
        int rgb = BODY_GLOW.rgb("color");
        boolean fp = mc.options.getPerspective().isFirstPerson();
        if (BODY_GLOW.bool("self") && !fp) aura(mc.world, mc.player, rgb);
        if (BODY_GLOW.bool("others")) {
            for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
                if (p != mc.player && p.squaredDistanceTo(mc.player) < 28 * 28) aura(mc.world, p, rgb);
            }
        }
    }

    private static void aura(ClientWorld w, Entity e, int rgb) {
        var r = w.random;
        for (int i = 0; i < 5; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double rr = e.getWidth() * 0.8;
            w.addParticle(dust(rgb, 0.8f), e.getX() + Math.cos(a) * rr, e.getY() + r.nextDouble() * e.getHeight(),
                    e.getZ() + Math.sin(a) * rr, 0, 0.02, 0);
        }
    }

    private static void trails(MinecraftClient mc) {
        var p = mc.player;
        Vec3d v = p.getVelocity();
        if (v.x * v.x + v.z * v.z < 0.003) return;
        var w = mc.world;
        var r = w.random;
        int n = Math.round(TRAILS.num("density"));
        for (int i = 0; i < n; i++) {
            int col = switch (TRAILS.mode("mode")) {
                case 0 -> PulseColors.lerp(TRAILS.rgb("color"), TRAILS.rgb("color2"), (float) (Math.sin(ticks * 0.1 + i) + 1) / 2f);
                case 1 -> TRAILS.rgb("color");
                default -> PulseColors.rainbow(ticks * 0.01f + i * 0.1f);
            };
            double vy = 0, vx = 0, vz = 0;
            switch (TRAILS.mode("physics")) {
                case 0 -> vy = 0.03;
                case 1 -> vy = -0.03;
                default -> { vx = (r.nextDouble() - .5) * 0.06; vz = (r.nextDouble() - .5) * 0.06; vy = 0.01; }
            }
            w.addParticle(dust(col, 1.0f), p.getX() + (r.nextDouble() - .5) * .4, p.getY() + 0.1 + r.nextDouble() * 0.2,
                    p.getZ() + (r.nextDouble() - .5) * .4, vx, vy, vz);
        }
    }

    private static void targetEsp(MinecraftClient mc) {
        if (ticks % 2 != 0) return;
        LivingEntity t = currentTarget(mc, TARGET_ESP.bool("combat"));
        if (t == null) return;
        var w = mc.world;
        int rgb = TARGET_ESP.rgb("color");
        float size = TARGET_ESP.num("thick") * 0.5f;
        double h = t.getHeight(), r = t.getWidth() * 0.9 + 0.15;
        double x = t.getX(), y = t.getY(), z = t.getZ();
        double phase = ticks * 0.12;
        switch (TARGET_ESP.mode("mode")) {
            case 0 -> {
                double k = (Math.sin(phase) + 1) / 2;
                ring(w, x, y + k * h, z, r, 18, rgb, size, 0);
                ring(w, x, y + (1 - k) * h, z, r, 18, rgb, size, 0);
            }
            case 1 -> {
                for (int i = 0; i < 3; i++) {
                    double a = phase * 2 + i * Math.PI * 2 / 3;
                    double yy = y + ((ticks * 0.03 + i / 3.0) % 1.0) * h;
                    w.addParticle(dust(rgb, size), x + Math.cos(a) * r, yy, z + Math.sin(a) * r, 0, 0, 0);
                }
            }
            default -> ring(w, x, y + h + 0.25, z, r * 0.8, 18, rgb, size, 0);
        }
    }

    private static void worldParticles(MinecraftClient mc) {
        if (ticks % 2 != 0) return;
        var w = mc.world;
        var p = mc.player;
        var r = w.random;
        int amount = Math.round(WORLD_PARTICLES.num("amount"));
        int rgb = WORLD_PARTICLES.rgb("color");
        switch (WORLD_PARTICLES.mode("mode")) {
            case 0 -> {
                for (int i = 0; i < amount; i++) {
                    double x = p.getX() + (r.nextDouble() - .5) * 20, y = p.getY() + r.nextDouble() * 7 - 1, z = p.getZ() + (r.nextDouble() - .5) * 20;
                    if (i % 3 == 0) w.addParticle(ParticleTypes.END_ROD, x, y, z, 0, -0.02, 0);
                    else w.addParticle(dust(rgb, 0.7f), x, y, z, 0, -0.02, 0);
                }
            }
            case 1 -> {
                for (int i = 0; i < Math.max(1, amount / 4); i++) {
                    ring(w, p.getX() + (r.nextDouble() - .5) * 14, p.getY() + r.nextDouble() * 4, p.getZ() + (r.nextDouble() - .5) * 14,
                            0.7, 14, rgb, 0.8f, 0);
                }
            }
            case 2 -> {
                for (int k = 0; k < amount; k++) {
                    double a = ticks * 0.25 + k * 0.5;
                    double yy = p.getY() + 0.2 + k * 0.18 + ((ticks * 0.05) % 1.0);
                    w.addParticle(dust(rgb, 0.8f), p.getX() + Math.cos(a) * 2.5, yy, p.getZ() + Math.sin(a) * 2.5, 0, 0, 0);
                }
            }
            default -> {
                for (int i = 0; i < Math.max(1, amount / 6); i++) {
                    double bx = p.getX() + (r.nextDouble() - .5) * 20, bz = p.getZ() + (r.nextDouble() - .5) * 20;
                    for (int j = 0; j < 10; j++) w.addParticle(dust(rgb, 0.8f), bx, p.getY() - 1 + j * 0.5, bz, 0, 0.05, 0);
                }
            }
        }
    }

    private static final Set<EntityType<?>> PROJECTILES = Set.of(EntityType.ARROW, EntityType.SPECTRAL_ARROW,
            EntityType.TRIDENT, EntityType.ENDER_PEARL, EntityType.SNOWBALL, EntityType.EGG);

    private static void prediction(MinecraftClient mc) {
        if (ticks % 2 != 0) return;
        var w = mc.world;
        int rgb = PREDICTION.rgb("color");
        int steps = Math.round(PREDICTION.num("length"));
        for (Entity e : w.getEntities()) {
            if (!PROJECTILES.contains(e.getType()) || e.squaredDistanceTo(mc.player) > 64 * 64) continue;
            Vec3d v = e.getVelocity();
            if (v.lengthSquared() < 0.01) continue;
            boolean arrowLike = e.getType() == EntityType.ARROW || e.getType() == EntityType.SPECTRAL_ARROW || e.getType() == EntityType.TRIDENT;
            double g = arrowLike ? 0.05 : 0.03;
            Vec3d pos = e.getPos();
            for (int i = 0; i < steps; i++) {
                Vec3d next = pos.add(v);
                BlockHitResult hit = w.raycast(new RaycastContext(pos, next, RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE, e));
                if (hit.getType() == HitResult.Type.BLOCK) {
                    Vec3d hp = hit.getPos();
                    w.addParticle(dust(rgb, 1.3f), hp.x, hp.y, hp.z, 0, 0, 0);
                    break;
                }
                if (i % 2 == 0) w.addParticle(dust(rgb, 0.6f), next.x, next.y, next.z, 0, 0, 0);
                pos = next;
                v = new Vec3d(v.x * 0.99, v.y * 0.99 - g, v.z * 0.99);
            }
        }
    }

    private static void chinaHat(MinecraftClient mc) {
        if (ticks % 2 != 0) return;
        var w = mc.world;
        int rgb = CHINA_HAT.rgb("color");
        if (!mc.options.getPerspective().isFirstPerson()) hat(w, mc.player, rgb);
        if (CHINA_HAT.bool("others")) {
            for (AbstractClientPlayerEntity p : w.getPlayers()) {
                if (p != mc.player && p.squaredDistanceTo(mc.player) < 24 * 24) hat(w, p, rgb);
            }
        }
    }

    private static void hat(ClientWorld w, Entity e, int rgb) {
        double top = e.getY() + e.getHeight();
        if (CHINA_HAT.mode("shape") == 0) {
            ring(w, e.getX(), top + 0.10, e.getZ(), 0.55, 16, rgb, 0.7f, 0);
            ring(w, e.getX(), top + 0.28, e.getZ(), 0.38, 12, rgb, 0.7f, 0);
            ring(w, e.getX(), top + 0.46, e.getZ(), 0.20, 8, rgb, 0.7f, 0);
            w.addParticle(dust(rgb, 0.9f), e.getX(), top + 0.62, e.getZ(), 0, 0, 0);
        } else {
            ring(w, e.getX(), top + 0.35, e.getZ(), 0.35, 16, rgb, 0.7f, 0);
        }
    }

    private static void killEffect(ClientWorld w, LivingEntity e) {
        var r = w.random;
        double x = e.getX(), y = e.getY(), z = e.getZ(), h = e.getHeight();
        switch (KILL_EFFECT.mode("mode")) {
            case 0 -> {
                for (int i = 0; i < 24; i++) {
                    w.addParticle(ParticleTypes.END_ROD, x, y + h / 2, z,
                            (r.nextDouble() - .5) * .25, 0.05 + r.nextDouble() * .2, (r.nextDouble() - .5) * .25);
                }
            }
            case 1 -> {
                for (int i = 0; i < 40; i++) {
                    w.addParticle(dust(PulseColors.rainbow(i / 40f), 1.4f), x, y + h / 2, z,
                            (r.nextDouble() - .5) * .4, (r.nextDouble() - .2) * .3, (r.nextDouble() - .5) * .4);
                }
            }
            default -> {
                for (int k = 0; k < 4; k++) ring(w, x, y + k * h / 3, z, 0.9, 20, PulseColors.rainbow(k * 0.2f), 1.2f, 0.02);
            }
        }
        if (STAR_GLOW.enabled) {
            for (int i = 0; i < 30; i++) {
                double a = r.nextDouble() * Math.PI * 2, b = (r.nextDouble() - .5) * Math.PI;
                double s = 0.15;
                w.addParticle(ParticleTypes.END_ROD, x, y + h / 2, z,
                        Math.cos(a) * Math.cos(b) * s, Math.sin(b) * s, Math.sin(a) * Math.cos(b) * s);
            }
        }
    }

    private static void autoRespawn(MinecraftClient mc) {
        if (mc.currentScreen instanceof DeathScreen) {
            if (++deathTicks >= Math.round(AUTO_RESPAWN.num("delay"))) {
                mc.player.requestRespawn();
                deathTicks = 0;
            }
        } else {
            deathTicks = 0;
        }
    }
}
