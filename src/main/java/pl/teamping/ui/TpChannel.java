package pl.teamping.ui;

import net.minecraft.client.gui.DrawContext;


public class TpChannel extends TpWidget {

    public static final int H = 32;
    private static final int BADGE = 20;

    private final String name;
    private final boolean active;
    private final String sub;
    private final Runnable action;

    private final Anim glow;

    public TpChannel(int x, int y, int w, String name, boolean active, String sub, Runnable action) {
        super(x, y, w, H, name);
        this.name = name;
        this.active = active;
        this.sub = sub;
        this.action = action;
        this.glow = new Anim(active ? 1 : 0, Anim.MED);
    }

    @Override
    protected void draw(DrawContext ctx, int mouseX, int mouseY) {
        glow.set(active ? 1 : 0);
        glow.update(dt);

        int x = getX(), y = getY(), w = getWidth();
        double t = hot.get();
        double a = Anim.easeOut(glow.get());

        int fill = Theme.lerpColor(
                Theme.lerpColor(Theme.surface(), Theme.surfaceHover(), t),
                Theme.accentSoft(), a);
        Theme.roundRect(ctx, x, y, w, H, 8, c(Theme.opSurface(fill)));



        int bx = x + 6, by = y + (H - BADGE) / 2;
        Theme.roundRect(ctx, bx, by, BADGE, BADGE, 6,
                c(Theme.lerpColor(Theme.track(), Theme.accent(), a)));
        String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
        Theme.textCentered(ctx, initial, bx + BADGE / 2, Theme.midY(by, BADGE),
                c(a > 0.5 ? 0xFFFFFFFF : Theme.TEXT_DIM));

        int tx = bx + BADGE + 8;
        Theme.text(ctx, name, tx, y + 6, c(Theme.lerpColor(Theme.TEXT_DIM, Theme.TEXT, Math.max(t, a))));
        if (sub != null && !sub.isEmpty()) {
            Theme.small(ctx, sub, tx, y + 18, c(a > 0.5 ? Theme.accentText() : Theme.TEXT_FAINT));
        }

        if (a > 0.02) {
            int s = 12;
            Icons.draw(ctx, Icons.CHECK_CIRCLE, x + w - s - 9, y + (H - s) / 2, s,
                    c(Theme.fade(Theme.accentText(), a)));
        }
    }

    @Override
    protected void onPress(double mouseX, double mouseY) {
        if (action != null) action.run();
    }
}
