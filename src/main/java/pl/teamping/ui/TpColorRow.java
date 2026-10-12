package pl.teamping.ui;

import net.minecraft.client.gui.DrawContext;


public class TpColorRow extends TpWidget {

    public static final int H = 22;
    private static final int DOT = 13;
    private static final int STEP = 19;

    private final String label;
    private final int[] palette;
    private final Runnable openPicker;

    private int index;
    
    private int custom;

    private final Anim[] grow;

    public TpColorRow(int x, int y, int w, String label, int[] palette, int index, int custom,
                      Runnable openPicker) {
        super(x, y, w, H, label);
        this.label = label;
        this.palette = palette;
        this.openPicker = openPicker;
        this.index = Math.max(0, Math.min(palette.length - 1, index));
        this.custom = custom;
        this.grow = new Anim[palette.length + 1];
        for (int i = 0; i < grow.length; i++) {
            grow[i] = new Anim(selected(i) ? 1 : 0, Anim.FAST);
        }
    }

    public int getIndex() {
        return index;
    }

    public int getCustom() {
        return custom;
    }

    
    public void setCustom(int argb) {
        this.custom = argb;
    }

    
    private boolean selected(int i) {
        boolean own = (custom >>> 24) != 0;
        return i == palette.length ? own : (!own && i == index);
    }

    private int count() {
        return palette.length + 1;
    }

    
    private int dotX(int i) {
        int right = getX() + getWidth();
        return right - (count() - 1 - i) * STEP - DOT / 2;
    }

    @Override
    protected void draw(DrawContext ctx, int mouseX, int mouseY) {
        Theme.text(ctx, label, getX(), Theme.midY(getY(), H), c(Theme.TEXT_DIM));

        int cy = getY() + H / 2;
        for (int i = 0; i < count(); i++) {
            boolean on = selected(i);
            boolean hov = isHovered() && Math.abs(mouseX - dotX(i)) <= STEP / 2
                    && Math.abs(mouseY - cy) <= H / 2;
            grow[i].set(on ? 1 : (hov ? 0.45 : 0));
            grow[i].update(dt);

            double g = Anim.easeOut(grow[i].get());
            int cx = dotX(i);
            int r = (int) Math.round(DOT / 2.0 + g * 1.5);

            if (g > 0.55) {

                Shapes.circle(ctx, cx, cy, r + 2, c(Theme.withAlpha(0xFFFFFF, (int) (200 * g))));
                Shapes.circle(ctx, cx, cy, r + 1, c(Theme.opSurface(Theme.card())));
            }

            if (i < palette.length) {
                Shapes.circle(ctx, cx, cy, r, c(palette[i]));
            } else if ((custom >>> 24) != 0) {
                Shapes.circle(ctx, cx, cy, r, c(0xFF000000 | (custom & 0xFFFFFF)));
            } else {
                drawCustomDot(ctx, cx, cy, r);
            }
        }
    }

    
    private void drawCustomDot(DrawContext ctx, int cx, int cy, int r) {
        Shapes.circle(ctx, cx, cy, r, c(Theme.opSurface(Theme.track())));
        Shapes.circle(ctx, cx, cy, r, c(Theme.withAlpha(0xFFFFFF, 0x2E)));
        Shapes.circle(ctx, cx, cy, r - 1, c(Theme.opSurface(Theme.track())));

        int arm = Math.max(2, r - 4);
        int col = c(0xFFFFFFFF);
        ctx.fill(cx - arm, cy - 1, cx + arm, cy + 1, col);
        ctx.fill(cx - 1, cy - arm, cx + 1, cy + arm, col);
    }

    @Override
    protected void onPress(double mouseX, double mouseY) {
        int cy = getY() + H / 2;
        for (int i = 0; i < count(); i++) {
            if (Math.abs(mouseX - dotX(i)) <= STEP / 2.0 && Math.abs(mouseY - cy) <= H / 2.0) {
                if (i == palette.length) {
                    if (openPicker != null) openPicker.run();
                } else if (i != index || (custom >>> 24) != 0) {
                    index = i;
                    custom = 0;
                    fireChanged();
                }
                return;
            }
        }
    }
}
