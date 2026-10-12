package pl.teamping;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import pl.teamping.ui.Flags;
import pl.teamping.ui.Icons;
import pl.teamping.ui.Theme;

import java.util.List;

public class HudRenderer {

    private static final double MARKER_BASE = 1.3;

    private static final int HEAD = 16;

    private static final int CLUSTER_PX = 12;

    private static final int MAX_DRAWN = 40;

    private static final Matrix4f CAP_PROJ = new Matrix4f();
    private static final Matrix4f CAP_VIEW = new Matrix4f();
    private static double capX, capY, capZ;
    private static volatile boolean capValid = false;

    public static void captureView(WorldFrame ctx) {
        if (ctx.projectionMatrix() == null) return;
        CAP_PROJ.set(ctx.projectionMatrix());
        CAP_VIEW.set(RenderSystem.getModelViewMatrix());
        Vec3d c = ctx.cameraPos();
        capX = c.x; capY = c.y; capZ = c.z;
        capValid = true;
    }

    public static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (mc.options.hudHidden) return;

        List<Ping> pings = PingManager.snapshot();
        if (pings.isEmpty()) return;

        Config cfg = TeamPingMod.CONFIG;

        if (!capValid) return;

        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();

        String dim = mc.world.getRegistryKey().getValue().getPath();
        long now = System.currentTimeMillis();
        int margin = 20;

        int n = pings.size();
        int[] drawnX = new int[n];
        int[] drawnY = new int[n];
        int drawn = 0;

        for (Ping p : pings) {
            double[] pos = p.mappedTo(dim);
            if (pos == null) continue;
            boolean converted = p.isConvertedFor(dim);

            double rx = pos[0] - capX, ry = pos[1] - capY, rz = pos[2] - capZ;
            double dist = Math.sqrt(rx * rx + ry * ry + rz * rz);

            if (!Double.isFinite(dist)) continue;
            if (!cfg.renderDistanceUnlimited && dist > cfg.renderDistance) continue;

            Vector4f vv = new Vector4f((float) rx, (float) ry, (float) rz, 1f);
            CAP_VIEW.transform(vv);
            double px = vv.x, py = vv.y, pz = -vv.z;
            Vector4f cv = new Vector4f(vv.x, vv.y, vv.z, 1f);
            CAP_PROJ.transform(cv);

            float age = (now - p.createdAt) / 1000f;
            float lifeSec = p.lifetimeMs() / 1000f;
            float fade = Math.max(0f, Math.min(1f, (lifeSec - age) / 2f));
            int alpha = (int) (255 * fade);
            if (alpha < 10) continue;

            double pop = Math.min(1.0, (now - p.createdAt) / 250.0);
            double scale = cfg.scale * MARKER_BASE * (0.86 + 0.14 * ease(pop));

            boolean front = cv.w > 0.05f;
            double sx = 0, sy2 = 0;
            if (front) {
                double iw = 1.0 / cv.w;
                sx = (cv.x * iw * 0.5 + 0.5) * w;
                sy2 = (0.5 - cv.y * iw * 0.5) * h;
            }

            boolean clamped = false;
            if (!front || sx < margin || sx > w - margin || sy2 < margin || sy2 > h - margin) {
                if (!cfg.edgeArrows) continue;
                double dx = front ? (sx - w / 2.0) : -px;
                double dy = front ? (sy2 - h / 2.0) : py;
                double len = Math.sqrt(dx * dx + dy * dy);
                if (len < 1e-4) { dx = 0; dy = -1; len = 1; }
                dx /= len; dy /= len;

                double halfW = w / 2.0 - margin;
                double halfH = h / 2.0 - margin;
                double tx = Math.abs(dx) < 1e-6 ? Double.MAX_VALUE : halfW / Math.abs(dx);
                double ty = Math.abs(dy) < 1e-6 ? Double.MAX_VALUE : halfH / Math.abs(dy);
                sx = w / 2.0 + dx * Math.min(tx, ty);
                sy2 = h / 2.0 + dy * Math.min(tx, ty);
                clamped = true;
            }

            int ix = (int) Math.round(sx);
            int iy = (int) Math.round(sy2);

            if (!p.own && !p.permanent) {
                if (drawn >= MAX_DRAWN) continue;
                boolean overlaps = false;
                for (int k = 0; k < drawn; k++) {
                    int ddx = drawnX[k] - ix, ddy = drawnY[k] - iy;
                    if (ddx * ddx + ddy * ddy < CLUSTER_PX * CLUSTER_PX) { overlaps = true; break; }
                }
                if (overlaps) continue;
            }
            if (drawn < n) { drawnX[drawn] = ix; drawnY[drawn] = iy; drawn++; }

            int white = (alpha << 24) | 0xFFFFFF;

            int head = Math.max(8, (int) Math.round(HEAD * scale));
            int top = iy - head / 2;

            boolean drewHead = false;
            if (cfg.showHead) {
                drewHead = drawHead(ctx, mc, p.sender, ix - head / 2, top, head, alpha);
            }
            if (!drewHead) {

                int fw = Math.max(8, (int) Math.round(14 * scale));
                int fh = Math.max(6, (int) Math.round(9 * scale));
                Flags.draw(ctx, ix, iy, fw, fh, flagFor(cfg, p.sender), alpha);
                top = iy - fh / 2;
                head = fh;
            }

            int textY = top + head + 3;

            int outlineA = Math.min(230, alpha);
            if (!clamped && cfg.showSender) {
                Theme.textCenteredOutlined(ctx, p.sender, ix, textY, white, outlineA);
                textY += 11;
            }
            if (cfg.showDistance) {

                String s = (int) dist + " kratek" + (converted ? " · portal" : "");
                int c = converted ? (alpha << 24) | 0xF0B429 : (alpha << 24) | 0xE6EBF2;
                Theme.textCenteredOutlined(ctx, s, ix, textY, c, outlineA);
            }
        }
    }

    private static boolean drawHead(DrawContext ctx, MinecraftClient mc, String nick, int x, int y, int size, int alpha) {
        if (nick == null) return false;
        Identifier skin = skinFor(mc, nick);
        if (skin == null) return false;

        int tint = (alpha << 24) | 0xFFFFFF;

        Icons.drawRegion(ctx, skin, x, y, size, size, 8f, 8f, 8, 8, 64, 64, tint);
        Icons.drawRegion(ctx, skin, x, y, size, size, 40f, 8f, 8, 8, 64, 64, tint);

        int oc = (Math.min(90, alpha) << 24);
        ctx.fill(x - 1, y - 1, x + size + 1, y, oc);
        ctx.fill(x - 1, y + size, x + size + 1, y + size + 1, oc);
        ctx.fill(x - 1, y, x, y + size, oc);
        ctx.fill(x + size, y, x + size + 1, y + size, oc);
        return true;
    }

    private static Identifier skinFor(MinecraftClient mc, String nick) {

        Identifier fallback = null;
        if (mc.getNetworkHandler() != null) {
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(nick);
            if (e != null) {
                try {
                    Identifier tex = pl.teamping.ui.Compat.headTexture(e);
                    if (tex != null) {

                        if (pl.teamping.ui.Compat.skinUrl(e) != null) return tex;
                        fallback = tex;
                    }
                } catch (Throwable ignored) {  }
            }
        }
        Identifier cached = SkinCache.get(nick);
        return cached != null ? cached : fallback;
    }

    private static double ease(double t) {
        return 1 - Math.pow(1 - Math.max(0, Math.min(1, t)), 3);
    }

    private static Flags.Flag flagFor(Config cfg, String sender) {
        if (sender != null && cfg.playerFlags != null) {
            String spec = cfg.playerFlags.get(sender.toLowerCase(java.util.Locale.ROOT));
            Flags.Flag f = Flags.parse(spec);
            if (f != null) return f;
        }
        return Flags.fallback();
    }
}
