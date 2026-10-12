package pl.teamping;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;


public class PingManager {


    
    private static final int MAX_QUEUED = 256;
    
    private static final int MAX_PER_TICK = 8;
    
    private static final double BUCKET_CAPACITY = 5.0;
    
    private static final double BUCKET_REFILL_PER_SEC = 2.0;
    
    private static final int MAX_TRACKED_SENDERS = 64;
    
    private static final int MAX_ACTIVE = 64;

    private static final long SOUND_MIN_GAP_MS = 150;
    private static final long FLOOD_NOTICE_GAP_MS = 10_000;

    private static final List<Ping> active = new ArrayList<>();
    private static final ConcurrentLinkedQueue<Ping> incoming = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger queued = new AtomicInteger();

    private static final Map<String, Bucket> buckets = new HashMap<>();

    private static long lastSoundAt = 0;
    private static long lastFloodNoticeAt = 0;
    
    private static final AtomicInteger dropped = new AtomicInteger();

    private static class Bucket {
        double tokens = BUCKET_CAPACITY;
        long lastRefill = System.currentTimeMillis();
        long lastUsed = System.currentTimeMillis();
    }

    
    public static void enqueue(Ping p) {
        if (p == null) return;
        if (queued.get() >= MAX_QUEUED) {
            dropped.incrementAndGet();
            return;
        }
        incoming.add(p);
        queued.incrementAndGet();
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        int acceptedThisTick = 0;


        while (acceptedThisTick < MAX_PER_TICK) {
            Ping p = incoming.poll();
            if (p == null) break;
            queued.decrementAndGet();

            if (isDuplicate(p)) continue;


            if (!p.own && !takeToken(p.sender, now)) {
                dropped.incrementAndGet();
                continue;
            }

            acceptedThisTick++;

            removeBySender(p.sender);
            active.add(p);
            if (active.size() > MAX_ACTIVE) active.remove(0);
            onAdded(p, now);
        }

        reportFlood(now);
        expire(now);
    }

    
    private static void removeBySender(String sender) {
        if (sender == null) return;
        String key = sender.toLowerCase(Locale.ROOT);
        active.removeIf(o -> o.sender != null && o.sender.toLowerCase(Locale.ROOT).equals(key));
    }

    private static boolean isDuplicate(Ping p) {
        for (Ping o : active) {
            if (o.id == p.id) return true;
        }
        return false;
    }

    
    private static boolean takeToken(String sender, long now) {
        String key = sender == null ? "?" : sender.toLowerCase(Locale.ROOT);

        if (buckets.size() > MAX_TRACKED_SENDERS) evictStaleBuckets(now);

        Bucket b = buckets.get(key);
        if (b == null) {
            if (buckets.size() >= MAX_TRACKED_SENDERS) return false;
            b = new Bucket();
            buckets.put(key, b);
        }

        double refill = (now - b.lastRefill) / 1000.0 * BUCKET_REFILL_PER_SEC;
        if (refill > 0) {
            b.tokens = Math.min(BUCKET_CAPACITY, b.tokens + refill);
            b.lastRefill = now;
        }
        b.lastUsed = now;

        if (b.tokens < 1.0) return false;
        b.tokens -= 1.0;
        return true;
    }

    private static void evictStaleBuckets(long now) {
        buckets.entrySet().removeIf(e -> now - e.getValue().lastUsed > 60_000);
        if (buckets.size() > MAX_TRACKED_SENDERS) buckets.clear();
    }

    
    private static void reportFlood(long now) {
        int n = dropped.get();
        if (n <= 0) return;
        if (now - lastFloodNoticeAt < FLOOD_NOTICE_GAP_MS) return;

        lastFloodNoticeAt = now;
        dropped.addAndGet(-n);
    }

    private static void expire(long now) {
        Iterator<Ping> it = active.iterator();
        while (it.hasNext()) {
            Ping p = it.next();
            if (now - p.createdAt > p.lifetimeMs()) it.remove();
        }
    }

    private static void onAdded(Ping p, long now) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        Config cfg = TeamPingMod.CONFIG;


        if (cfg.sound && now - lastSoundAt >= SOUND_MIN_GAP_MS) {
            lastSoundAt = now;

            Sounds.play(cfg.pingSound, cfg.soundVolume, p.own ? 1.12f : 1.0f);
        }
    }

    public static List<Ping> snapshot() {
        return active;
    }

    public static void clear() {
        active.clear();
        incoming.clear();
        queued.set(0);
        buckets.clear();
        dropped.set(0);
    }
}
