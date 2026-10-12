package pl.teamping.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import pl.teamping.TeamPingMod;


public final class Theme {

    private Theme() {}



    
    public static final int[][] ACCENTS = {
            {0xFF06B6D4, 0xFF25C7E1, 0xFF7FEBFA},
            {0xFF5B6EE1, 0xFF6E80F0, 0xFF97A6FF},
            {0xFF8B5CF6, 0xFF9D75F8, 0xFFC4B5FD},
            {0xFFEC4899, 0xFFF160A8, 0xFFF9A8D4},
            {0xFF10B981, 0xFF2CC796, 0xFF6EE7B7},
            {0xFFF59E0B, 0xFFFFB023, 0xFFFCD34D},
            {0xFFEF4444, 0xFFF55E5E, 0xFFFCA5A5},
    };

    public static final String[] ACCENT_NAMES = {
            "Aqua", "Indygo", "Fiolet", "Róż", "Zieleń", "Bursztyn", "Czerwień"
    };

    private static int[] pal() {
        try {
            int cust = TeamPingMod.CONFIG.uiAccentCustom;
            if ((cust >>> 24) != 0) {

                int base = 0xFF000000 | (cust & 0xFFFFFF);
                return new int[] {base, lerpColor(base, 0xFFFFFFFF, 0.16), lerpColor(base, 0xFFFFFFFF, 0.58)};
            }
        } catch (Throwable ignored) {}
        int i = 0;
        try {
            i = TeamPingMod.CONFIG.uiAccent;
        } catch (Throwable ignored) {}
        return ACCENTS[Math.max(0, Math.min(ACCENTS.length - 1, i))];
    }

    public static int accent()      { return pal()[0]; }
    public static int accentHover() { return pal()[1]; }
    public static int accentText()  { return pal()[2]; }
    public static int accentSoft()  { return withAlpha(pal()[0], 0x3D); }
    public static int accentGlow()  { return withAlpha(pal()[0], 0x28); }



    
    public static final int[] BACKGROUNDS = {
            0xFF12151D,
            0xFF121317,
            0xFF0E1116,
            0xFF191621,
            0xFF141C1B,
            0xFF1C1820,
    };

    public static final String[] BACKGROUND_NAMES = {
            "Granat", "Grafit", "Noc", "Śliwka", "Butelkowy", "Węgiel"
    };

    
    public static final int[] SURFACES = {
            0xFF0F1219,
            0xFF0F1015,
            0xFF0B0E13,
            0xFF15121C,
            0xFF111817,
            0xFF18141C,
    };

    
    public static final int BG_SOLID = 0xFF161922;
    public static final int CARD = 0xFF13161F;

    private static int pick(int[] palette, int index, int custom, int fallback) {
        try {
            if ((custom >>> 24) != 0) return 0xFF000000 | (custom & 0xFFFFFF);
            return palette[Math.max(0, Math.min(palette.length - 1, index))];
        } catch (Throwable t) {
            return fallback;
        }
    }

    
    public static int bg() {
        try {
            return pick(BACKGROUNDS, TeamPingMod.CONFIG.uiBg, TeamPingMod.CONFIG.uiBgCustom, BG_SOLID);
        } catch (Throwable t) {
            return BG_SOLID;
        }
    }

    
    public static int card() {
        try {
            return pick(SURFACES, TeamPingMod.CONFIG.uiBg2, TeamPingMod.CONFIG.uiBg2Custom, CARD);
        } catch (Throwable t) {
            return CARD;
        }
    }

    
    private static int darker(int c, double f) {
        return lerpColor(c, 0xFF000000, f);
    }

    
    private static int lighter(int c, double f) {
        return lerpColor(c, 0xFFFFFFFF, f);
    }

    
    public static int panelBorder() { return darker(bg(), 0.30); }

    public static int cardHover() { return lighter(card(), 0.045); }
    
    public static int border() { return darker(card(), 0.22); }

    
    public static int surface() { return lighter(card(), 0.055); }
    public static int surfaceHover() { return lighter(card(), 0.10); }

    public static int track() { return darker(card(), 0.32); }
    public static int field() { return darker(card(), 0.45); }

    public static final int HOVER = 0x10FFFFFF;

    
    public static final int HAIRLINE = 0x14FFFFFF;
    public static final int HAIRLINE_STRONG = 0x22FFFFFF;



    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_DIM = 0xF2FFFFFF;
    public static final int TEXT_FAINT = 0xC4FFFFFF;

    public static final int DANGER = 0xFFE0603C;
    public static final int OK = 0xFF56C271;



    
    public static double GLOBAL_ALPHA = 1.0;

    
    public static double RASTER_PX_PER_UNIT = 0;

    
    public static double rasterScale() {
        if (RASTER_PX_PER_UNIT > 0) return RASTER_PX_PER_UNIT;
        try {
            double s = net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaleFactor();
            return s > 0 ? s : 2;
        } catch (Throwable t) {
            return 2;
        }
    }

    
    public static int alpha(int color) {
        return GLOBAL_ALPHA >= 0.999 ? color : fade(color, GLOBAL_ALPHA);
    }



    private static double opacity() {
        try {
            return Math.max(0.30, Math.min(1.0, TeamPingMod.CONFIG.uiOpacity));
        } catch (Throwable t) {
            return 0.95;
        }
    }

    
    private static double surfaceOpacity() {
        double o = opacity();
        return o + (1.0 - o) * 0.75;
    }

    
    public static int op(int color) {
        return fade(color, opacity());
    }

    
    public static int opSurface(int color) {
        return fade(color, surfaceOpacity());
    }

    
    public static final int RADIUS = 15;

    public static int panelRadius() {
        return RADIUS;
    }

    
    public static int cardRadius() {
        return panelRadius() - 4;
    }



    private static final float S_BODY = 8.6f;

    private static TpFont BODY()  { return TpFont.ui(S_BODY, 0.028f); }
    private static TpFont VALUE() { return TpFont.ui(S_BODY, 0.062f); }
    private static TpFont SMALL() { return TpFont.ui(7.2f, 0.032f); }
    private static TpFont MICRO() { return TpFont.ui(6.2f, 0.085f); }
    private static TpFont H2()    { return TpFont.display(11.0f, 0.040f); }
    private static TpFont H1()    { return TpFont.display(12.0f, 0.040f); }
    private static TpFont NAME()  { return TpFont.display(9.2f, 0.045f); }

    private static final Identifier FONT_UI = Identifier.of(TeamPingMod.MOD_ID, "ui");
    private static final Identifier FONT_DISPLAY = Identifier.of(TeamPingMod.MOD_ID, "display");

    private static final Style UI = Compat.fontStyle(FONT_UI);
    private static final Style DISPLAY = Compat.fontStyle(FONT_DISPLAY);

    public static TextRenderer font() {
        return MinecraftClient.getInstance().textRenderer;
    }

    
    private static boolean custom() {
        return true;
    }

    public static Text ui(String s) {
        return custom() ? Text.literal(s).setStyle(UI) : Text.literal(s);
    }

    public static Text display(String s) {
        return custom() ? Text.literal(s).setStyle(DISPLAY) : Text.literal(s);
    }



    public static int width(String s) {
        if (s == null || s.isEmpty()) return 0;
        if (custom() && BODY().ready()) return BODY().width(s);
        return font().getWidth(ui(s));
    }

    public static int widthSmall(String s) {
        if (s == null || s.isEmpty()) return 0;
        if (custom() && SMALL().ready()) return SMALL().width(s);
        return font().getWidth(ui(s));
    }

    public static int widthDisplay(String s) {
        if (s == null || s.isEmpty()) return 0;
        if (custom() && H2().ready()) return H2().width(s);
        return font().getWidth(display(s));
    }

    public static int widthBrand(String s) {
        if (s == null || s.isEmpty()) return 0;
        if (custom() && H1().ready()) return H1().width(s);
        return font().getWidth(display(s));
    }

    public static int lineH() {
        if (custom() && BODY().ready()) return BODY().height();
        return 9;
    }

    public static int midY(int y, int h) {
        return y + (h - lineH()) / 2;
    }

    
    public static int baselineOfHead(int headTopY) {
        if (custom() && H2().ready() && BODY().ready()) {
            return headTopY + H2().ascent() - BODY().ascent();
        }
        return headTopY + 1;
    }



    public static void text(DrawContext ctx, String s, int x, int y, int color) {
        if (custom() && BODY().draw(ctx, s, x, y, color)) return;
        ctx.drawText(font(), ui(s), x, y, color, false);
    }

    public static void textRight(DrawContext ctx, String s, int rightX, int y, int color) {
        if (custom() && BODY().drawRight(ctx, s, rightX, y, color)) return;
        ctx.drawText(font(), ui(s), rightX - width(s), y, color, false);
    }

    public static void textCentered(DrawContext ctx, String s, int cx, int y, int color) {
        if (custom() && BODY().drawCentered(ctx, s, cx, y, color)) return;
        ctx.drawText(font(), ui(s), cx - width(s) / 2, y, color, false);
    }

    
    public static void valueRight(DrawContext ctx, String s, int rightX, int y, int color) {
        if (custom() && VALUE().drawRight(ctx, s, rightX, y, color)) return;
        ctx.drawText(font(), ui(s), rightX - width(s), y, color, false);
    }

    public static void small(DrawContext ctx, String s, int x, int y, int color) {
        if (custom() && SMALL().draw(ctx, s, x, y, color)) return;
        ctx.drawText(font(), ui(s), x, y, color, false);
    }

    public static void textShadow(DrawContext ctx, String s, int x, int y, int color) {
        if (custom() && BODY().ready()) {
            BODY().draw(ctx, s, x + 1, y + 1, 0x99000000);
            BODY().draw(ctx, s, x, y, color);
            return;
        }
        ctx.drawText(font(), ui(s), x, y, color, true);
    }

    public static void head(DrawContext ctx, String s, int x, int y, int color) {
        if (custom() && H2().draw(ctx, s, x, y, color)) return;
        ctx.drawText(font(), display(s), x, y, color, false);
    }

    public static void headCentered(DrawContext ctx, String s, int cx, int y, int color) {
        if (custom() && H2().drawCentered(ctx, s, cx, y, color)) return;
        ctx.drawText(font(), display(s), cx - widthDisplay(s) / 2, y, color, false);
    }

    public static void brand(DrawContext ctx, String s, int x, int y, int color) {
        if (custom() && H1().draw(ctx, s, x, y, color)) return;
        ctx.drawText(font(), display(s), x, y, color, false);
    }

    public static void name(DrawContext ctx, String s, int x, int y, int color) {
        if (custom() && NAME().draw(ctx, s, x, y, color)) return;
        ctx.drawText(font(), display(s), x, y, color, false);
    }

    public static void textCenteredOutlined(DrawContext ctx, String s, int cx, int y,
                                            int color, int outlineAlpha) {
        if (custom() && BODY().drawCenteredOutlined(ctx, s, cx, y, color, outlineAlpha)) return;
        int ox = cx - width(s) / 2;
        int outline = (Math.max(0, Math.min(255, outlineAlpha)) << 24);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                ctx.drawText(font(), ui(s), ox + dx, y + dy, outline, false);
            }
        }
        ctx.drawText(font(), ui(s), ox, y, color, false);
    }

    
    public static void sectionLabel(DrawContext ctx, String s, int x, int y, int color) {
        String up = s.toUpperCase(java.util.Locale.ROOT);
        TpFont f = MICRO();
        boolean own = custom() && f.ready();
        int cx = x;
        for (int i = 0; i < up.length(); i++) {
            String ch = String.valueOf(up.charAt(i));
            if (own) {
                f.draw(ctx, ch, cx, y, color);
                cx += f.width(ch) + 1;
            } else {
                ctx.drawText(font(), ui(ch), cx, y, color, false);
                cx += font().getWidth(ui(ch)) + 1;
            }
        }
    }



    public static void roundRect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        Shapes.roundRect(ctx, x, y, w, h, r, color);
    }

    public static void roundRectBordered(DrawContext ctx, int x, int y, int w, int h, int r,
                                         int fill, int border) {
        roundRect(ctx, x, y, w, h, r, fill);
        roundOutline(ctx, x, y, w, h, r, border);
    }

    public static void roundOutline(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        Shapes.roundOutline(ctx, x, y, w, h, r, 1, color);
    }

    
    public static void accentSurface(DrawContext ctx, int x, int y, int w, int h, int r, int base) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        Shapes.roundRect(ctx, x, y, w, h, r, base);
        for (int i = 0; i < h; i++) {
            double f = i / (double) Math.max(1, h - 1);
            int a = (int) Math.round(20 * (1 - f) * (color_alpha(base) / 255.0));
            if (a <= 0) continue;
            int inset = (int) Math.ceil(arcInset(i, h, r));
            int x0 = x + inset, x1 = x + w - inset;
            if (x1 > x0) ctx.fill(x0, y + i, x1, y + i + 1, alpha((a << 24) | 0xFFFFFF));
        }
    }

    private static int color_alpha(int c) {
        return c >>> 24;
    }

    
    private static double arcInset(int dy, int h, int r) {
        if (r <= 0) return 0;
        double yc = dy + 0.5;
        if (yc < r) {
            double d = r - yc;
            return r - Math.sqrt(Math.max(0, r * (double) r - d * d));
        } else if (yc > h - r) {
            double d = yc - (h - r);
            return r - Math.sqrt(Math.max(0, r * (double) r - d * d));
        }
        return 0;
    }

    public static void vGradient(DrawContext ctx, int x, int y, int w, int h, int top, int bottom) {
        if (h <= 0) return;
        int denom = (h - 1 == 0) ? 1 : h - 1;
        for (int i = 0; i < h; i++) {
            ctx.fill(x, y + i, x + w, y + i + 1, alpha(lerpColor(top, bottom, i / (double) denom)));
        }
    }

    
    public static void shadow(DrawContext ctx, int x, int y, int w, int h, int r, int layers) {
        for (int i = layers; i >= 1; i--) {
            int a = (int) (30.0 * (1.0 - (double) i / (layers + 1)));
            if (a <= 0) continue;
            Shapes.roundRect(ctx, x - i, y - i + 3, w + i * 2, h + i * 2, r + i, (a << 24));
        }
    }

    
    
    public static void card(DrawContext ctx, int x, int y, int w, int h, int r) {
        roundRect(ctx, x, y, w, h, r, opSurface(card()));
    }

    public static void card(DrawContext ctx, int x, int y, int w, int h) {
        card(ctx, x, y, w, h, cardRadius());
    }

    
    public static void pill(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        accentSurface(ctx, x, y, w, h, r, color);
    }

    public static void disc(DrawContext ctx, int cx, int cy, int radius, int color) {
        Shapes.circle(ctx, cx, cy, radius, color);
    }

    
    public static void glassPanel(DrawContext ctx, int x, int y, int w, int h, int r) {
        roundRect(ctx, x, y, w, h, r, op(bg()));
    }



    
    public static boolean pushPress(DrawContext ctx, int x, int y, int w, int h, double press) {
        if (press <= 0.001) return false;
        float s = (float) (1.0 - 0.03 * press);
        IconBackend.pushPopup(ctx, x + w / 2f, y + h / 2f, s, 0f);
        return true;
    }

    public static void popPress(DrawContext ctx) {
        IconBackend.popMatrix(ctx);
    }



    public static int lerpColor(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int aa = (a >>> 24), ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24), br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int ra = (int) (aa + (ba - aa) * t);
        int rr = (int) (ar + (br - ar) * t);
        int rg = (int) (ag + (bg - ag) * t);
        int rb = (int) (ab + (bb - ab) * t);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }

    public static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0xFFFFFF);
    }

    
    public static int fade(int color, double f) {
        int a = (int) ((color >>> 24) * Math.max(0, Math.min(1, f)));
        return (a << 24) | (color & 0xFFFFFF);
    }
}
