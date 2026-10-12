package pl.teamping.ui;

import net.minecraft.client.gui.DrawContext;

import java.util.function.DoubleFunction;

public class TpSlider extends TpWidget {

    public static final int H = 20;
    private static final int TRACK_H = 4;
    private static final int TRACK_DY = 13;

    private static final double INF_ZONE = 0.94;
    public static final String INFINITY = "bez limitu";

    private final double min, max, step;
    private final boolean allowInfinite;
    private final DoubleFunction<String> fmt;

    private double value;
    private boolean infinite;
    private boolean dragging;

    public TpSlider(int x, int y, int w, String label, double min, double max, double step,
                    double value, boolean allowInfinite, DoubleFunction<String> fmt) {
        super(x, y, w, H, label);
        this.min = min;
        this.max = max;
        this.step = step;
        this.value = Math.max(min, Math.min(max, value));
        this.allowInfinite = allowInfinite;
        this.fmt = fmt;
    }

    public void setInfinite(boolean b) { this.infinite = b && allowInfinite; }
    public boolean isInfinite() { return infinite; }
    public double getValue() { return value; }
    public int getIntValue() { return (int) Math.round(value); }

    private int trackY() { return getY() + TRACK_DY; }

    private double toFraction() {
        if (infinite) return 1.0;
        double f = (value - min) / (max - min);
        return allowInfinite ? f * INF_ZONE : f;
    }

    @Override
    protected void draw(DrawContext ctx, int mouseX, int mouseY) {
        hot.set(isHovered() || dragging ? 1 : 0);

        int x = getX(), y = getY(), w = getWidth();
        double t = hot.get();

        Theme.text(ctx, getMessage().getString(), x, y,
                c(Theme.lerpColor(Theme.TEXT_DIM, Theme.TEXT, t)));
        String v = infinite ? INFINITY : fmt.apply(value);
        Theme.valueRight(ctx, v, x + w, y, c(Theme.accentText()));

        int ty = trackY();
        Theme.roundRect(ctx, x, ty, w, TRACK_H, TRACK_H / 2, c(Theme.track()));

        int fill = (int) Math.round(w * toFraction());
        if (fill > 0) {
            Theme.roundRect(ctx, x, ty, fill, TRACK_H, TRACK_H / 2,
                    c(Theme.lerpColor(Theme.accent(), Theme.accentHover(), t)));
        }

        int r = 4 + (int) Math.round(1.5 * t);
        int hx = x + Math.max(r, Math.min(w - r, fill));
        int hy = ty + TRACK_H / 2;
        if (t > 0.01) {
            Theme.roundRect(ctx, hx - r - 2, hy - r - 2, (r + 2) * 2, (r + 2) * 2, r + 2,
                    c(Theme.fade(Theme.accentGlow(), t)));
        }
        Theme.roundRect(ctx, hx - r, hy - r, r * 2, r * 2, r, c(0xFFFFFFFF));
    }

    private void applyFromMouse(double mouseX) {
        double f = (mouseX - getX()) / (double) getWidth();
        f = Math.max(0, Math.min(1, f));

        double before = value;
        boolean infBefore = infinite;

        if (allowInfinite && f >= INF_ZONE) {
            infinite = true;
            value = max;
        } else {
            infinite = false;
            double raw = allowInfinite ? f / INF_ZONE : f;
            raw = Math.max(0, Math.min(1, raw));
            double v = min + raw * (max - min);
            value = Math.max(min, Math.min(max, Math.round(v / step) * step));
        }
        if (value != before || infinite != infBefore) fireChanged();
    }

    @Override
    protected void onPress(double mouseX, double mouseY) {
        dragging = true;
        applyFromMouse(mouseX);
    }

    @Override
    protected void cDrag(double mouseX, double mouseY, double dx, double dy) {
        applyFromMouse(mouseX);
    }

    @Override
    protected void cRelease(double mouseX, double mouseY) {
        super.cRelease(mouseX, mouseY);
        dragging = false;
    }
}
