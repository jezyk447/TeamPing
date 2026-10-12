package pl.teamping;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;
import net.fabricmc.loader.api.FabricLoader;
import pl.teamping.ui.IconBackend;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public final class SkinCache {

    private SkinCache() {}

    private static final int MAX = 64;
    private static final int MAX_BYTES = 256 * 1024;
    private static final int TIMEOUT_MS = 5000;
    private static final String HOST = "textures.minecraft.net";

    public static final int GREY = 0x9AA0A6;

    private static final class Head {
        final String nick;
        volatile String url;
        volatile Identifier id;
        volatile int status;
        volatile int color = 0;
        volatile long lastUse;
        Head(String nick) { this.nick = nick; }
    }

    private static final Map<String, Head> heads = new ConcurrentHashMap<>();
    private static final LinkedBlockingQueue<Head> queue = new LinkedBlockingQueue<>();
    private static volatile boolean started = false;
    private static int nameSeq = 0;

    public static void put(String nick, String url) {
        if (nick == null || url == null) return;
        url = normalize(url);
        if (url == null) return;
        String key = nick.toLowerCase(Locale.ROOT);

        Head h = heads.computeIfAbsent(key, Head::new);
        synchronized (h) {

            if (url.equals(h.url) && (h.status == 1 || h.status == 2)) return;
            h.url = url;
            h.status = 1;
        }
        queue.offer(h);
        ensureWorker();
    }

    public static Identifier get(String nick) {
        if (nick == null) return null;
        Head h = heads.get(nick.toLowerCase(Locale.ROOT));
        if (h == null || h.status != 2) return null;
        h.lastUse = System.currentTimeMillis();
        return h.id;
    }

    public static int color(String nick) {
        if (nick == null) return defaultColor();
        Head h = heads.get(nick.toLowerCase(Locale.ROOT));
        return (h != null && h.color != 0) ? h.color : defaultColor();
    }

    private static volatile int defaultColor = 0;
    private static volatile boolean defaultColorTried = false;

    public static int defaultColor() {
        int c = defaultColor;
        if (c != 0) return c;
        if (defaultColorTried) return GREY;
        defaultColorTried = true;
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null || mc.getResourceManager() == null) return GREY;
            Identifier id = Identifier.of("minecraft", "textures/entity/player/wide/steve.png");
            var res = mc.getResourceManager().getResource(id);
            if (res.isEmpty()) return GREY;
            try (InputStream in = res.get().getInputStream()) {
                NativeImage img = NativeImage.read(in);
                int col = dominantColor(img);
                img.close();
                defaultColor = col;
                return col;
            }
        } catch (Throwable t) {
            return GREY;
        }
    }

    private static synchronized void ensureWorker() {
        if (started) return;
        started = true;
        Thread t = new Thread(SkinCache::loop, "TeamPing-Skins");
        t.setDaemon(true);
        t.start();
    }

    private static void loop() {
        while (true) {
            Head h;
            try { h = queue.take(); } catch (InterruptedException e) { return; }
            String url = h.url;
            try {
                byte[] png = load(url);
                NativeImage img = toModern(NativeImage.read(new ByteArrayInputStream(png)));

                h.color = dominantColor(img);
                MinecraftClient mc = MinecraftClient.getInstance();
                mc.execute(() -> register(h, img));
            } catch (Throwable t) {
                h.status = 3;
            }
        }
    }

    private static void register(Head h, NativeImage img) {
        try {
            Identifier old = h.id;
            Identifier id = IconBackend.uploadHead("head_" + h.nick + "_" + (nameSeq++), img);
            h.id = id;
            h.status = 2;
            h.lastUse = System.currentTimeMillis();
            if (old != null && !old.equals(id)) IconBackend.destroyTexture(old);
            evictIfNeeded();
        } catch (Throwable t) {
            h.status = 3;
            try { img.close(); } catch (Throwable ignored) {}
        }
    }

    private static void evictIfNeeded() {
        int ready = 0;
        for (Head h : heads.values()) if (h.status == 2) ready++;
        while (ready > MAX) {
            Head victim = null;
            for (Head h : heads.values()) {
                if (h.status != 2) continue;
                if (victim == null || h.lastUse < victim.lastUse) victim = h;
            }
            if (victim == null) break;
            if (victim.id != null) IconBackend.destroyTexture(victim.id);
            heads.remove(victim.nick);
            ready--;
        }
    }

    private static int dominantColor(NativeImage img) {
        try {
            int w = img.getWidth(), h = img.getHeight();
            int[] count = new int[16 * 16 * 16];
            double[] weight = new double[16 * 16 * 16];
            long[] sr = new long[16 * 16 * 16], sg = new long[16 * 16 * 16], sb = new long[16 * 16 * 16];

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = img.getColorArgb(x, y);
                    if (((argb >>> 24) & 0xFF) < 200) continue;
                    int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;

                    int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
                    if (max < 24) continue;
                    double sat = (max - min) / (double) max;
                    int bucket = ((r >> 4) << 8) | ((g >> 4) << 4) | (b >> 4);

                    count[bucket]++;

                    weight[bucket] += 0.15 + sat;
                    sr[bucket] += r; sg[bucket] += g; sb[bucket] += b;
                }
            }

            int best = -1;
            double bestW = 0;
            for (int i = 0; i < weight.length; i++) {
                if (weight[i] > bestW) { bestW = weight[i]; best = i; }
            }
            if (best < 0 || count[best] == 0) return GREY;

            int r = (int) (sr[best] / count[best]);
            int g = (int) (sg[best] / count[best]);
            int b = (int) (sb[best] / count[best]);
            return boost(r, g, b);
        } catch (Throwable t) {
            return GREY;
        }
    }

    private static int boost(int r, int g, int b) {
        float[] hsv = java.awt.Color.RGBtoHSB(r, g, b, null);
        float s = Math.min(1f, hsv[1] * 1.25f);
        float v = Math.max(0.55f, Math.min(1f, hsv[2] * 1.15f));
        return java.awt.Color.HSBtoRGB(hsv[0], s, v) & 0xFFFFFF;
    }

    public static void putFromGame(MinecraftClient mc, String nick) {
        if (nick == null || mc == null) return;
        Head h = heads.get(nick.toLowerCase(Locale.ROOT));
        if (h != null && h.color != 0) return;
        try {
            var nh = mc.getNetworkHandler();
            var entry = (nh != null) ? nh.getPlayerListEntry(nick) : null;
            if (entry == null) return;
            put(nick, pl.teamping.ui.Compat.skinUrl(entry));
        } catch (Throwable ignored) {

        }
    }

    private static byte[] load(String url) throws Exception {
        Path file = diskPath(url);
        try {
            if (file != null && Files.exists(file)) {
                byte[] b = Files.readAllBytes(file);
                if (b.length > 0) return b;
            }
        } catch (Throwable ignored) {  }

        byte[] png = download(url);
        if (file != null) {
            try {
                Files.createDirectories(file.getParent());
                Files.write(file, png);
            } catch (Throwable ignored) {  }
        }
        return png;
    }

    private static byte[] download(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(TIMEOUT_MS);
        c.setReadTimeout(TIMEOUT_MS);
        c.setInstanceFollowRedirects(false);
        c.setRequestProperty("User-Agent", "TeamPing");
        try (InputStream in = new BufferedInputStream(c.getInputStream())) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n, total = 0;
            while ((n = in.read(buf)) != -1) {
                total += n;
                if (total > MAX_BYTES) throw new java.io.IOException("skin za duzy");
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            c.disconnect();
        }
    }

    public static String normalize(String url) {
        if (url == null) return null;
        try {
            URL u = new URL(url.trim());
            String proto = u.getProtocol();
            if (!"https".equalsIgnoreCase(proto) && !"http".equalsIgnoreCase(proto)) return null;
            if (!HOST.equalsIgnoreCase(u.getHost()) || u.getPort() != -1) return null;
            String path = u.getPath();
            if (path == null || !path.startsWith("/texture/") || u.getQuery() != null) return null;
            return "https://" + HOST + path;
        } catch (Exception e) {
            return null;
        }
    }

    private static NativeImage toModern(NativeImage img) {
        if (img.getWidth() != 64 || img.getHeight() != 32) return img;
        NativeImage out = new NativeImage(64, 64, true);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 64; x++) out.setColorArgb(x, y, img.getColorArgb(x, y));
        }
        img.close();
        return out;
    }

    private static Path diskPath(String url) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] d = md.digest(url.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            return FabricLoader.getInstance().getConfigDir().resolve("teamping").resolve("skins").resolve(sb + ".png");
        } catch (Exception e) {
            return null;
        }
    }
}
