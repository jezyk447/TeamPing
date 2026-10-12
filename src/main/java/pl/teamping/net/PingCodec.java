package pl.teamping.net;

import pl.teamping.Ping;

public class PingCodec {

    public static final String PREFIX = "TP1,";

    public static String encode(Ping p) {
        String sender = clean(p.sender);
        String dim = clean(p.dim);

        return PREFIX
                + (p.permanent ? "2," : "0,")
                + String.format(java.util.Locale.ROOT, "%.1f", p.x) + ","
                + String.format(java.util.Locale.ROOT, "%.1f", p.y) + ","
                + String.format(java.util.Locale.ROOT, "%.1f", p.z) + ","
                + dim + ","
                + sender + ","
                + Math.abs(p.id % 100000000L);
    }

    public static Ping decodeFromText(String raw) {
        if (raw == null) return null;
        int i = raw.indexOf(PREFIX);
        if (i < 0) return null;

        int end = i;
        while (end < raw.length() && !Character.isWhitespace(raw.charAt(end))) end++;
        String payload = raw.substring(i, end);

        try {
            String[] a = payload.split(",");
            if (a.length < 8) return null;

            boolean permanent = "2".equals(a[1].trim());
            double x = Double.parseDouble(a[2].trim());
            double y = Double.parseDouble(a[3].trim());
            double z = Double.parseDouble(a[4].trim());

            if (!Ping.isSane(x, y, z)) return null;
            String dim = a[5].trim();
            String sender = a[6].trim();
            long id = Long.parseLong(a[7].trim());
            return new Ping(id, x, y, z, dim, sender, false, permanent);
        } catch (Exception e) {
            return null;
        }
    }

    private static String clean(String s) {
        if (s == null) return "?";
        String out = s.replaceAll("[^A-Za-z0-9_\\-]", "");
        return out.isEmpty() ? "?" : out;
    }
}
