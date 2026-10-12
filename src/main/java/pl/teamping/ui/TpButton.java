package pl.teamping.ui;

import net.minecraft.client.gui.DrawContext;


public class TpButton extends TpWidget {

    public static final int H = 20;

    private final Runnable action;
    private final boolean primary;
    private boolean danger = false;

    public TpButton(int x, int y, int w, String label, boolean primary, Runnable action) {
        super(x, y, w, H, label);
        this.primary = primary;
        this.action = action;
    }

    
    public TpButton danger() {
        this.danger = true;
        return this;
    }

    
    public void setLabel(String label) {
        setMessage(net.minecraft.text.Text.literal(label));
    }

    @Override
    protected void draw(DrawContext ctx, int mouseX, int mouseY) {
        int x = getX(), y = getY(), w = getWidth();
        double t = hot.get();
        String label = getMessage().getString();

        if (primary) {

            if (t > 0.01) {
                Theme.roundRect(ctx, x - 1, y - 1, w + 2, H + 2, 7, c(Theme.fade(Theme.accentGlow(), t)));
            }
            Theme.accentSurface(ctx, x, y, w, H, 6, c(Theme.lerpColor(Theme.accent(), Theme.accentHover(), t)));
            Theme.textCentered(ctx, label, x + w / 2, Theme.midY(y, H), c(0xFFFFFFFF));
        } else if (danger) {
            Theme.roundRectBordered(ctx, x, y, w, H, 6,
                    c(Theme.fade(Theme.DANGER, 0.10 + 0.10 * t)),
                    c(Theme.withAlpha(Theme.DANGER, (int) (0x66 + 0x50 * t))));

            Theme.textCentered(ctx, label, x + w / 2, Theme.midY(y, H),
                    c(Theme.lerpColor(Theme.DANGER, 0xFFFFF3EE, t)));
        } else {
            Theme.roundRectBordered(ctx, x, y, w, H, 6,
                    c(Theme.lerpColor(0x00000000, Theme.HOVER, t)), c(Theme.HAIRLINE_STRONG));
            Theme.textCentered(ctx, label, x + w / 2, Theme.midY(y, H),
                    c(Theme.lerpColor(Theme.TEXT_DIM, Theme.TEXT, t)));
        }
    }

    @Override
    protected void onPress(double mouseX, double mouseY) {
        if (action != null) action.run();
    }
}
