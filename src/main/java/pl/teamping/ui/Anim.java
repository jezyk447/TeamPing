package pl.teamping.ui;


public class Anim {

    
    public static final double SNAP = 30;
    
    public static final double FAST = 22;
    
    public static final double MED = 17;

    private double value;
    private double target;
    private final double speed;

    public Anim(double initial, double speed) {
        this.value = initial;
        this.target = initial;
        this.speed = speed;
    }

    public void set(double t) {
        this.target = t;
    }

    
    public void snap(double t) {
        this.target = t;
        this.value = t;
    }

    public double get() {
        return value;
    }

    
    public void update(double dt) {
        if (dt <= 0) return;

        double k = 1.0 - Math.exp(-speed * Math.min(dt, 0.1));
        value += (target - value) * k;
        if (Math.abs(target - value) < 0.0005) value = target;
    }

    
    public static double easeOut(double t) {
        t = Math.max(0, Math.min(1, t));
        return 1 - Math.pow(1 - t, 3);
    }

    
    public static double easeOutBack(double t) {
        t = Math.max(0, Math.min(1, t));
        double c1 = 1.70158, c3 = c1 + 1.0, u = t - 1.0;
        return 1.0 + c3 * u * u * u + c1 * u * u;
    }

    public static double clamp01(double t) {
        return Math.max(0, Math.min(1, t));
    }

    
    public static double easeInOut(double t) {
        t = clamp01(t);
        return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }

    
    public static double easeIn(double t) {
        t = clamp01(t);
        return t * t * t;
    }



    
    public static final double POP_SECONDS = 0.38;

    
    public static double popIn(double p) {
        p = clamp01(p);
        double split = 0.55;
        if (p < split) return lerp(0.85, 1.06, easeOut(p / split));
        return lerp(1.06, 1.00, easeInOut((p - split) / (1 - split)));
    }

    
    public static double popOut(double p) {
        p = clamp01(p);
        double split = 0.35;
        if (p < split) return lerp(1.00, 1.06, easeOut(p / split));
        return lerp(1.06, 0.80, easeIn((p - split) / (1 - split)));
    }

    
    public static double popInAlpha(double p) {
        return clamp01(easeOut(clamp01(p) / 0.45));
    }

    public static double popOutAlpha(double p) {
        return 1.0 - clamp01((clamp01(p) - 0.35) / 0.65);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
