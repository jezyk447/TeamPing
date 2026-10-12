package pl.teamping.ui;

import net.minecraft.client.gui.DrawContext;

public class TpChoice extends TpWidget {

    public static final int H = 20;

    private final String[] options;
    private int index;
    private Runnable onChange;
    private String label;

    private final Anim slide;

    public TpChoice(int x, int y, int w, String[] options, int index) {
        super(x, y, w, H, "choice");
        this.options = options;
        this.index = Math.max(0, Math.min(options.length - 1, index));
        this.slide = new Anim(this.index, Anim.MED);
    }

    public TpChoice onChange(Runnable r) { this.onChange = r; return this; }
    public TpChoice label(String s) { this.label = s; return this; }
    public int getIndex() { return index; }
    public String getLabel() { return label == null ? "" : label; }

    private int cellW() { return getWidth() / options.length; }

    @Override
    protected void draw(DrawContext ctx, int mouseX, int mouseY) {
        slide.set(index);
        slide.update(dt);

        int x = getX(), y = getY(), w = getWidth();
        if (label != null) Theme.text(ctx, label, x, y - 12, c(Theme.TEXT_DIM));

        Theme.roundRect(ctx, x, y, w, H, 6, c(Theme.track()));

        int cw = cellW();
        int sx = x + (int) Math.round(slide.get() * cw);
        Theme.accentSurface(ctx, sx + 1, y + 1, cw - 2, H - 2, 5, c(Theme.accent()));

        for (int i = 0; i < options.length; i++) {
            boolean on = i == index;
            boolean hov = isHovered() && mouseX >= x + i * cw && mouseX < x + (i + 1) * cw;
            Theme.textCentered(ctx, options[i], x + i * cw + cw / 2, Theme.midY(y, H),
                    c(on || hov ? Theme.TEXT : Theme.TEXT_DIM));
        }
    }

    @Override
    protected void onPress(double mouseX, double mouseY) {
        int i = (int) ((mouseX - getX()) / cellW());
        i = Math.max(0, Math.min(options.length - 1, i));
        if (i != index) {
            index = i;
            if (onChange != null) onChange.run();
            fireChanged();
        }
    }
}
