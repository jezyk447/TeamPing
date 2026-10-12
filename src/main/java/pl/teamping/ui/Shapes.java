package pl.teamping.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;


public final class Shapes {

    private Shapes() {}

    
    private static final int OVER = 2;

    private static int k = 0;
    private static final Map<Integer, Identifier> DISCS = new HashMap<>();
    private static final Map<Integer, Identifier> RINGS = new HashMap<>();

    private static int COUNTER = 0;

    static {
        try { System.setProperty("java.awt.headless", "true"); } catch (Throwable ignored) {}
    }

    private static int guiScale() {
        return Math.max(1, (int) Math.round(Theme.rasterScale()));
    }

    
    private static int k() {
        k = Math.max(2, Math.min(16, guiScale() * OVER));
        return k;
    }

    private static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    private static Identifier upload(BufferedImage img, String name) {
        int w = img.getWidth(), h = img.getHeight();
        int[] argb = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = (img.getRGB(x, y) >>> 24) & 0xFF;
                argb[y * w + x] = a == 0 ? 0 : ((a << 24) | 0x00FFFFFF);
            }
        }
        return IconBackend.uploadGlyph(name, argb, w, h);
    }

    
    private static Identifier disc(int rq) {
        Identifier id = DISCS.get(rq);
        if (id != null) return id;
        int d = Math.max(2, rq * 2);
        BufferedImage img = new BufferedImage(d, d, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        hints(g);
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Float(0f, 0f, d, d));
        g.dispose();
        id = upload(img, "shape/disc" + (COUNTER++));
        DISCS.put(rq, id);
        return id;
    }

    private static final Map<Integer, Identifier> ANTI = new HashMap<>();

    
    private static Identifier antiDisc(int rq) {
        Identifier id = ANTI.get(rq);
        if (id != null) return id;
        int d = Math.max(2, rq * 2);
        BufferedImage img = new BufferedImage(d, d, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        hints(g);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, d, d);
        g.setComposite(AlphaComposite.Clear);
        g.fill(new Ellipse2D.Float(0f, 0f, d, d));
        g.dispose();
        id = upload(img, "shape/anti" + (COUNTER++));
        ANTI.put(rq, id);
        return id;
    }

    
    public static void cornerCutout(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        color = Theme.alpha(color);
        if (w <= 0 || h <= 0 || r <= 0 || (color >>> 24) == 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        int q = k();
        int X = x * q, Y = y * q, W = w * q, H = h * q, R = r * q;
        try {
            IconBackend.pushScale(ctx, 1f / q);
            corners(ctx, antiDisc(R), X, Y, W, H, R, color);
            IconBackend.popMatrix(ctx);
        } catch (Throwable ignored) {
        }
    }

    
    private static Identifier ring(int rq, int tq) {
        int key = (rq << 8) | (tq & 0xFF);
        Identifier id = RINGS.get(key);
        if (id != null) return id;
        int d = Math.max(2, rq * 2);
        BufferedImage img = new BufferedImage(d, d, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        hints(g);
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Float(0f, 0f, d, d));
        int inner = Math.max(0, d - tq * 2);
        if (inner > 0) {
            g.setComposite(AlphaComposite.Clear);
            g.fill(new Ellipse2D.Float(tq, tq, inner, inner));
        }
        g.dispose();
        id = upload(img, "shape/ring" + (COUNTER++));
        RINGS.put(key, id);
        return id;
    }

    
    private static void corners(DrawContext ctx, Identifier mask, int X, int Y, int W, int H, int R, int color) {
        int d = R * 2;
        IconBackend.drawRegion(ctx, mask, X, Y, R, R, 0f, 0f, R, R, d, d, color);
        IconBackend.drawRegion(ctx, mask, X + W - R, Y, R, R, R, 0f, R, R, d, d, color);
        IconBackend.drawRegion(ctx, mask, X, Y + H - R, R, R, 0f, R, R, R, d, d, color);
        IconBackend.drawRegion(ctx, mask, X + W - R, Y + H - R, R, R, R, R, R, R, d, d, color);
    }

    
    public static void roundRect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        color = Theme.alpha(color);
        if (w <= 0 || h <= 0 || (color >>> 24) == 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) { ctx.fill(x, y, x + w, y + h, color); return; }

        int q = k();
        int X = x * q, Y = y * q, W = w * q, H = h * q, R = r * q;

        try {
            IconBackend.pushScale(ctx, 1f / q);
            ctx.fill(X + R, Y, X + W - R, Y + H, color);
            ctx.fill(X, Y + R, X + R, Y + H - R, color);
            ctx.fill(X + W - R, Y + R, X + W, Y + H - R, color);
            corners(ctx, disc(R), X, Y, W, H, R, color);
            IconBackend.popMatrix(ctx);
        } catch (Throwable t) {
            ctx.fill(x, y, x + w, y + h, color);
        }
    }

    
    public static void roundOutline(DrawContext ctx, int x, int y, int w, int h, int r, int t, int color) {
        color = Theme.alpha(color);
        if (w <= 0 || h <= 0 || t <= 0 || (color >>> 24) == 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));

        int q = k();
        int X = x * q, Y = y * q, W = w * q, H = h * q, R = r * q, T = t * q;

        try {
            IconBackend.pushScale(ctx, 1f / q);
            ctx.fill(X + R, Y, X + W - R, Y + T, color);
            ctx.fill(X + R, Y + H - T, X + W - R, Y + H, color);
            ctx.fill(X, Y + R, X + T, Y + H - R, color);
            ctx.fill(X + W - T, Y + R, X + W, Y + H - R, color);
            if (R > 0) corners(ctx, ring(R, T), X, Y, W, H, R, color);
            IconBackend.popMatrix(ctx);
        } catch (Throwable ignored) {
        }
    }

    
    public static void circle(DrawContext ctx, int cx, int cy, int radius, int color) {
        roundRect(ctx, cx - radius, cy - radius, radius * 2, radius * 2, radius, color);
    }
}
