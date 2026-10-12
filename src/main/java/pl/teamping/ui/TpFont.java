package pl.teamping.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


public final class TpFont {

    private static final String UI_TTF = "assets/teamping/font/ui.ttf";
    private static final String DISPLAY_TTF = "assets/teamping/font/display.ttf";

    
    private static final int OVER = 2;

    private static int COUNTER = 0;

    private static final Map<String, TpFont> POOL = new LinkedHashMap<>();

    static {
        try { System.setProperty("java.awt.headless", "true"); } catch (Throwable ignored) {}
    }

    
    public static TpFont ui(float size, float embolden) {
        return get(UI_TTF, size, embolden);
    }

    
    public static TpFont display(float size, float embolden) {
        return get(DISPLAY_TTF, size, embolden);
    }

    private static TpFont get(String res, float size, float embolden) {
        String key = res + "/" + size + "/" + embolden;
        TpFont f = POOL.get(key);
        if (f == null) {
            f = new TpFont(res, size, embolden);
            POOL.put(key, f);
        }
        return f;
    }

    private final String res;
    private final float size;
    private final float embolden;

    private boolean loaded = false, ok = false;
    private Font base;

    
    private int k = 0;
    private Font font;
    private FontMetrics fm;
    private float stroke;
    private int ascent, descent, pad;

    
    
    private record Glyph(Identifier id, int w, int h, int adv, int off) {}

    private final Map<Long, Glyph> glyphs = new HashMap<>();
    
    private final Map<Long, Glyph> outlined = new HashMap<>();

    private TpFont(String res, float size, float embolden) {
        this.res = res;
        this.size = size;
        this.embolden = embolden;
    }



    private static int guiScale() {
        return Math.max(1, (int) Math.round(Theme.rasterScale()));
    }

    private void load() {
        if (loaded) return;
        loaded = true;
        try (InputStream in = TpFont.class.getClassLoader().getResourceAsStream(res)) {
            if (in == null) return;
            base = Font.createFont(Font.TRUETYPE_FONT, in);
            ok = true;
        } catch (Throwable t) {
            ok = false;
        }
    }

    
    private record State(Font font, FontMetrics fm, float stroke, int ascent, int descent, int pad) {}

    
    private final Map<Integer, State> states = new HashMap<>();

    
    private boolean ensure() {
        load();
        if (!ok) return false;
        int want = Math.max(2, Math.min(24, guiScale() * OVER));
        if (want == k && font != null) return true;
        try {
            State st = states.get(want);
            if (st == null) {
                float px = size * want;
                Font f = base.deriveFont(Font.PLAIN, px);
                float str = embolden * px;

                BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = tmp.createGraphics();
                hints(g);
                FontMetrics m = g.getFontMetrics(f);
                g.dispose();

                st = new State(f, m, str, m.getAscent(), m.getDescent(),
                        Math.max(2, want + (int) Math.ceil(str)));
                states.put(want, st);
            }
            k = want;
            font = st.font();
            fm = st.fm();
            stroke = st.stroke();
            ascent = st.ascent();
            descent = st.descent();
            pad = st.pad();
            return true;
        } catch (Throwable t) {
            ok = false;
            return false;
        }
    }

    private static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    
    private int advance(int cp) {
        return fm.charWidth(cp) + Math.round(stroke * 0.5f);
    }

    
    private Glyph glyph(int cp) {
        return glyph(cp, false);
    }

    
    private Glyph glyph(int cp, boolean outline) {
        Map<Long, Glyph> cache = outline ? outlined : glyphs;

        long key = ((long) k << 32) | (cp & 0xFFFFFFFFL);
        Glyph g = cache.get(key);
        if (g != null) return g;

        String s = new String(Character.toChars(cp));
        int adv = advance(cp);
        int ring = outline ? Math.max(1, k) : 0;
        int off = pad + ring;
        int w = Math.max(1, adv + off * 2);
        int h = Math.max(1, ascent + descent + off * 2);

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr = img.createGraphics();
        hints(gr);
        gr.setFont(font);

        float ox = off + (stroke > 0.01f ? stroke * 0.5f : 0f);
        float oy = off + ascent;
        GlyphVector gv = font.createGlyphVector(gr.getFontRenderContext(), s);
        Shape shape = gv.getOutline(ox, oy);

        if (outline) {

            gr.setColor(Color.BLACK);
            gr.setStroke(new BasicStroke(ring * 2f + (stroke > 0.01f ? stroke : 0f),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            gr.draw(shape);
        }

        gr.setColor(Color.WHITE);
        if (stroke > 0.01f) {

            gr.fill(shape);
            gr.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            gr.draw(shape);
        } else {
            gr.fill(shape);
        }
        gr.dispose();

        int[] argb = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int px = img.getRGB(x, y);
                int a = (px >>> 24) & 0xFF;

                argb[y * w + x] = a == 0 ? 0 : ((a << 24) | (px & 0x00FFFFFF));
            }
        }

        Identifier id = IconBackend.uploadGlyph("glyph/g" + (COUNTER++), argb, w, h);
        g = new Glyph(id, w, h, adv, off);
        cache.put(key, g);
        return g;
    }



    public boolean ready() { return ensure(); }

    
    private int widthQ(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            w += advance(cp);
        }
        return w;
    }

    
    public int width(String s) {
        if (s == null || s.isEmpty() || !ensure()) return 0;
        return Math.round(widthQ(s) / (float) k);
    }

    
    public int height() {
        if (!ensure()) return 9;
        return Math.round((ascent + descent) / (float) k);
    }

    
    public int ascent() {
        if (!ensure()) return 7;
        return Math.round(ascent / (float) k);
    }



    
    public boolean draw(DrawContext ctx, String s, int x, int y, int color) {
        return at(ctx, s, x, y, color, 0);
    }

    public boolean drawCentered(DrawContext ctx, String s, int cx, int y, int color) {
        return at(ctx, s, cx, y, color, 1);
    }

    public boolean drawRight(DrawContext ctx, String s, int rightX, int y, int color) {
        return at(ctx, s, rightX, y, color, 2);
    }

    
    private boolean at(DrawContext ctx, String s, int x, int y, int color, int mode) {
        if (!ensure()) return false;
        color = Theme.alpha(color);
        if (s == null || s.isEmpty() || (color >>> 24) == 0) return true;
        try {
            int x0 = switch (mode) {
                case 1 -> x * k - widthQ(s) / 2;
                case 2 -> x * k - widthQ(s);
                default -> x * k;
            };
            IconBackend.pushScale(ctx, 1f / k);
            run(ctx, s, x0, y * k, color, false);
            IconBackend.popMatrix(ctx);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    
    public boolean drawCenteredOutlined(DrawContext ctx, String s, int cx, int y, int color, int outlineAlpha) {
        if (!ensure()) return false;
        color = Theme.alpha(color);
        if (s == null || s.isEmpty() || (color >>> 24) == 0) return true;
        try {
            IconBackend.pushScale(ctx, 1f / k);
            run(ctx, s, cx * k - widthQ(s) / 2, y * k, color, true);
            IconBackend.popMatrix(ctx);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    
    private void run(DrawContext ctx, String s, int xq, int yq, int color, boolean outline) {
        int pen = xq;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == ' ') { pen += advance(cp); continue; }
            Glyph g = glyph(cp, outline);
            IconBackend.drawRegion(ctx, g.id(), pen - g.off(), yq - g.off(), g.w(), g.h(),
                    0f, 0f, g.w(), g.h(), g.w(), g.h(), color);
            pen += g.adv();
        }
    }
}
