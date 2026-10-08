package dev.pulsefx;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

/** PulseFX menu: sidebar, module cards, settings panel. Opened with Right Shift. */
public class PulseScreen extends Screen {
    private static final int BG = 0xF2100E24, SIDE = 0xF20C0B1D, CARD = 0xFF181636, CARD_H = 0xFF221F4A,
            TXT = 0xFFFFFFFF, SUB = 0xFF8D8BB5, ACC = Draw.ACC, ACC_T = 0xFF8B7CFF, OFF = Draw.OFF;
    private static final String[] CATS = {"Visuals", "HUD", "Utilities", "Cosmetics", "Groups", "Configs"};

    private static int cat = 0;
    private static Module selected;

    private final Screen parent;
    private final List<Hit> hits = new ArrayList<>();
    private Hit dragging;
    private String search = "", status = "";
    private long statusTime;
    private boolean searchFocus;
    private float scroll, pscroll, maxScroll, pMax;
    private int tab;
    private int wx, wy, W, H, sw, pw, gx, gy, gw, gh, px, py, ph;

    public PulseScreen(Screen parent) {
        super(Text.literal("PulseFX"));
        this.parent = parent;
    }

    private static final class Hit {
        final int x, y, w, h;
        final int[] clip;
        final IntConsumer click;
        DoubleConsumer drag;

        Hit(int x, int y, int w, int h, int[] clip, IntConsumer click) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.clip = clip; this.click = click;
        }

        boolean contains(double mx, double my) {
            if (mx < x || mx >= x + w || my < y || my >= y + h) return false;
            return clip == null || (mx >= clip[0] && mx < clip[2] && my >= clip[1] && my < clip[3]);
        }
    }

    private Hit hit(int x, int y, int w, int h, int[] clip, IntConsumer click) {
        Hit hh = new Hit(x, y, w, h, clip, click);
        hits.add(hh);
        return hh;
    }

    private static boolean hov(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    protected void init() {
        W = Math.min(width - 16, 520);
        H = Math.min(height - 16, 300);
        wx = (width - W) / 2;
        wy = (height - H) / 2;
        sw = Math.max(70, W * 17 / 100);
        pw = Math.max(120, W * 29 / 100);
        gx = wx + sw + 6;
        gy = wy + 26;
        gw = W - sw - pw - 18;
        gh = H - 32;
        px = gx + gw + 6;
        py = gy;
        ph = gh;
    }

    // ------------------------------------------------------------------ data
    private List<Module> visible() {
        List<Module> out = new ArrayList<>();
        String q = search.toLowerCase(Locale.ROOT);
        for (Module m : Modules.ALL) {
            if (!q.isEmpty()) {
                if (m.name.toLowerCase(Locale.ROOT).contains(q) || m.desc.toLowerCase(Locale.ROOT).contains(q)) out.add(m);
            } else if (cat < 5 && m.cat.ordinal() == cat) {
                out.add(m);
            }
        }
        return out;
    }

    private void setStatus(String s) { status = s; statusTime = System.currentTimeMillis(); }

    // ------------------------------------------------------------------ render
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        hits.clear();
        TextRenderer tr = textRenderer;

        Draw.rr(ctx, wx, wy, W, H, 6, BG);
        Draw.rr(ctx, wx, wy, sw, H, 6, SIDE);
        ctx.fill(wx + sw - 6, wy, wx + sw, wy + H, SIDE);
        ctx.fill(wx + sw, wy + 4, wx + sw + 1, wy + H - 4, 0x22FFFFFF);

        drawSidebar(ctx, tr, mx, my);
        drawTop(ctx, tr, mx, my);
        if (cat == 5 && search.isEmpty()) drawConfigs(ctx, tr, mx, my);
        else drawGrid(ctx, tr, mx, my);
        drawPanel(ctx, tr, mx, my);
    }

    private void drawSidebar(DrawContext ctx, TextRenderer tr, int mx, int my) {
        ctx.drawTextWithShadow(tr, "Pulse", wx + 10, wy + 9, TXT);
        ctx.drawTextWithShadow(tr, "FX", wx + 10 + tr.getWidth("Pulse"), wy + 9, ACC_T);
        for (int i = 0; i < CATS.length; i++) {
            int x = wx + 6, y = wy + 28 + i * 20, w = sw - 12;
            boolean sel = cat == i && search.isEmpty();
            if (sel) Draw.rr(ctx, x, y, w, 17, 4, ACC);
            else if (hov(mx, my, x, y, w, 17)) Draw.rr(ctx, x, y, w, 17, 4, CARD_H);
            Draw.rr(ctx, x + 5, y + 5, 7, 7, 2, sel ? 0xFFFFFFFF : ACC);
            ctx.drawTextWithShadow(tr, CATS[i], x + 18, y + 5, sel ? TXT : 0xFFB9B7D8);
            final int ci = i;
            hit(x, y, w, 17, null, b -> { cat = ci; scroll = 0; pscroll = 0; tab = 0; search = ""; selected = null; });
        }
        Draw.small(ctx, tr, "Right Shift - меню", wx + 8, wy + H - 22, SUB, 0.75f);
        Draw.small(ctx, tr, "PulseFX v1.1.0", wx + 8, wy + H - 12, SUB, 0.75f);
    }

    private void drawTop(DrawContext ctx, TextRenderer tr, int mx, int my) {
        int sy = wy + 5;
        Draw.rr(ctx, gx, sy, gw, 16, 4, searchFocus ? CARD_H : CARD);
        String shown = search.isEmpty() && !searchFocus ? "Поиск функций..." : search + (searchFocus && System.currentTimeMillis() / 500 % 2 == 0 ? "_" : "");
        ctx.drawTextWithShadow(tr, tr.trimToWidth(shown, gw - 12), gx + 6, sy + 4, search.isEmpty() && !searchFocus ? SUB : TXT);
        hit(gx, sy, gw, 16, null, b -> searchFocus = true);

        int cx = wx + W - 16;
        boolean h = hov(mx, my, cx - 2, wy + 4, 14, 16);
        ctx.drawTextWithShadow(tr, "x", cx + 2, wy + 9, h ? 0xFFFF6B6B : SUB);
        hit(cx - 2, wy + 4, 14, 16, null, b -> close());
    }

    private void drawGrid(DrawContext ctx, TextRenderer tr, int mx, int my) {
        List<Module> list = visible();
        String title = search.isEmpty() ? CATS[cat] : "Поиск";
        ctx.drawTextWithShadow(tr, title, gx + 2, gy + 2, TXT);
        String cnt = list.size() + " функций";
        ctx.drawTextWithShadow(tr, cnt, gx + gw - tr.getWidth(cnt) - 2, gy + 2, SUB);

        if (selected == null || (search.isEmpty() && cat < 5 && selected.cat.ordinal() != cat)) {
            selected = list.isEmpty() ? null : list.get(0);
        }

        int top = gy + 16, ch = 30, gap = 4, cw = (gw - gap) / 2;
        int rows = (list.size() + 1) / 2;
        maxScroll = Math.max(0, rows * (ch + gap) - (gh - 16));
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        int[] clip = {gx, top, gx + gw, gy + gh};
        ctx.enableScissor(gx, top, gx + gw, gy + gh);
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            int cx = gx + (i % 2) * (cw + gap);
            int cy = top + (i / 2) * (ch + gap) - (int) scroll;
            if (cy + ch < top || cy > gy + gh) continue;
            boolean sel = m == selected;
            boolean over = hov(mx, my, cx, cy, cw, ch) && my >= top && my < gy + gh;
            if (sel) { Draw.rr(ctx, cx, cy, cw, ch, 5, ACC); Draw.rr(ctx, cx + 1, cy + 1, cw - 2, ch - 2, 4, CARD_H); }
            else Draw.rr(ctx, cx, cy, cw, ch, 5, over ? CARD_H : CARD);

            Draw.rr(ctx, cx + 5, cy + 6, 18, 18, 4, 0xFF2A2760);
            String letter = m.name.substring(0, 1);
            ctx.drawCenteredTextWithShadow(tr, letter, cx + 14, cy + 11, ACC_T);
            ctx.drawTextWithShadow(tr, Draw.trim(tr, m.name, cw - 58, 1f), cx + 27, cy + 6, TXT);
            Draw.small(ctx, tr, Draw.trim(tr, m.desc, cw - 32, 0.72f), cx + 27, cy + 18, SUB, 0.72f);
            if (m.soon) {
                Draw.small(ctx, tr, "скоро", cx + cw - 28, cy + 11, SUB, 0.75f);
            } else {
                Draw.toggle(ctx, cx + cw - 27, cy + 10, m.enabled);
            }
            final Module fm = m;
            hit(cx, cy, cw, ch, clip, b -> {
                if (b == 1 && !fm.soon) fm.enabled = !fm.enabled;
                else { selected = fm; pscroll = 0; tab = 0; }
            });
            if (!m.soon) hit(cx + cw - 30, cy + 6, 28, 18, clip, b -> fm.enabled = !fm.enabled);
        }
        ctx.disableScissor();
    }

    private void drawConfigs(DrawContext ctx, TextRenderer tr, int mx, int my) {
        ctx.drawTextWithShadow(tr, "Configs", gx + 2, gy + 2, TXT);
        if (!status.isEmpty() && System.currentTimeMillis() - statusTime < 3000) {
            ctx.drawTextWithShadow(tr, status, gx + gw - tr.getWidth(status) - 2, gy + 2, ACC_T);
        }
        int top = gy + 16, ch = 30, gap = 4, cw = (gw - gap) / 2;
        for (int i = 0; i < 3; i++) {
            int cx = gx + (i % 2) * (cw + gap), cy = top + (i / 2) * (ch + gap);
            Draw.rr(ctx, cx, cy, cw, ch, 5, CARD);
            ctx.drawTextWithShadow(tr, "Слот " + (i + 1), cx + 7, cy + 4, TXT);
            final int n = i + 1;
            button(ctx, tr, mx, my, cx + 5, cy + 16, (cw - 14) / 2, 10, "Сохранить", () -> setStatus(Store.saveSlot(n) ? "Сохранено в слот " + n : "Ошибка сохранения"));
            button(ctx, tr, mx, my, cx + 9 + (cw - 14) / 2, cy + 16, (cw - 14) / 2, 10, "Загрузить", () -> setStatus(Store.loadSlot(n) ? "Загружен слот " + n : "Слот пуст"));
        }
        int cx = gx + (3 % 2) * (cw + gap), cy = top + (3 / 2) * (ch + gap);
        Draw.rr(ctx, cx, cy, cw, ch, 5, CARD);
        ctx.drawTextWithShadow(tr, "Сброс", cx + 7, cy + 4, TXT);
        button(ctx, tr, mx, my, cx + 5, cy + 16, cw - 10, 10, "Сбросить всё", () -> { Store.resetAll(); setStatus("Настройки сброшены"); });
    }

    private void button(DrawContext ctx, TextRenderer tr, int mx, int my, int x, int y, int w, int h, String label, Runnable r) {
        Draw.rr(ctx, x, y, w, h, 3, hov(mx, my, x, y, w, h) ? ACC : OFF);
        Draw.small(ctx, tr, label, x + (w - (int) (tr.getWidth(label) * 0.75f)) / 2, y + 2, TXT, 0.75f);
        hit(x, y, w, h, null, b -> r.run());
    }

    private void drawPanel(DrawContext ctx, TextRenderer tr, int mx, int my) {
        Draw.rr(ctx, px, py, pw, ph, 6, CARD);
        Module m = selected;
        if (m == null || (cat == 5 && search.isEmpty())) {
            Draw.small(ctx, tr, cat == 5 ? "Сохраняйте наборы настроек в слоты." : "Выберите функцию.", px + 8, py + 10, SUB, 0.8f);
            return;
        }
        Draw.rr(ctx, px + 6, py + 6, 20, 20, 5, 0xFF2A2760);
        ctx.drawCenteredTextWithShadow(tr, m.name.substring(0, 1), px + 16, py + 12, ACC_T);
        ctx.drawTextWithShadow(tr, Draw.trim(tr, m.name, pw - 70, 1f), px + 32, py + 7, TXT);
        Draw.small(ctx, tr, Draw.trim(tr, m.desc, pw - 70, 0.72f), px + 32, py + 18, SUB, 0.72f);
        if (m.soon) {
            Draw.small(ctx, tr, "Скоро", px + pw - 32, py + 12, SUB, 0.8f);
            Draw.small(ctx, tr, "Функция в разработке.", px + 8, py + 38, SUB, 0.8f);
            return;
        }
        Draw.toggle(ctx, px + pw - 30, py + 11, m.enabled);
        hit(px + pw - 32, py + 8, 28, 16, null, b -> m.enabled = !m.enabled);

        int y0 = py + 32;
        int maxTab = m.maxTab();
        if (maxTab > 0) {
            String[] names = {"Основные", "Цвета", "Доп."};
            int tw = (pw - 12) / (maxTab + 1);
            for (int t = 0; t <= maxTab; t++) {
                int x = px + 6 + t * tw;
                boolean sel = tab == t;
                Draw.rr(ctx, x, y0, tw - 2, 14, 4, sel ? ACC : (hov(mx, my, x, y0, tw - 2, 14) ? CARD_H : 0xFF1F1D40));
                Draw.small(ctx, tr, names[t], x + (tw - 2 - (int) (tr.getWidth(names[t]) * 0.75f)) / 2, y0 + 4, TXT, 0.75f);
                final int ft = t;
                hit(x, y0, tw - 2, 14, null, b -> { tab = ft; pscroll = 0; });
            }
            y0 += 18;
        }
        if (tab > maxTab) tab = 0;

        int[] clip = {px, y0, px + pw, py + ph};
        int total = 0;
        for (Setting s : m.settings) if (s.tab == tab) total += rowHeight(s);
        pMax = Math.max(0, total - (py + ph - y0 - 4));
        pscroll = Math.max(0, Math.min(pMax, pscroll));

        ctx.enableScissor(px, y0, px + pw, py + ph);
        int y = y0 + 2 - (int) pscroll;
        for (Setting s : m.settings) {
            if (s.tab != tab) continue;
            drawSetting(ctx, tr, mx, my, s, y, clip);
            y += rowHeight(s);
        }
        ctx.disableScissor();
    }

    private int rowHeight(Setting s) {
        return switch (s.type) {
            case BOOL -> 18;
            case FLOAT -> 26;
            default -> 30;
        };
    }

    private void drawSetting(DrawContext ctx, TextRenderer tr, int mx, int my, Setting s, int y, int[] clip) {
        int x = px + 8, w = pw - 16;
        switch (s.type) {
            case BOOL -> {
                ctx.drawTextWithShadow(tr, Draw.trim(tr, s.name, w - 28, 1f), x, y + 4, 0xFFD6D4F0);
                Draw.toggle(ctx, x + w - 22, y + 3, s.b);
                hit(x, y, w, 16, clip, b -> s.b = !s.b);
            }
            case FLOAT -> {
                ctx.drawTextWithShadow(tr, Draw.trim(tr, s.name, w - 30, 1f), x, y, 0xFFD6D4F0);
                String val = String.format(s.fmt, s.f);
                ctx.drawTextWithShadow(tr, val, x + w - tr.getWidth(val), y, TXT);
                float t = (s.f - s.min) / (s.max - s.min);
                Draw.rr(ctx, x, y + 13, w, 4, 2, OFF);
                Draw.rr(ctx, x, y + 13, Math.max(4, (int) (w * t)), 4, 2, ACC);
                Draw.rr(ctx, x + (int) ((w - 6) * t), y + 12, 6, 6, 3, 0xFFFFFFFF);
                Hit h = hit(x, y + 9, w, 14, clip, b -> { });
                DoubleConsumer setter = mxx -> {
                    float v = (float) Math.max(0, Math.min(1, (mxx - x) / (double) w));
                    float nv = s.min + (s.max - s.min) * v;
                    s.f = s.fmt.equals("%.0f") ? Math.round(nv) : Math.round(nv * 10f) / 10f;
                };
                h.drag = setter;
            }
            case MODE -> {
                ctx.drawTextWithShadow(tr, Draw.trim(tr, s.name, w, 1f), x, y, 0xFFD6D4F0);
                boolean over = hov(mx, my, x, y + 11, w, 14);
                Draw.rr(ctx, x, y + 11, w, 14, 4, over ? ACC : CARD_H);
                ctx.drawCenteredTextWithShadow(tr, s.modes[s.idx], x + w / 2, y + 15, TXT);
                hit(x, y + 11, w, 14, clip, b -> {
                    int n = s.modes.length;
                    s.idx = b == 1 ? (s.idx + n - 1) % n : (s.idx + 1) % n;
                });
            }
            default -> {
                ctx.drawTextWithShadow(tr, Draw.trim(tr, s.name, w, 1f), x, y, 0xFFD6D4F0);
                boolean over = hov(mx, my, x, y + 11, w, 14);
                Draw.rr(ctx, x, y + 11, w, 14, 4, over ? ACC : CARD_H);
                Draw.rr(ctx, x + 3, y + 14, 16, 8, 2, 0xFF000000 | s.rgb());
                ctx.drawTextWithShadow(tr, s.modes[s.idx], x + 24, y + 15, TXT);
                hit(x, y + 11, w, 14, clip, b -> {
                    int n = s.modes.length;
                    s.idx = b == 1 ? (s.idx + n - 1) % n : (s.idx + 1) % n;
                });
            }
        }
    }

    // ------------------------------------------------------------------ input
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        searchFocus = false;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.contains(mx, my)) {
                h.click.accept(button);
                if (h.drag != null) { dragging = h; h.drag.accept(mx); }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null && dragging.drag != null) { dragging.drag.accept(mx); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != null) { dragging = null; Store.save(); }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        if (mx >= px && mx < px + pw) pscroll = Math.max(0, Math.min(pMax, pscroll - (float) vAmount * 12f));
        else scroll = Math.max(0, Math.min(maxScroll, scroll - (float) vAmount * 14f));
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocus && chr >= 32 && chr != 127) {
            if (search.length() < 24) { search += chr; scroll = 0; }
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchFocus && keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() { Store.save(); }

    @Override
    public void close() { if (client != null) client.setScreen(parent); }
}
