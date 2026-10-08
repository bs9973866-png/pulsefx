package dev.pulsefx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/** All HUD drawing routines. */
final class Hud {
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] ARMOR_NAMES = {"Шлем", "Нагрудник", "Поножи", "Ботинки"};

    private Hud() { }

    private static int pctColor(int pct) { return 0xFF000000 | (pct > 50 ? 0x55FF55 : pct > 20 ? 0xFFFF55 : 0xFF5555); }

    static void info(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        Module m = Modules.INFO;
        List<String> lines = new ArrayList<>();
        if (m.bool("fps")) lines.add(mc.getCurrentFps() + " FPS");
        if (m.bool("ping") && mc.getNetworkHandler() != null) {
            var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) lines.add(entry.getLatency() + " ms");
        }
        if (m.bool("coords")) lines.add(String.format("XYZ %.0f %.0f %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ()));
        if (m.bool("dir")) {
            String d = mc.player.getHorizontalFacing().asString();
            lines.add(Character.toUpperCase(d.charAt(0)) + d.substring(1));
        }
        String title = "PulseFX";
        int w = tr.getWidth(title);
        for (String l : lines) w = Math.max(w, tr.getWidth(l));
        w += 10;
        int h = 14 + lines.size() * 10;
        int x = 4, y = 4;
        if (m.bool("bg")) ctx.fill(x, y, x + w, y + h, 0x66000000);
        ctx.fill(x, y, x + 2, y + h, PulseColors.argb(255, PulseColors.rainbow(0f)));
        int tx = x + 6;
        for (int i = 0; i < title.length(); i++) {
            String ch = String.valueOf(title.charAt(i));
            ctx.drawTextWithShadow(tr, ch, tx, y + 3, PulseColors.argb(255, PulseColors.rainbow(i * 0.08f)));
            tx += tr.getWidth(ch);
        }
        int ly = y + 14;
        for (String l : lines) {
            ctx.drawTextWithShadow(tr, l, x + 6, ly, 0xFFFFFFFF);
            ly += 10;
        }
    }

    static void crosshair(DrawContext ctx, RenderTickCounter tc) {
        Module m = Modules.CROSSHAIR;
        int cx = ctx.getScaledWindowWidth() / 2, cy = ctx.getScaledWindowHeight() / 2;
        int style = m.mode("style");
        int len = Math.round(m.num("size")), gap0 = Math.round(m.num("gap"));
        float pulse = style == 0 ? (MathHelper.sin((Modules.ticks + tc.getTickDelta(false)) * 0.12f) + 1f) / 2f : 0f;
        int gap = gap0 + Math.round(pulse * 3);
        int col = PulseColors.argb(style == 0 ? 140 + Math.round(pulse * 115) : 230, m.rgb("color"));
        if (style == 2) { ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, col); return; }
        ctx.fill(cx - gap - len, cy, cx - gap, cy + 1, col);
        ctx.fill(cx + gap, cy, cx + gap + len, cy + 1, col);
        ctx.fill(cx, cy - gap - len, cx + 1, cy - gap, col);
        ctx.fill(cx, cy + gap, cx + 1, cy + gap + len, col);
    }

    static void keystrokes(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        int rgb = Modules.KEYSTROKES.rgb("color");
        int x = 4, y = ctx.getScaledWindowHeight() - 62;
        key(ctx, tr, "W", x + 20, y, 18, 18, mc.options.forwardKey.isPressed(), rgb);
        key(ctx, tr, "A", x, y + 20, 18, 18, mc.options.leftKey.isPressed(), rgb);
        key(ctx, tr, "S", x + 20, y + 20, 18, 18, mc.options.backKey.isPressed(), rgb);
        key(ctx, tr, "D", x + 40, y + 20, 18, 18, mc.options.rightKey.isPressed(), rgb);
        key(ctx, tr, "SPACE", x, y + 40, 58, 12, mc.options.jumpKey.isPressed(), rgb);
    }

    private static void key(DrawContext ctx, TextRenderer tr, String label, int x, int y, int w, int h, boolean down, int rgb) {
        ctx.fill(x, y, x + w, y + h, down ? PulseColors.argb(170, rgb) : 0x66000000);
        ctx.drawCenteredTextWithShadow(tr, label, x + w / 2, y + (h - 8) / 2 + 1, 0xFFFFFFFF);
    }

    static void armor(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        int x = ctx.getScaledWindowWidth() - 52, y = ctx.getScaledWindowHeight() - 84;
        for (int i = 0; i < 4; i++) {
            ItemStack s = mc.player.getEquippedStack(ARMOR[i]);
            if (s.isEmpty()) continue;
            ctx.drawItem(s, x, y + i * 18);
            if (s.isDamageable()) {
                int pct = (s.getMaxDamage() - s.getDamage()) * 100 / s.getMaxDamage();
                ctx.drawTextWithShadow(tr, pct + "%", x + 19, y + i * 18 + 4, pctColor(pct));
            }
        }
    }

    static void inventory(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        int x = ctx.getScaledWindowWidth() - 150, y = 4;
        ctx.fill(x - 2, y - 2, x + 146, y + 50, 0x66000000);
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                ItemStack s = mc.player.getInventory().getStack(9 + r * 9 + c);
                if (s.isEmpty()) continue;
                ctx.drawItem(s, x + c * 16, y + r * 16);
                if (s.getCount() > 1) Draw.small(ctx, tr, String.valueOf(s.getCount()), x + c * 16 + 8, y + r * 16 + 9, 0xFFFFFFFF, 0.6f);
            }
        }
    }

    static void hotkeys(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        var o = mc.options;
        KeyBinding[] ks = {o.sneakKey, o.sprintKey, o.jumpKey, o.inventoryKey, o.swapHandsKey, o.dropKey};
        int y = ctx.getScaledWindowHeight() / 2 - 30;
        for (KeyBinding k : ks) {
            String s = Text.translatable(k.getTranslationKey()).getString() + ": " + k.getBoundKeyLocalizedText().getString();
            ctx.drawTextWithShadow(tr, s, 4, y, 0xFFCCCCDD);
            y += 10;
        }
    }

    static void totems(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int n = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (s.isOf(Items.TOTEM_OF_UNDYING)) n += s.getCount();
        }
        ItemStack off = mc.player.getOffHandStack();
        if (off.isOf(Items.TOTEM_OF_UNDYING)) n += off.getCount();
        int x = ctx.getScaledWindowWidth() / 2 - 91, y = ctx.getScaledWindowHeight() - 66;
        ctx.drawItem(new ItemStack(Items.TOTEM_OF_UNDYING), x, y);
        ctx.drawTextWithShadow(mc.textRenderer, "x" + n, x + 18, y + 4, n > 0 ? 0xFFFFFFFF : 0xFFFF5555);
    }

    static void saturation(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        String s = String.format("Сытость %.1f", mc.player.getHungerManager().getSaturationLevel());
        int w = mc.textRenderer.getWidth(s);
        ctx.drawTextWithShadow(mc.textRenderer, s, ctx.getScaledWindowWidth() / 2 + 91 - w, ctx.getScaledWindowHeight() - 62, 0xFFFFD27F);
    }

    static void targetHud(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        LivingEntity t = Modules.currentTarget(mc, false);
        if (t == null) return;
        int x = ctx.getScaledWindowWidth() / 2 + 24, y = ctx.getScaledWindowHeight() / 2 + 14, w = 120;
        Draw.rr(ctx, x, y, w, 34, 4, 0x99000000);
        ctx.fill(x, y + 3, x + 2, y + 31, PulseColors.argb(255, PulseColors.rainbow(0f)));
        ctx.drawTextWithShadow(tr, t.getName().getString(), x + 7, y + 4, 0xFFFFFFFF);
        float hp = Math.max(0f, t.getHealth()), max = Math.max(1f, t.getMaxHealth());
        int bw = w - 14;
        ctx.fill(x + 7, y + 16, x + 7 + bw, y + 20, 0x55FFFFFF);
        ctx.fill(x + 7, y + 16, x + 7 + (int) (bw * Math.min(1f, hp / max)), y + 20, 0xFFFF5555);
        ctx.drawTextWithShadow(tr, String.format("HP %.1f", hp), x + 7, y + 23, 0xFFFFAAAA);
        String ar = "Броня " + t.getArmor();
        ctx.drawTextWithShadow(tr, ar, x + w - 7 - tr.getWidth(ar), y + 23, 0xFFAAC8FF);
    }

    static void tnt(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TntEntity best = null;
        double bd = 48 * 48;
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof TntEntity t) {
                double d = t.squaredDistanceTo(mc.player);
                if (d < bd) { bd = d; best = t; }
            }
        }
        if (best == null) return;
        String s = String.format("TNT %.1f c  (%.0f м)", best.getFuse() / 20f, Math.sqrt(bd));
        ctx.drawCenteredTextWithShadow(mc.textRenderer, s, ctx.getScaledWindowWidth() / 2, 46, 0xFFFF7755);
    }

    static void armorWarn(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int thr = Math.round(Modules.ARMOR_WARN.num("threshold"));
        String worst = null;
        int worstPct = 101;
        for (int i = 0; i < 4; i++) {
            ItemStack s = mc.player.getEquippedStack(ARMOR[i]);
            if (s.isEmpty() || !s.isDamageable()) continue;
            int pct = (s.getMaxDamage() - s.getDamage()) * 100 / s.getMaxDamage();
            if (pct < thr && pct < worstPct) { worstPct = pct; worst = ARMOR_NAMES[i]; }
        }
        if (worst == null) return;
        float a = (MathHelper.sin(Modules.ticks * 0.3f) + 1f) / 2f;
        ctx.drawCenteredTextWithShadow(mc.textRenderer, "! " + worst + ": " + worstPct + "%",
                ctx.getScaledWindowWidth() / 2, 32, PulseColors.argb(120 + (int) (a * 135), 0xFF5555));
    }

    static void team(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        int y = 70;
        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            String name = p.getName().getString();
            if (p == mc.player || !Store.isFriend(name)) continue;
            ctx.fill(4, y, 118, y + 22, 0x66000000);
            ctx.drawTextWithShadow(tr, name, 8, y + 2, 0xFFFFFFFF);
            String ar = "Бр. " + p.getArmor();
            ctx.drawTextWithShadow(tr, ar, 114 - tr.getWidth(ar), y + 2, 0xFFAAC8FF);
            float hp = Math.max(0f, p.getHealth()), max = Math.max(1f, p.getMaxHealth());
            ctx.fill(8, y + 14, 110, y + 18, 0x55FFFFFF);
            ctx.fill(8, y + 14, 8 + (int) (102 * Math.min(1f, hp / max)), y + 18, 0xFFFF5555);
            y += 25;
        }
    }

    static void pointers(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        int cx = ctx.getScaledWindowWidth() / 2, cy = ctx.getScaledWindowHeight() / 2;
        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            String name = p.getName().getString();
            if (p == mc.player || !Store.isFriend(name)) continue;
            double dx = p.getX() - mc.player.getX(), dz = p.getZ() - mc.player.getZ();
            float toTarget = (float) Math.toDegrees(Math.atan2(-dx, dz));
            double diff = Math.toRadians(MathHelper.wrapDegrees(toTarget - mc.player.getYaw()));
            int px = cx + (int) (Math.sin(diff) * 62), py = cy - (int) (Math.cos(diff) * 62);
            int col = 0xFF000000 | PulseColors.rainbow(0.3f);
            Draw.rr(ctx, px - 3, py - 3, 7, 7, 3, col);
            String s = name + " " + Math.round(Math.sqrt(dx * dx + dz * dz)) + "м";
            Draw.small(ctx, tr, s, px - (int) (tr.getWidth(s) * 0.35f), py + 6, 0xFFFFFFFF, 0.7f);
        }
    }
}
