package pl.teamping.ui;

import net.minecraft.util.Identifier;

public final class Gradients {

    private Gradients() {}

    private static final int N = 256;

    private static Identifier hue, white, black;

    public static Identifier hueStrip() {
        if (hue == null) {
            int[] px = new int[N];
            for (int i = 0; i < N; i++) {
                px[i] = 0xFF000000 | TpColorPicker.hsvToRgb(i / (float) N, 1f, 1f);
            }
            hue = IconBackend.uploadGlyph("gradient/hue", px, 1, N);
        }
        return hue;
    }

    public static Identifier whiteFade() {
        if (white == null) {
            int[] px = new int[N];
            for (int i = 0; i < N; i++) {
                int a = 255 - Math.round(255f * i / (N - 1));
                px[i] = (a << 24) | 0xFFFFFF;
            }
            white = IconBackend.uploadGlyph("gradient/white", px, N, 1);
        }
        return white;
    }

    public static Identifier blackFade() {
        if (black == null) {
            int[] px = new int[N];
            for (int i = 0; i < N; i++) {
                int a = Math.round(255f * i / (N - 1));
                px[i] = (a << 24);
            }
            black = IconBackend.uploadGlyph("gradient/black", px, 1, N);
        }
        return black;
    }
}
