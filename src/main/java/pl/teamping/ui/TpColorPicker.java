package pl.teamping.ui;

import net.minecraft.client.gui.DrawContext;

public class TpColorPicker extends TpWidget {

    private static final int HUE_W = 14;
    private static final int GAP = 8;

    private float hue, sat, val;
    private int grab = 0;

    public TpColorPicker(int x, int y, int w, int h, int rgb) {
        super(x, y, w, h, "Kolor");
        float[] hsv = rgbToHsv(rgb);
        hue = hsv[0]; sat = hsv[1]; val = hsv[2];
    }

    public int getColor() {
        return 0xFF000000 | (hsvToRgb(hue, sat, val) & 0xFFFFFF);
    }

    public void setColor(int rgb) {
        float[] hsv = rgbToHsv(rgb);
        hue = hsv[0]; sat = hsv[1]; val = hsv[2];
    }

    public String hex() {
        return String.format(java.util.Locale.ROOT, "#%06X", getColor() & 0xFFFFFF);
    }

    private int sqW() { return getWidth() - HUE_W - GAP; }
    private int sqH() { return getHeight(); }

    @Override
    protected void draw(DrawContext ctx, int mouseX, int mouseY) {
        int sx = getX(), sy = getY(), sw = sqW(), sh = sqH();
        if (sw <= 2 || sh <= 2) return;

        Theme.roundRect(ctx, sx, sy, sw, sh, 4, c(0xFF000000 | hsvToRgb(hue, 1f, 1f)));
        Icons.drawRegion(ctx, Gradients.whiteFade(), sx, sy, sw, sh, 0f, 0f, 256, 1, 256, 1, c(0xFFFFFFFF));
        Icons.drawRegion(ctx, Gradients.blackFade(), sx, sy, sw, sh, 0f, 0f, 1, 256, 1, 256, c(0xFFFFFFFF));

        int hx = sx + Math.round(sat * sw);
        int hy = sy + Math.round((1f - val) * sh);
        Shapes.circle(ctx, hx, hy, 4, c(0xFFFFFFFF));
        Shapes.circle(ctx, hx, hy, 3, c(0xFF000000 | hsvToRgb(hue, sat, val)));

        int bx = sx + sw + GAP;
        Icons.drawRegion(ctx, Gradients.hueStrip(), bx, sy, HUE_W, sh, 0f, 0f, 1, 256, 1, 256, c(0xFFFFFFFF));
        int by = sy + Math.round(hue * sh);
        Theme.roundRect(ctx, bx - 2, by - 2, HUE_W + 4, 4, 2, c(0xFFFFFFFF));
    }

    @Override
    protected void onPress(double mouseX, double mouseY) {
        int bx = getX() + sqW() + GAP;
        grab = mouseX >= bx - 2 ? 2 : 1;
        apply(mouseX, mouseY);
    }

    @Override
    protected void cDrag(double mouseX, double mouseY, double dx, double dy) {
        apply(mouseX, mouseY);
    }

    private void apply(double mx, double my) {
        if (grab == 2) {
            hue = clamp01((float) (my - getY()) / sqH());
        } else {
            sat = clamp01((float) (mx - getX()) / sqW());
            val = 1f - clamp01((float) (my - getY()) / sqH());
        }
        fireChanged();
    }

    private static float clamp01(float v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    public static int hsvToRgb(float h, float s, float v) {
        float r = 0, g = 0, b = 0;
        int i = (int) Math.floor(h * 6) % 6;
        if (i < 0) i += 6;
        float f = h * 6 - (float) Math.floor(h * 6);
        float p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        switch (i) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    public static float[] rgbToHsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h = 0;
        if (d > 1e-5) {
            if (max == r) h = ((g - b) / d) % 6;
            else if (max == g) h = (b - r) / d + 2;
            else h = (r - g) / d + 4;
            h /= 6;
            if (h < 0) h += 1;
        }
        float s = max <= 0 ? 0 : d / max;
        return new float[] {h, s, max};
    }
}
