package pl.teamping.ui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public abstract class CompatScreen extends Screen {

    protected CompatScreen(Text title) {
        super(title);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        return cMouseClicked(x, y, button);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        return cMouseReleased(x, y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return cMouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return cKeyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return cCharTyped(chr, modifiers);
    }

    protected boolean superMouseClicked(double x, double y, int button) {
        return super.mouseClicked(x, y, button);
    }

    protected boolean superMouseReleased(double x, double y, int button) {
        return super.mouseReleased(x, y, button);
    }

    protected boolean superMouseDragged(double x, double y, int button, double dx, double dy) {
        return super.mouseDragged(x, y, button, dx, dy);
    }

    protected boolean superKeyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    protected boolean superCharTyped(char chr, int modifiers) {
        return super.charTyped(chr, modifiers);
    }

    protected abstract boolean cMouseClicked(double x, double y, int button);
    protected abstract boolean cMouseReleased(double x, double y, int button);
    protected abstract boolean cMouseDragged(double x, double y, int button, double dx, double dy);
    protected abstract boolean cKeyPressed(int keyCode, int scanCode, int modifiers);
    protected abstract boolean cCharTyped(char chr, int modifiers);
}
