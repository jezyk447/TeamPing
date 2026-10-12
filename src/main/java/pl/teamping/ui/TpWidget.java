package pl.teamping.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;

public abstract class TpWidget extends CompatWidget {

    protected final Anim hot = new Anim(0, Anim.FAST);
    protected final Anim press = new Anim(0, Anim.SNAP);

    private long lastFrame = System.nanoTime();
    private double appear = 1.0;
    private boolean down = false;
    private Runnable changed;

    protected double alpha = 1.0;

    protected double dt = 0;

    protected TpWidget(int x, int y, int w, int h, String label) {
        super(x, y, w, h, Text.literal(label));
    }

    public void setAppear(double a) {
        this.appear = Anim.clamp01(a);
    }

    public void setOnChanged(Runnable r) {
        this.changed = r;
    }

    protected void fireChanged() {
        if (changed != null) changed.run();
    }

    protected int c(int color) {
        return alpha >= 0.999 ? color : Theme.fade(color, alpha);
    }

    @Override
    protected final void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        dt = (now - lastFrame) / 1_000_000_000.0;
        lastFrame = now;

        hot.set(isHovered() ? 1 : 0);
        press.set(down ? 1 : 0);
        hot.update(dt);
        press.update(dt);

        double p = Anim.easeOut(appear);
        alpha = p;
        if (p <= 0.004) return;

        int dy = (int) Math.round((1 - p) * 7);
        float sc = (float) (0.965 + 0.035 * Anim.easeOutBack(appear));
        boolean moved = dy != 0 || Math.abs(sc - 1f) > 0.001f;
        if (moved) IconBackend.pushPopup(ctx, getX() + getWidth() / 2f, getY() + getHeight() / 2f, sc, dy);

        boolean pressed = Theme.pushPress(ctx, getX(), getY(), getWidth(), getHeight(), press.get());
        draw(ctx, mouseX, mouseY);
        if (pressed) Theme.popPress(ctx);

        if (moved) IconBackend.popMatrix(ctx);
    }

    protected abstract void draw(DrawContext ctx, int mouseX, int mouseY);

    @Override
    protected void cClick(double mouseX, double mouseY) {
        down = true;
        onPress(mouseX, mouseY);
    }

    @Override
    protected void cRelease(double mouseX, double mouseY) {
        down = false;
    }

    protected void onPress(double mouseX, double mouseY) {
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }

    @Override
    public void playDownSound(net.minecraft.client.sound.SoundManager sm) {
        pl.teamping.Sounds.playClick();
    }
}
