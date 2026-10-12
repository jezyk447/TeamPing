package pl.teamping;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.List;


public final class WaypointBeam {

    private WaypointBeam() {}

    private static final float RADIUS = 0.26f;
    private static final int SIDES = 4;
    private static final int FULL_LIGHT = 0x00F000F0;
    
    private static final long SPIN_MS = 16000L;
    
    private static final long SCROLL_MS = 2600L;
    
    private static final float TEX_BLOCKS = 2.0f;
    
    private static final float BELOW = 5.0f;

    
    private static final net.minecraft.util.Identifier BEAM_TEX =
            net.minecraft.util.Identifier.of("minecraft", "textures/entity/beacon_beam.png");

    
    private static final double LIGHT_DIR = -Math.PI / 4;

    public static void render(WorldFrame ctx) {
        Config cfg = TeamPingMod.CONFIG;
        if (!cfg.beam) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        List<Ping> pings = PingManager.snapshot();
        if (pings.isEmpty()) return;

        MatrixStack matrices = ctx.matrixStack();
        VertexConsumerProvider consumers = ctx.consumers();
        if (matrices == null || consumers == null) return;

        Vec3d cam = ctx.cameraPos();
        String dim = mc.world.getRegistryKey().getValue().getPath();
        long now = System.currentTimeMillis();
        int worldTop = mc.world.getBottomY() + mc.world.getHeight();
        int worldBottom = mc.world.getBottomY();

        VertexConsumer buf = consumers.getBuffer(pl.teamping.ui.Compat.entityTranslucent(BEAM_TEX));


        double spin = (now % SPIN_MS) / (double) SPIN_MS * Math.PI * 2;
        float scroll = (now % SCROLL_MS) / (float) SCROLL_MS;
        float[] cx = new float[SIDES + 1], cz = new float[SIDES + 1];
        for (int i = 0; i <= SIDES; i++) {
            double a = spin + i * (Math.PI * 2 / SIDES);
            cx[i] = (float) (Math.cos(a) * RADIUS);
            cz[i] = (float) (Math.sin(a) * RADIUS);
        }

        for (Ping p : pings) {
            double[] pos = p.mappedTo(dim);
            if (pos == null) continue;

            float age = (now - p.createdAt) / 1000f;
            float lifeSec = p.lifetimeMs() / 1000f;
            float fade = Math.max(0f, Math.min(1f, (lifeSec - age) / 2f));
            if (fade < 0.04f) continue;

            float top = (float) (worldTop - pos[1]);
            if (top < 1f) continue;

            float bot = (float) Math.max(-BELOW, worldBottom - pos[1]);


            float vBot = scroll - bot / TEX_BLOCKS;
            float vTop = scroll - top / TEX_BLOCKS;


            SkinCache.putFromGame(mc, p.sender);
            int rgb = SkinCache.color(p.sender);
            int r0 = (rgb >> 16) & 0xFF, g0 = (rgb >> 8) & 0xFF, b0 = rgb & 0xFF;
            int a = (int) (235 * fade);

            matrices.push();
            matrices.translate(pos[0] - cam.x, pos[1] - cam.y, pos[2] - cam.z);
            MatrixStack.Entry e = matrices.peek();
            Matrix4f m = e.getPositionMatrix();

            for (int i = 0; i < SIDES; i++) {

                double face = spin + (i + 0.5) * (Math.PI * 2 / SIDES);
                float sh = (float) (0.58 + 0.42 * (0.5 + 0.5 * Math.cos(face - LIGHT_DIR)));
                int r = Math.min(255, Math.round(r0 * sh));
                int g = Math.min(255, Math.round(g0 * sh));
                int b = Math.min(255, Math.round(b0 * sh));


                quad(buf, e, m, cx[i], cz[i], cx[i + 1], cz[i + 1], bot, top, r, g, b, a, false, vBot, vTop);
                quad(buf, e, m, cx[i], cz[i], cx[i + 1], cz[i + 1], bot, top, r, g, b, a, true, vBot, vTop);
            }

            matrices.pop();
        }
    }

    private static void quad(VertexConsumer buf, MatrixStack.Entry e, Matrix4f m,
                             float x0, float z0, float x1, float z1, float bot, float top,
                             int r, int g, int b, int a, boolean back, float vBot, float vTop) {
        if (!back) {
            vertex(buf, e, m, x0, bot, z0, r, g, b, a, 0f, vBot);
            vertex(buf, e, m, x1, bot, z1, r, g, b, a, 1f, vBot);
            vertex(buf, e, m, x1, top, z1, r, g, b, a, 1f, vTop);
            vertex(buf, e, m, x0, top, z0, r, g, b, a, 0f, vTop);
        } else {
            vertex(buf, e, m, x0, bot, z0, r, g, b, a, 0f, vBot);
            vertex(buf, e, m, x0, top, z0, r, g, b, a, 0f, vTop);
            vertex(buf, e, m, x1, top, z1, r, g, b, a, 1f, vTop);
            vertex(buf, e, m, x1, bot, z1, r, g, b, a, 1f, vBot);
        }
    }

    private static void vertex(VertexConsumer buf, MatrixStack.Entry e, Matrix4f m,
                               float x, float y, float z, int r, int g, int b, int a, float u, float v) {
        buf.vertex(m, x, y, z)
                .color(r, g, b, a)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(FULL_LIGHT)
                .normal(e, 0f, 1f, 0f);
    }
}
