package pl.teamping.ui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;
import net.minecraft.text.Text;

public abstract class CompatScreen extends Screen {

    protected CompatScreen(Text title) {
        super(title);
    }

    private MouseInput lastButton = new MouseInput(0, 0);

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        lastButton = click.buttonInfo();
        return cMouseClicked(click.x(), click.y(), click.button());
    }

    @Override
    public boolean mouseReleased(Click click) {
        lastButton = click.buttonInfo();
        return cMouseReleased(click.x(), click.y(), click.button());
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        lastButton = click.buttonInfo();
        return cMouseDragged(click.x(), click.y(), click.button(), dx, dy);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        return cKeyPressed(input.key(), input.scancode(), input.modifiers());
    }

    @Override
    public boolean charTyped(CharInput input) {
        return cCharTyped((char) input.codepoint(), input.modifiers());
    }

    protected boolean superMouseClicked(double x, double y, int button) {
        return super.mouseClicked(new Click(x, y, buttonInfo(button)), false);
    }

    protected boolean superMouseReleased(double x, double y, int button) {
        return super.mouseReleased(new Click(x, y, buttonInfo(button)));
    }

    protected boolean superMouseDragged(double x, double y, int button, double dx, double dy) {
        return super.mouseDragged(new Click(x, y, buttonInfo(button)), dx, dy);
    }

    protected boolean superKeyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(new KeyInput(keyCode, scanCode, modifiers));
    }

    protected boolean superCharTyped(char chr, int modifiers) {
        return super.charTyped(new CharInput(chr, modifiers));
    }

    private MouseInput buttonInfo(int button) {
        return (lastButton != null && lastButton.button() == button) ? lastButton : new MouseInput(button, 0);
    }

    protected abstract boolean cMouseClicked(double x, double y, int button);
    protected abstract boolean cMouseReleased(double x, double y, int button);
    protected abstract boolean cMouseDragged(double x, double y, int button, double dx, double dy);
    protected abstract boolean cKeyPressed(int keyCode, int scanCode, int modifiers);
    protected abstract boolean cCharTyped(char chr, int modifiers);
}
