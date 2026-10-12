package pl.teamping;

public class Ping {

    private static final double MAX_Y = 8192.0;

    public static boolean isSane(double x, double y, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return false;
        if (Math.abs(y) > MAX_Y) return false;

        double border = TeamPingMod.CONFIG.worldBorder;
        if (border <= 0) return true;
        return Math.abs(x) <= border && Math.abs(z) <= border;
    }

    public static final String OVERWORLD = "overworld";
    public static final String NETHER = "the_nether";

    private static final double NETHER_RATIO = 8.0;

    public static final long PERMANENT_MS = 60_000L;

    public final long id;
    public final double x, y, z;
    public final String dim;
    public final String sender;
    public final boolean own;

    public final boolean permanent;
    public final long createdAt;

    public Ping(long id, double x, double y, double z, String dim, String sender, boolean own) {
        this(id, x, y, z, dim, sender, own, false);
    }

    public Ping(long id, double x, double y, double z, String dim, String sender, boolean own, boolean permanent) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dim = dim;
        this.sender = sender;
        this.own = own;
        this.permanent = permanent;
        this.createdAt = System.currentTimeMillis();
    }

    public long lifetimeMs() {
        return permanent ? PERMANENT_MS : TeamPingMod.CONFIG.lifetimeSeconds * 1000L;
    }

    public double[] mappedTo(String viewerDim) {
        if (dim == null || viewerDim == null) return null;
        if (dim.equals(viewerDim)) return new double[] {x, y, z};
        if (!TeamPingMod.CONFIG.crossDimension) return null;

        if (OVERWORLD.equals(dim) && NETHER.equals(viewerDim)) {
            return new double[] {x / NETHER_RATIO, y, z / NETHER_RATIO};
        }
        if (NETHER.equals(dim) && OVERWORLD.equals(viewerDim)) {
            return new double[] {x * NETHER_RATIO, y, z * NETHER_RATIO};
        }
        return null;
    }

    public boolean isConvertedFor(String viewerDim) {
        return dim != null && !dim.equals(viewerDim);
    }
}
