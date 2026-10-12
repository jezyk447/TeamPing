package pl.teamping.ui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.input.MouseInput;
import net.minecraft.text.Text;

public abstract class CompatWidget extends ClickableWidget {

    protected CompatWidget(int x, int y, int w, int h, Text label) {
        super(x, y, w, h, label);
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        cClick(click.x(), click.y());
    }

    @Override
    public void onRelease(Click click) {
        cRelease(click.x(), click.y());
    }

    @Override
    protected void onDrag(Click click, double dx, double dy) {
        cDrag(click.x(), click.y(), dx, dy);
    }

    protected void cClick(double mouseX, double mouseY) {}
    protected void cRelease(double mouseX, double mouseY) {}
    protected void cDrag(double mouseX, double mouseY, double dx, double dy) {}

    public boolean cForwardClick(double mouseX, double mouseY, int button) {
        return mouseClicked(new Click(mouseX, mouseY, new MouseInput(button, 0)), false);
    }

    public boolean cForwardDrag(double mouseX, double mouseY, int button, double dx, double dy) {
        return mouseDragged(new Click(mouseX, mouseY, new MouseInput(button, 0)), dx, dy);
    }

    public boolean cForwardRelease(double mouseX, double mouseY, int button) {
        return mouseReleased(new Click(mouseX, mouseY, new MouseInput(button, 0)));
    }
}
