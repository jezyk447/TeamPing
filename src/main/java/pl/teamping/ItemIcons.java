package pl.teamping;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ItemIcons {

    private ItemIcons() {}

    public record Icon(String hash, String b64) {}

    private static final int MAX_PNG = 24 * 1024;

    private static final long TTL_MS = 30_000;

    private record Entry(Icon icon, long at) {}
    private static final Map<String, Entry> cache = new HashMap<>();

    public static Icon iconFor(ItemStack stack, MinecraftClient mc) {
        if (stack == null || stack.isEmpty() || mc == null || mc.getResourceManager() == null) return null;
        try {
            Identifier itemId = Registries.ITEM.getId(stack.getItem());
            Identifier model = stack.get(DataComponentTypes.ITEM_MODEL);
            if (model == null) model = itemId;
            CustomModelDataComponent cmd = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
            String key = model + "|" + (cmd == null ? "" : cmd.toString()) + "|" + itemId;

            long now = System.currentTimeMillis();
            Entry e = cache.get(key);
            if (e != null && now - e.at < TTL_MS) return e.icon;
            if (cache.size() > 512) cache.clear();

            Icon icon = resolve(mc, itemId, model, cmd);
            cache.put(key, new Entry(icon, now));
            return icon;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Icon resolve(MinecraftClient mc, Identifier itemId, Identifier model, CustomModelDataComponent cmd)
            throws java.io.IOException {
        JsonObject def = readJson(mc, Identifier.of(model.getNamespace(), "items/" + model.getPath() + ".json"));
        if (def == null) return null;
        String modelRef = pickModel(def.get("model"), cmd, 0);
        if (modelRef == null) return null;
        Identifier tex = layer0(mc, modelRef);
        if (tex == null) return null;

        Optional<Resource> res = mc.getResourceManager().getResource(
                Identifier.of(tex.getNamespace(), "textures/" + tex.getPath() + ".png"));
        if (res.isEmpty()) return null;

        boolean vanillaPack = "vanilla".equals(res.get().getPackId());
        if (vanillaPack && model.equals(itemId) && cmd == null) return null;

        byte[] png;
        try (InputStream in = res.get().getInputStream()) {
            png = squarePng(in.readAllBytes());
        }
        if (png == null || png.length == 0 || png.length > MAX_PNG) return null;
        return new Icon(sha(png), Base64.getEncoder().encodeToString(png));
    }

    private static String pickModel(JsonElement node, CustomModelDataComponent cmd, int depth) {
        if (node == null || !node.isJsonObject() || depth > 16) return null;
        JsonObject o = node.getAsJsonObject();
        String type = strip(str(o, "type"));
        String prop = strip(str(o, "property"));
        boolean byCmd = "custom_model_data".equals(prop);
        int index = o.has("index") ? o.get("index").getAsInt() : 0;

        switch (type) {
            case "model":
                return str(o, "model");
            case "composite": {
                JsonArray arr = o.has("models") && o.get("models").isJsonArray() ? o.getAsJsonArray("models") : null;
                return (arr != null && !arr.isEmpty()) ? pickModel(arr.get(0), cmd, depth + 1) : null;
            }
            case "condition": {
                boolean flag = false;
                if (byCmd && cmd != null) {
                    Boolean f = cmd.getFlag(index);
                    flag = f != null && f;
                }
                return pickModel(o.get(flag ? "on_true" : "on_false"), cmd, depth + 1);
            }
            case "select": {
                String v = (byCmd && cmd != null) ? cmd.getString(index) : null;
                if (v != null && o.has("cases") && o.get("cases").isJsonArray()) {
                    for (JsonElement c : o.getAsJsonArray("cases")) {
                        if (!c.isJsonObject()) continue;
                        JsonElement when = c.getAsJsonObject().get("when");
                        if (matches(when, v)) return pickModel(c.getAsJsonObject().get("model"), cmd, depth + 1);
                    }
                }
                return pickModel(o.get("fallback"), cmd, depth + 1);
            }
            case "range_dispatch": {
                if (byCmd && cmd != null && o.has("entries") && o.get("entries").isJsonArray()) {
                    Float f = cmd.getFloat(index);
                    if (f != null) {
                        float scale = o.has("scale") ? o.get("scale").getAsFloat() : 1f;
                        float v = f * scale;
                        JsonElement best = null;
                        float bestT = Float.NEGATIVE_INFINITY;
                        for (JsonElement en : o.getAsJsonArray("entries")) {
                            if (!en.isJsonObject() || !en.getAsJsonObject().has("threshold")) continue;
                            float t = en.getAsJsonObject().get("threshold").getAsFloat();
                            if (t <= v && t >= bestT) { bestT = t; best = en.getAsJsonObject().get("model"); }
                        }
                        if (best != null) return pickModel(best, cmd, depth + 1);
                    }
                }
                return pickModel(o.get("fallback"), cmd, depth + 1);
            }
            default:
                return null;
        }
    }

    private static boolean matches(JsonElement when, String v) {
        if (when == null) return false;
        if (when.isJsonArray()) {
            for (JsonElement w : when.getAsJsonArray()) if (w.isJsonPrimitive() && v.equals(w.getAsString())) return true;
            return false;
        }
        return when.isJsonPrimitive() && v.equals(when.getAsString());
    }

    private static Identifier layer0(MinecraftClient mc, String modelRef) {
        Map<String, String> textures = new HashMap<>();
        String ref = modelRef;
        for (int depth = 0; depth < 12 && ref != null; depth++) {
            if (ref.startsWith("builtin/")) break;
            Identifier id = Identifier.tryParse(ref);
            if (id == null) return null;
            JsonObject m = readJson(mc, Identifier.of(id.getNamespace(), "models/" + id.getPath() + ".json"));
            if (m == null) break;
            if (m.has("textures") && m.get("textures").isJsonObject()) {
                for (var en : m.getAsJsonObject("textures").entrySet()) {
                    if (en.getValue().isJsonPrimitive()) textures.putIfAbsent(en.getKey(), en.getValue().getAsString());
                }
            }
            ref = str(m, "parent");
        }
        String t = textures.get("layer0");
        for (int i = 0; i < 5 && t != null && t.startsWith("#"); i++) t = textures.get(t.substring(1));
        if (t == null || t.startsWith("#")) return null;
        return Identifier.tryParse(t);
    }

    private static byte[] squarePng(byte[] raw) {
        try {
            var img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(raw));
            if (img == null) return null;
            int w = img.getWidth(), h = img.getHeight();
            if (w <= 0 || h <= 0 || w > 256) return null;
            if (h <= w) return raw;
            var frame = img.getSubimage(0, 0, w, w);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(frame, "png", out);
            return out.toByteArray();
        } catch (Throwable t) {
            return null;
        }
    }

    private static JsonObject readJson(MinecraftClient mc, Identifier id) {
        try {
            Optional<Resource> r = mc.getResourceManager().getResource(id);
            if (r.isEmpty()) return null;
            try (InputStream in = r.get().getInputStream()) {
                JsonElement el = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                return el.isJsonObject() ? el.getAsJsonObject() : null;
            }
        } catch (Throwable t) {
            return null;
        }
    }

    private static String str(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return (e != null && e.isJsonPrimitive()) ? e.getAsString() : null;
    }

    private static String strip(String s) {
        if (s == null) return "";
        s = s.toLowerCase(Locale.ROOT);
        return s.startsWith("minecraft:") ? s.substring(10) : s;
    }

    static String sha(byte[] b) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(b);
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 8; i++) sb.append(Character.forDigit((d[i] >> 4) & 0xF, 16)).append(Character.forDigit(d[i] & 0xF, 16));
            return sb.toString();
        } catch (Exception e) {
            return "0000000000000000";
        }
    }
}
