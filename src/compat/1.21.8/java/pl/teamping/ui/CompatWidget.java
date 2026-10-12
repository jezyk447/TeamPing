package pl.teamping.ui;

import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public abstract class CompatWidget extends ClickableWidget {

    protected CompatWidget(int x, int y, int w, int h, Text label) {
        super(x, y, w, h, label);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        cClick(mouseX, mouseY);
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        cRelease(mouseX, mouseY);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dx, double dy) {
        cDrag(mouseX, mouseY, dx, dy);
    }

    protected void cClick(double mouseX, double mouseY) {}
    protected void cRelease(double mouseX, double mouseY) {}
    protected void cDrag(double mouseX, double mouseY, double dx, double dy) {}

    public boolean cForwardClick(double mouseX, double mouseY, int button) {
        return mouseClicked(mouseX, mouseY, button);
    }

    public boolean cForwardDrag(double mouseX, double mouseY, int button, double dx, double dy) {
        return mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    public boolean cForwardRelease(double mouseX, double mouseY, int button) {
        return mouseReleased(mouseX, mouseY, button);
    }
}
