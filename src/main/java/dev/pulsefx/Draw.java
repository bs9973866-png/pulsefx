package dev.pulsefx;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public final class Draw {
    public static final int ACC = 0xFF5B4BE6, OFF = 0xFF3B3963;

    private Draw() { }

    /** Filled rectangle with rounded corners. */
    public static void rr(DrawContext c, int x, int y, int w, int h, int r, int col) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) { c.fill(x, y, x + w, y + h, col); return; }
        c.fill(x, y + r, x + w, y + h - r, col);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, col);
            c.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, col);
        }
    }

    public static void toggle(DrawContext c, int x, int y, boolean on) {
        rr(c, x, y, 22, 10, 5, on ? ACC : OFF);
        rr(c, on ? x + 13 : x + 3, y + 2, 6, 6, 3, 0xFFFFFFFF);
    }

    public static void small(DrawContext c, TextRenderer tr, String s, int x, int y, int col, float sc) {
        c.getMatrices().push();
        c.getMatrices().translate((float) x, (float) y, 0f);
        c.getMatrices().scale(sc, sc, 1f);
        c.drawTextWithShadow(tr, s, 0, 0, col);
        c.getMatrices().pop();
    }

    public static String trim(TextRenderer tr, String s, int maxPx, float sc) {
        return tr.trimToWidth(s, (int) (maxPx / sc));
    }
}
