package pl.teamping.ui;

import net.minecraft.client.gui.DrawContext;

public class TpToggle extends TpWidget {

    public static final int H = 16;
    private static final int PILL_W = 26;
    private static final int PILL_H = 14;
    private static final int KNOB = 10;
    private static final int INSET = 2;

    private boolean value;
    private String hint;
    private final Anim knob;

    public TpToggle(int x, int y, int w, String label, boolean value) {
        super(x, y, w, H, label);
        this.value = value;
        this.knob = new Anim(value ? 1 : 0, Anim.FAST);
    }

    public TpToggle hint(String h) {
        this.hint = h;
        return this;
    }

    public boolean getValue() {
        return value;
    }

    @Override
    protected void draw(DrawContext ctx, int mouseX, int mouseY) {
        knob.set(value ? 1 : 0);
        knob.update(dt);

        int x = getX(), y = getY(), w = getWidth();
        double t = Anim.easeOut(knob.get());
        double h = hot.get();

        String label = getMessage().getString();
        Theme.text(ctx, label, x, Theme.midY(y, H),
                c(Theme.lerpColor(Theme.TEXT_DIM, Theme.TEXT, h)));
        if (hint != null) {
            Theme.text(ctx, hint, x + Theme.width(label) + 8, Theme.midY(y, H), c(Theme.TEXT_FAINT));
        }

        int px = x + w - PILL_W;
        int py = y + (H - PILL_H) / 2;
        Theme.roundRect(ctx, px, py, PILL_W, PILL_H, PILL_H / 2,
                c(Theme.lerpColor(Theme.track(), Theme.accent(), t)));

        int travel = PILL_W - KNOB - INSET * 2;
        int kx = px + INSET + (int) Math.round(travel * t);
        Theme.roundRect(ctx, kx, py + INSET, KNOB, KNOB, KNOB / 2, c(0xFFFFFFFF));
    }

    @Override
    protected void onPress(double mouseX, double mouseY) {
        value = !value;
        fireChanged();
    }
}
