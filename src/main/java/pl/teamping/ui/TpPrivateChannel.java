package pl.teamping.ui;

import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;

public class TpPrivateChannel extends TpWidget {

    public static final int H = 32;
    public static final int MAX_DIGITS = 4;
    private static final int BADGE = 20;
    private static final int FIELD_W = 44, FIELD_H = 18;
    private static final int BTN_W = 52;

    private final boolean active;
    private final String sub;
    private final Consumer<String> onJoin;
    private final Anim glow;

    private String code;
    private boolean focused;

    public TpPrivateChannel(int x, int y, int w, String code, boolean focused, boolean active,
                            String sub, Consumer<String> onJoin) {
        super(x, y, w, H, "Kanał prywatny");
        this.code = code == null ? "" : code;
        this.focused = focused;
        this.active = active;
        this.sub = sub;
        this.onJoin = onJoin;
        this.glow = new Anim(active ? 1 : 0, Anim.MED);
    }

    public String getCode() { return code; }
    public boolean isFocused() { return focused; }
    public void unfocus() { focused = false; }

    public boolean type(char chr) {
        if (!focused) return false;
        if (chr >= '0' && chr <= '9' && code.length() < MAX_DIGITS) code += chr;
        return true;
    }

    public boolean backspace() {
        if (!focused) return false;
        if (!code.isEmpty()) code = code.substring(0, code.length() - 1);
        return true;
    }

    public boolean enter() {
        if (!focused) return false;
        join();
        return true;
    }

    private void join() {
        if (code.isEmpty()) return;
        focused = false;
        onJoin.accept(code);
    }

    private int fieldX() { return getX() + getWidth() - 8 - BTN_W - 6 - FIELD_W; }
    private int btnX() { return getX() + getWidth() - 8 - BTN_W; }

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
        Theme.roundRect(ctx, bx, by, BADGE, BADGE, 6, c(Theme.lerpColor(Theme.track(), Theme.accent(), a)));
        Icons.draw(ctx, Icons.SHIELD, bx + 4, by + 4, BADGE - 8, c(a > 0.5 ? 0xFFFFFFFF : Theme.TEXT_DIM));

        int tx = bx + BADGE + 8;
        Theme.text(ctx, "Kanał prywatny", tx, y + 6, c(Theme.lerpColor(Theme.TEXT_DIM, Theme.TEXT, Math.max(t, a))));
        if (sub != null && !sub.isEmpty()) {
            Theme.small(ctx, sub, tx, y + 18, c(a > 0.5 ? Theme.accentText() : Theme.TEXT_FAINT));
        }

        int fx = fieldX(), fy = y + (H - FIELD_H) / 2;
        Theme.roundRectBordered(ctx, fx, fy, FIELD_W, FIELD_H, 6,
                c(focused ? Theme.accentSoft() : Theme.track()),
                c(focused ? Theme.accent() : Theme.HAIRLINE_STRONG));
        boolean empty = code.isEmpty();
        String shown = empty ? (focused ? "" : "kod") : code;
        Theme.textCentered(ctx, shown, fx + FIELD_W / 2, Theme.midY(fy, FIELD_H),
                c(empty ? Theme.TEXT_FAINT : Theme.TEXT));
        if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cx = fx + FIELD_W / 2 + Theme.width(code) / 2 + 1;
            ctx.fill(cx, fy + 4, cx + 1, fy + FIELD_H - 4, c(Theme.accentText()));
        }

        int kx = btnX(), ky = y + (H - FIELD_H) / 2;
        boolean can = !empty;
        boolean btnHot = can && mouseX >= kx && mouseX < kx + BTN_W && mouseY >= ky && mouseY < ky + FIELD_H;
        if (can) {
            Theme.accentSurface(ctx, kx, ky, BTN_W, FIELD_H, 6,
                    c(btnHot ? Theme.accentHover() : Theme.accent()));
            Theme.textCentered(ctx, "Dołącz", kx + BTN_W / 2, Theme.midY(ky, FIELD_H), c(0xFFFFFFFF));
        } else {
            Theme.roundRectBordered(ctx, kx, ky, BTN_W, FIELD_H, 6, c(0x00000000), c(Theme.HAIRLINE_STRONG));
            Theme.textCentered(ctx, "Dołącz", kx + BTN_W / 2, Theme.midY(ky, FIELD_H), c(Theme.TEXT_FAINT));
        }
    }

    @Override
    protected void onPress(double mouseX, double mouseY) {
        int kx = btnX();
        if (mouseX >= kx && mouseX < kx + BTN_W) {
            join();
            return;
        }

        focused = true;
    }
}
