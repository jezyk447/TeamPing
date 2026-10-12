package pl.teamping.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;
import pl.teamping.TeamPingMod;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class Icons {

    private Icons() {}

    private static Identifier of(String name) {
        return Identifier.of(TeamPingMod.MOD_ID, "textures/gui/" + name + ".png");
    }

    public static final Identifier CHANNELS = of("channels");
    public static final Identifier VISUALS = of("visuals");
    public static final Identifier PING = of("ping");
    public static final Identifier KEYBOARD = of("keyboard");
    public static final Identifier USER = of("user");
    public static final Identifier SHIELD = of("shield");
    public static final Identifier GEAR = of("gear");
    public static final Identifier EYE = of("eye");
    public static final Identifier FILTER = of("filter");
    public static final Identifier SEARCH = of("search");
    public static final Identifier CHECK = of("check");
    public static final Identifier CHECK_CIRCLE = of("check_circle");
    public static final Identifier XMARK = of("xmark");
    public static final Identifier HOME = of("home");

    public static final Identifier LOGO = of("logo");

    private static final int TEX = 64;

    private static final Map<Identifier, Identifier> RUNTIME = new HashMap<>();

    private static final Map<Identifier, Boolean> FAILED = new HashMap<>();

    private static Identifier runtimeOf(Identifier key) {
        Identifier cached = RUNTIME.get(key);
        if (cached != null) return cached;
        if (FAILED.containsKey(key)) return null;
        try {
            String res = "assets/" + key.getNamespace() + "/" + key.getPath();
            try (InputStream in = Icons.class.getClassLoader().getResourceAsStream(res)) {
                if (in == null) throw new IllegalStateException("brak " + res);
                NativeImage img = NativeImage.read(in);
                String name = "icon/" + key.getPath().replace('/', '_');
                Identifier id = IconBackend.uploadIcon(name, img);
                RUNTIME.put(key, id);
                return id;
            }
        } catch (Throwable t) {
            FAILED.put(key, Boolean.TRUE);
            return null;
        }
    }

    public static void draw(DrawContext ctx, Identifier icon, int x, int y, int size, int color) {
        color = Theme.alpha(color);
        if ((color >>> 24) == 0) return;
        Identifier tex = runtimeOf(icon);
        if (tex == null) return;
        IconBackend.draw(ctx, tex, x, y, size, size, TEX, TEX, color);
    }

    public static void drawRegion(DrawContext ctx, Identifier tex, int x, int y, int w, int h,
                                  float u, float v, int regionW, int regionH,
                                  int texW, int texH, int color) {
        color = Theme.alpha(color);
        if ((color >>> 24) == 0) return;
        IconBackend.drawRegion(ctx, tex, x, y, w, h, u, v, regionW, regionH, texW, texH, color);
    }
}
