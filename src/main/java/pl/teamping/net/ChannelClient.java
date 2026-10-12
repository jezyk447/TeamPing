package pl.teamping.net;

import pl.teamping.Ping;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class ChannelClient {

    private static final int MAX_ATTEMPTS = 12;

    private static final int MAX_LINE = 8192;

    private static final int MAX_ENC_LINE = 32768;

    private volatile boolean running = false;
    private volatile Socket socket;
    private volatile BufferedWriter out;
    private volatile String state = "rozlaczony";

    private volatile String lastError = null;
    private volatile List<String> roster = Collections.emptyList();

    private volatile byte[] enc;

    private volatile boolean ready = false;
    private Thread thread;

    private final String room;

    public ChannelClient(String room) {
        this.room = room;
    }

    public void start() {
        running = true;
        lastError = null;
        thread = new Thread(this::loop, "TeamPing-Channel");
        thread.setDaemon(true);
        thread.start();
    }

    private void loop() {
        int backoff = 2000;
        int attempts = 0;

        while (running) {
            Socket s = null;
            try {
                state = "laczenie...";
                enc = null;
                ready = false;
                skinSent = "";
                s = new Socket();
                s.connect(new InetSocketAddress(RelayAddress.HOST, RelayAddress.PORT), 5000);
                s.setKeepAlive(true);

                s.setSoTimeout(120_000);
                socket = s;

                BufferedWriter w = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));
                BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                out = w;

                String nick = net.minecraft.client.MinecraftClient.getInstance().getSession().getUsername();
                String room = this.room;

                upgrade(r);

                writeLine("H " + room + " " + nick);
                ready = true;

                state = "polaczony";
                lastError = null;
                backoff = 2000;
                attempts = 0;

                startKeepAlive(s);

                String line;
                while (running && (line = readLine(r)) != null) {
                    handleLine(line);
                }
            } catch (Exception e) {
                if (lastError == null) state = "blad: " + e.getMessage();
            } finally {
                ready = false;
                roster = Collections.emptyList();
                closeQuietly();
            }

            if (!running) break;

            if (lastError != null) {
                state = lastError;
                break;
            }

            if (++attempts >= MAX_ATTEMPTS) {
                state = "brak polaczenia - sprawdz adres i port";
                break;
            }

            try { Thread.sleep(backoff); } catch (InterruptedException e) { break; }
            backoff = Math.min(backoff * 2, 30000);
        }

        if (lastError == null && running) state = "rozlaczony";
        running = false;
    }

    private void upgrade(BufferedReader r) throws java.io.IOException {
        String c = readLine(r);
        if (c == null) throw new java.io.IOException("serwer zerwal przed handshake");
        if (!c.startsWith("C ")) { handleLine(c); throw new java.io.IOException("brak wyzwania C"); }
        String[] cp = c.substring(2).trim().split("\\s+");
        String srvEphHex = cp.length > 0 ? cp[0].toLowerCase() : "";
        String srvStaticHex = cp.length > 1 ? cp[1].toLowerCase() : "";
        if (!srvEphHex.matches("[0-9a-f]{64}") || !srvStaticHex.matches("[0-9a-f]{64}")) {
            throw new java.io.IOException("zly handshake serwera");
        }
        if (!constEq(srvStaticHex, hex(RelayAddress.SERVER_STATIC_PUB))) {

            lastError = "niezgodny klucz serwera";
            throw new java.io.IOException("niezgodny klucz serwera (pin)");
        }

        java.security.KeyPair eph = Transport.x25519();
        writeLine("X " + hex(Transport.rawPublic(eph)));
        enc = Transport.sessionKey(eph, unhex(srvEphHex), unhex(srvStaticHex));

        String y = readLine(r);
        if (y == null || !y.startsWith("Y")) {
            throw new java.io.IOException("brak potwierdzenia szyfrowania");
        }
    }

    private String readLine(BufferedReader r) throws java.io.IOException {
        byte[] k = enc;
        String raw = readLimited(r, k != null);
        if (raw == null) return null;
        if (k == null) return raw;
        try { return Transport.open(k, raw); }
        catch (Exception e) { throw new java.io.IOException("zly szyfrogram z serwera"); }
    }

    private void writeLine(String line) throws java.io.IOException {
        byte[] k = enc;
        String wire = (k != null) ? Transport.seal(k, line) : line;
        synchronized (this) {
            if (out == null) throw new java.io.IOException("brak polaczenia");
            out.write(wire + "\n");
            out.flush();
        }
    }

    private static String readLimited(BufferedReader r, boolean encrypted) throws java.io.IOException {
        StringBuilder sb = new StringBuilder(128);
        int c;
        while ((c = r.read()) != -1) {
            if (c == 10) return sb.toString();
            if (c != 13) sb.append((char) c);
            if (sb.length() > (encrypted ? MAX_ENC_LINE : MAX_LINE)) {
                throw new java.io.IOException("serwer przyslal za dluga linie");
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) { sb.append(Character.forDigit((x >> 4) & 0xF, 16)); sb.append(Character.forDigit(x & 0xF, 16)); }
        return sb.toString();
    }

    private static byte[] unhex(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) b[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        return b;
    }

    private void handleLine(String line) {
        if (line.startsWith("P ")) {
            Ping p = PingCodec.decodeFromText(line.substring(2));
            if (p != null) pl.teamping.PingManager.enqueue(p);
            return;
        }
        if (line.startsWith("N ")) {
            String body = line.substring(2).trim();
            roster = body.isEmpty() ? Collections.emptyList() : List.of(body.split(","));
            return;
        }
        if (line.startsWith("S ")) {

            String[] parts = line.substring(2).trim().split(" ", 2);
            if (parts.length == 2) pl.teamping.SkinCache.put(parts[0], parts[1]);
            return;
        }
        if (line.startsWith("E ")) {
            String[] parts = line.substring(2).split(" ", 2);
            String code = parts[0];
            String msg = parts.length > 1 ? parts[1] : code;
            lastError = switch (code) {
                case "BAD_TOKEN" -> "odrzucono: zly pokoj lub token";
                case "ROOM_FULL" -> "odrzucono: pokoj pelny";
                case "LINE_TOO_LONG" -> "odrzucono: za dluga wiadomosc";
                default -> "odrzucono: " + msg;
            };
        }

    }

    private void startKeepAlive(Socket owner) {
        Thread ka = new Thread(() -> {
            try {
                while (running && socket == owner) {
                    Thread.sleep(20000);
                    if (socket == owner) writeLine("K");
                }
            } catch (Exception ignored) {

            }
        }, "TeamPing-Channel-KA");
        ka.setDaemon(true);
        ka.start();
    }

    private void closeQuietly() {
        synchronized (this) {
            try { if (out != null) out.close(); } catch (Exception ignored) {}
            try { if (socket != null) socket.close(); } catch (Exception ignored) {}
            out = null;
            socket = null;
        }
    }

    public void stop() {
        running = false;
        closeQuietly();
        if (thread != null) thread.interrupt();
    }

    private volatile String skinSent = "";

    public void sendSkin(String url) {
        if (!ready || url == null || url.isEmpty()) return;
        if (url.equals(skinSent)) return;
        try {
            writeLine("U " + url);
            skinSent = url;
        } catch (Exception ignored) {

        }
    }

    public void send(String payload) {
        if (!ready) return;
        try {
            writeLine("P " + payload);
        } catch (Exception e) {
            closeQuietly();
        }
    }

    private static boolean constEq(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }

    public List<String> roster() {
        return roster;
    }

    public String status() {
        int n = roster.size();
        return "(" + state + (n > 0 ? ", " + n + " w pokoju" : "") + ")";
    }
}
