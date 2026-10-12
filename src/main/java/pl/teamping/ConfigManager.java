package pl.teamping;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;


public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "config.json";

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("teamping").resolve(FILE);
    }

    
    public static Config load() {
        return readFrom(file());
    }

    
    static Config readFrom(Path f) {
        try {
            if (Files.exists(f)) {
                Config c = GSON.fromJson(Files.readString(f), Config.class);
                if (c != null) {
                    migrate(c);
                    return c;
                }
            }
        } catch (Exception ignored) {

        }
        return new Config();
    }

    
    static void writeTo(Config c, Path f) {
        try {
            Files.createDirectories(f.getParent());
            Path tmp = f.resolveSibling(FILE + ".tmp");
            Files.writeString(tmp, GSON.toJson(c));
            try {
                Files.move(tmp, f, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmp, f, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ignored) {
        }
    }

    
    private static void migrate(Config c) {
        if (c.channels == null) {
            c.channels = new Config().channels;
            return;
        }

        c.channels.removeIf(n -> n != null && n.matches("kanal-([5-9]|\\d{2,})"));
        for (int n = 1; c.channels.size() < 4; n++) {
            String name = "kanal-" + n;
            if (!c.channels.contains(name)) c.channels.add(name);
            if (n > 20) break;
        }
    }

    
    public static Config copyOf(Config c) {
        Config out = GSON.fromJson(GSON.toJson(c), Config.class);
        return out == null ? new Config() : out;
    }

    
    public static Config parse(String json) {
        try {
            Config c = GSON.fromJson(json, Config.class);
            if (c == null) return null;
            migrate(c);
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    
    public static String json(Config c) {
        try {
            return GSON.toJson(c);
        } catch (Exception e) {
            return String.valueOf(System.nanoTime());
        }
    }

    
    public static void save(Config c) {
        c.savedAt = System.currentTimeMillis();
        writeTo(c, file());
    }

}
