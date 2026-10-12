package pl.teamping;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;
import pl.teamping.net.PingCodec;
import pl.teamping.net.ChannelClient;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class TeamPingMod implements ClientModInitializer {

    public static final String MOD_ID = "teamping";
    public static Config CONFIG = new Config();

    public static final String VERSION = resolveVersion();

    private static String resolveVersion() {
        try (java.io.InputStream in = TeamPingMod.class.getResourceAsStream("/teamping.version")) {
            if (in != null) {
                String v = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
                if (!v.isEmpty() && !v.startsWith("${")) return v;
            }
        } catch (Throwable ignored) {
        }
        return net.fabricmc.loader.api.FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(m -> m.getMetadata().getVersion().getFriendlyString())
                .orElse("3.0.0");
    }

    private static ChannelClient client = null;
    private static final Random RNG = new Random();

    private boolean pingWasDown = false;
    private boolean menuWasDown = false;
    private long lastPing = 0;

    private static final long HOLD_PIN_MS = 1000;
    private long pingPressStart = 0;
    private boolean pingHoldFired = false;

    @Override
    public void onInitializeClient() {
        CONFIG = ConfigManager.load();
        Sounds.register();

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudRenderCallback.EVENT.register((ctx, tickDelta) -> HudRenderer.render(ctx));

        pl.teamping.ui.Compat.registerWorldRender(frame -> {
            HudRenderer.captureView(frame);
            WaypointBeam.render(frame);
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> restartTransport());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            stopTransport();
            PingManager.clear();
        });
    }

    private void tick(MinecraftClient mc) {
        PingManager.tick();

        if (mc.player == null) return;

        sendOwnSkinIfDue(mc);

        boolean free = mc.currentScreen == null;

        boolean menuDown = free && isKeyDown(mc, CONFIG.menuKey);
        if (menuDown && !menuWasDown) {
            mc.setScreen(new ConfigScreen(null));
        }
        menuWasDown = menuDown;

        boolean pingDown = free && isKeyDown(mc, CONFIG.pingKey);
        long tnow = System.currentTimeMillis();
        if (pingDown && !pingWasDown) {
            pingPressStart = tnow;
            pingHoldFired = false;
        }
        if (pingDown && !pingHoldFired && tnow - pingPressStart >= HOLD_PIN_MS) {
            placePing(mc, true);
            pingHoldFired = true;
        }
        if (!pingDown && pingWasDown && !pingHoldFired) {
            placePing(mc, false);
        }
        pingWasDown = pingDown;
    }

    public static boolean isKeyDown(MinecraftClient mc, int code) {
        if (code == GLFW.GLFW_KEY_UNKNOWN) return false;
        long handle = mc.getWindow().getHandle();
        if (pl.teamping.ui.TpKeyBind.isMouse(code)) {
            return GLFW.glfwGetMouseButton(handle, pl.teamping.ui.TpKeyBind.mouseButton(code))
                    == GLFW.GLFW_PRESS;
        }
        return pl.teamping.ui.Compat.isKeyPressed(mc, code);
    }

    private void placePing(MinecraftClient mc, boolean permanent) {
        if (mc.player == null || mc.world == null) return;

        long now = System.currentTimeMillis();

        if (!permanent && now - lastPing < CONFIG.cooldownMs) return;

        Vec3d start = pl.teamping.ui.Compat.cameraPos(mc.gameRenderer.getCamera());
        Vec3d dir = mc.player.getRotationVec(1.0f);

        double loaded = Math.max(1, mc.options.getClampedViewDistance() - 1) * 16.0;
        double max = Math.min(CONFIG.maxPingDistance, loaded);
        Vec3d end = start.add(dir.multiply(max));

        BlockHitResult block = mc.world.raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));

        boolean hitBlock = block != null && block.getType() != HitResult.Type.MISS;
        Vec3d target = hitBlock ? block.getPos() : end;
        double bestDist = target.squaredDistanceTo(start);
        boolean hitEntity = false;

        Box scan = new Box(start, target).expand(1.0);
        List<Entity> ents = mc.world.getOtherEntities(mc.player, scan);
        for (Entity e : ents) {
            Optional<Vec3d> hit = e.getBoundingBox().expand(0.25).raycast(start, target);
            if (hit.isPresent()) {
                double dd = hit.get().squaredDistanceTo(start);
                if (dd < bestDist) {
                    bestDist = dd;
                    target = pl.teamping.ui.Compat.entityPos(e).add(0, e.getHeight() / 2.0, 0);
                    hitEntity = true;
                }
            }
        }

        if (!hitBlock && !hitEntity) return;
        if (!Ping.isSane(target.x, target.y, target.z)) return;

        lastPing = now;
        String dim = mc.world.getRegistryKey().getValue().getPath();
        String nick = mc.getSession().getUsername();
        long id = Math.abs(RNG.nextLong() % 100000000L);

        Ping p = new Ping(id, target.x, target.y, target.z, dim, nick, true, permanent);
        PingManager.enqueue(p);

        if (client != null) {

            client.sendSkin(ownSkinUrl(mc, nick));
            client.send(PingCodec.encode(p));
        }
    }

    private long lastSkinCheck = 0;

    private void sendOwnSkinIfDue(MinecraftClient mc) {
        ChannelClient c = client;
        if (c == null) return;
        long now = System.currentTimeMillis();
        if (now - lastSkinCheck < 5000) return;
        lastSkinCheck = now;
        c.sendSkin(ownSkinUrl(mc, mc.getSession().getUsername()));
    }

    private static String ownSkinUrl(MinecraftClient mc, String nick) {
        try {
            var nh = mc.getNetworkHandler();
            var entry = (nh != null) ? nh.getPlayerListEntry(nick) : null;

            return entry != null ? SkinCache.normalize(pl.teamping.ui.Compat.skinUrl(entry)) : null;
        } catch (Throwable t) {
            return null;
        }
    }

    public static void restartTransport() {
        stopTransport();
        String ch = CONFIG.channel == null ? "" : CONFIG.channel.trim();
        if (ch.isEmpty()) return;
        client = new ChannelClient(ch.replace(' ', '_'));
        client.start();
    }

    public static void stopTransport() {
        if (client != null) {
            try { client.stop(); } catch (Exception ignored) {}
            client = null;
        }
    }

    public static boolean isConnected() {
        return client != null;
    }

    public static String transportStatus() {
        return client == null ? "rozłączony" : client.status();
    }

    public static java.util.List<String> roster() {
        return client == null ? java.util.Collections.emptyList() : client.roster();
    }

    public static void applyAndRestart(Config cfg) {
        CONFIG = cfg;
        ConfigManager.save(cfg);
        restartTransport();
    }
}
