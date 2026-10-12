package pl.teamping.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class TpKeyBind extends TpWidget {

    public static final int H = 20;
    private static final int BOX_W = 100;

    public static final int UNBOUND = GLFW.GLFW_KEY_UNKNOWN;

    private int key;
    private boolean listening = false;

    public TpKeyBind(int x, int y, int w, String label, int key) {
        super(x, y, w, H, label);
        this.key = key;
    }

    public int getKey() {
        return key;
    }

    public boolean isListening() {
        return listening;
    }

    public void stopListening() {
        listening = false;
    }

    public boolean captureKey(int keyCode) {
        if (!listening) return false;
        listening = false;

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) return true;
        if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            key = UNBOUND;
            fireChanged();
            return true;
        }
        key = keyCode;
        fireChanged();
        return true;
    }

    public boolean captureMouse(int button) {
        if (!listening) return false;
        listening = false;
        key = encodeMouse(button);
        fireChanged();
        return true;
    }

    public static int encodeMouse(int button) {
        return -100 - button;
    }

    public static boolean isMouse(int code) {
        return code <= -100;
    }

    public static int mouseButton(int code) {
        return -100 - code;
    }

    public static String label(int code) {
        if (code == UNBOUND) return "brak";
        if (isMouse(code)) {
            int b = mouseButton(code);
            return switch (b) {
                case 0 -> "LPM";
                case 1 -> "PPM";
                case 2 -> "SPM";
                default -> "Mysz " + (b + 1);
            };
        }
        try {

            return InputUtil.Type.KEYSYM.createFromCode(code).getLocalizedText().getString();
        } catch (Exception e) {
            return "klawisz " + code;
        }
    }

    @Override
    protected void draw(DrawContext ctx, int mouseX, int mouseY) {
        int x = getX(), y = getY(), w = getWidth();
        double t = hot.get();

        Theme.text(ctx, getMessage().getString(), x, Theme.midY(y, H),
                c(Theme.lerpColor(Theme.TEXT_DIM, Theme.TEXT, t)));

        int bx = x + w - BOX_W;
        Theme.roundRectBordered(ctx, bx, y, BOX_W, H, 6,
                c(listening ? Theme.accentSoft() : Theme.track()),
                c(listening ? Theme.accent() : Theme.HAIRLINE_STRONG));

        String s = listening ? "naciśnij klawisz" : label(key);
        int color = listening ? Theme.accentText() : (key == UNBOUND ? Theme.TEXT_FAINT : Theme.TEXT);
        Theme.textCentered(ctx, s, bx + BOX_W / 2, Theme.midY(y, H),
                c(t > 0.01 || listening ? color : Theme.lerpColor(Theme.TEXT_DIM, color, t)));
    }

    @Override
    protected void onPress(double mouseX, double mouseY) {
        listening = true;
    }
}
