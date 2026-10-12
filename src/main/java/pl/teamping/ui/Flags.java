package pl.teamping.ui;

import net.minecraft.client.gui.DrawContext;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class Flags {

    private Flags() {}

    public record Flag(boolean horizontal, int[] colors) {}

    private static final Map<String, String> BUILTIN = new LinkedHashMap<>();

    static {

        BUILTIN.put("ua", "h:0057B7,FFD700");
        BUILTIN.put("pl", "h:FFFFFF,DC143C");
        BUILTIN.put("de", "h:000000,DD0000,FFCE00");
        BUILTIN.put("nl", "h:AE1C28,FFFFFF,21468B");
        BUILTIN.put("ru", "h:FFFFFF,0039A6,D52B1E");
        BUILTIN.put("hu", "h:CE2939,FFFFFF,477050");
        BUILTIN.put("lt", "h:FDB913,006A44,C1272D");
        BUILTIN.put("lv", "h:9E3039,FFFFFF,9E3039");
        BUILTIN.put("ee", "h:0072CE,000000,FFFFFF");
        BUILTIN.put("at", "h:ED2939,FFFFFF,ED2939");
        BUILTIN.put("bg", "h:FFFFFF,00966E,D62612");

        BUILTIN.put("fr", "v:002395,FFFFFF,ED2939");
        BUILTIN.put("it", "v:009246,FFFFFF,CE2B37");
        BUILTIN.put("ie", "v:169B62,FFFFFF,FF883E");
        BUILTIN.put("be", "v:000000,FDDA24,EF3340");
        BUILTIN.put("ro", "v:002B7F,FCD116,CE1126");

        BUILTIN.put("none", "h:CFD3D8,9AA1AA");
    }

    public static String availableCodes() {
        return String.join(", ", BUILTIN.keySet());
    }

    public static Flag parse(String spec) {
        if (spec == null) return null;
        String s = spec.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return null;

        String resolved = BUILTIN.getOrDefault(s, s);

        boolean horizontal = true;
        if (resolved.startsWith("h:")) {
            resolved = resolved.substring(2);
        } else if (resolved.startsWith("v:")) {
            horizontal = false;
            resolved = resolved.substring(2);
        } else {
            return null;
        }

        String[] parts = resolved.split(",");
        if (parts.length == 0 || parts.length > 8) return null;

        int[] colors = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String hex = parts[i].trim();
            if (hex.length() != 6) return null;
            try {
                colors[i] = Integer.parseInt(hex, 16) & 0xFFFFFF;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return new Flag(horizontal, colors);
    }

    public static Flag fallback() {
        return parse("none");
    }

    public static void draw(DrawContext ctx, int cx, int cy, int w, int h, Flag flag, int alpha) {
        if (flag == null || w <= 0 || h <= 0) return;

        int x = cx - w / 2;
        int y = cy - h / 2;
        int a = (alpha & 0xFF) << 24;

        ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, a);

        int n = flag.colors().length;
        for (int i = 0; i < n; i++) {
            int color = a | flag.colors()[i];
            if (flag.horizontal()) {
                int y0 = y + h * i / n;
                int y1 = y + h * (i + 1) / n;
                ctx.fill(x, y0, x + w, y1, color);
            } else {
                int x0 = x + w * i / n;
                int x1 = x + w * (i + 1) / n;
                ctx.fill(x0, y, x1, y + h, color);
            }
        }
    }
}
