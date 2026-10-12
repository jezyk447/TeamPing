package pl.teamping.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;


public final class PanelBlur {

    private PanelBlur() {}

    
    private static boolean warned = false;

    
    private static final int TAP_STEP = 2;
    
    private static final int TAP_REACH = 4;

    
    private static final float[][] TAPS;
    private static final double[] WEIGHTS;
    static {
        java.util.List<float[]> tl = new java.util.ArrayList<>();
        java.util.List<Double> wl = new java.util.ArrayList<>();
        double sigma = TAP_REACH * TAP_STEP / 2.0;
        double lim = (double) TAP_REACH * TAP_REACH;
        for (int dy = -TAP_REACH; dy <= TAP_REACH; dy++) {
            for (int dx = -TAP_REACH; dx <= TAP_REACH; dx++) {
                if (dx * dx + dy * dy > lim) continue;
                double ox = dx * TAP_STEP, oy = dy * TAP_STEP;
                tl.add(new float[]{(float) (ox + 0.5), (float) (oy + 0.5)});
                wl.add(Math.exp(-(ox * ox + oy * oy) / (2.0 * sigma * sigma)));
            }
        }
        int count = tl.size();
        float[][] taps = tl.toArray(new float[0][]);
        double[] ws = new double[count];
        for (int i = 0; i < count; i++) ws[i] = wl.get(i);

        for (int i = 1; i < count; i++) {
            for (int j = i; j > 0 && ws[j] > ws[j - 1]; j--) {
                double tw = ws[j]; ws[j] = ws[j - 1]; ws[j - 1] = tw;
                float[] tt = taps[j]; taps[j] = taps[j - 1]; taps[j - 1] = tt;
            }
        }
        TAPS = taps;
        WEIGHTS = ws;
    }

    
    public static boolean draw(DrawContext ctx, double x, double y, double w, double h, double radius) {
        if (w <= 1 || h <= 1) return false;

        int fbW = IconBackend.framebufferWidth();
        int fbH = IconBackend.framebufferHeight();
        int scaledW = ctx.getScaledWindowWidth();
        int scaledH = ctx.getScaledWindowHeight();
        if (fbW <= 0 || fbH <= 0 || scaledW <= 0 || scaledH <= 0) return false;


        double sx = fbW / (double) scaledW;
        double sy = fbH / (double) scaledH;


        Identifier tex = IconBackend.captureScreen(0, 0, fbW, fbH);
        if (tex == null) {
            if (!warned) {
                warned = true;
            }
            return false;
        }

        double g = Theme.GLOBAL_ALPHA;
        if (g <= 0.004) return true;


        double r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        int rows = (int) Math.ceil(r);
        strip(ctx, tex, sx, sy, fbW, fbH, x, y + r, w, h - 2 * r, g);
        for (int i = 0; i < rows; i++) {
            double y0 = i, y1 = Math.min(i + 1.0, r);
            if (y1 <= y0) break;
            double dy = r - y0;
            double inset = r - Math.sqrt(Math.max(0.0, r * r - dy * dy));
            double bx = x + inset, bw = w - 2 * inset;
            strip(ctx, tex, sx, sy, fbW, fbH, bx, y + y0, bw, y1 - y0, g);
            strip(ctx, tex, sx, sy, fbW, fbH, bx, y + h - y1, bw, y1 - y0, g);
        }
        return true;
    }

    
    private static void strip(DrawContext ctx, Identifier tex, double sx, double sy,
                              int fbW, int fbH,
                              double x, double y, double w, double h, double g) {
        if (w <= 0 || h <= 0) return;
        int ix = (int) Math.round(x), iy = (int) Math.round(y);
        int iw = (int) Math.round(x + w) - ix, ih = (int) Math.round(y + h) - iy;
        if (iw <= 0 || ih <= 0) return;


        double u0 = ix * sx;
        double u1 = (ix + iw) * sx;
        double t0 = iy * sy;
        double t1 = (iy + ih) * sy;
        int ru = (int) Math.round(u1 - u0);
        int rv = (int) Math.round(t0 - t1);
        if (ru <= 0 || rv >= 0) return;
        float uu = (float) u0;
        float vv = (float) (fbH - t0);

        double cum = 0;
        for (int i = 0; i < TAPS.length; i++) {
            cum += WEIGHTS[i];
            int a = (int) Math.round(255.0 * WEIGHTS[i] / cum);
            if (a <= 0) continue;
            IconBackend.drawRegion(ctx, tex, ix, iy, iw, ih,
                    uu + TAPS[i][0], vv + TAPS[i][1], ru, rv, fbW, fbH, (a << 24) | 0xFFFFFF);
        }


        if (g < 0.999) {
            int a = (int) Math.round(255.0 * (1.0 - g));
            if (a > 0) {
                IconBackend.drawRegion(ctx, tex, ix, iy, iw, ih,
                        uu, vv, ru, rv, fbW, fbH, (a << 24) | 0xFFFFFF);
            }
        }
    }
}
