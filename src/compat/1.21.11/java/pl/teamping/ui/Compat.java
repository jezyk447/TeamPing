package pl.teamping.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Consumer;

public final class Compat {

    private Compat() {}

    public static boolean isKeyPressed(MinecraftClient mc, int code) {
        return InputUtil.isKeyPressed(mc.getWindow(), code);
    }

    public static RenderLayer entityTranslucent(Identifier tex) {
        return RenderLayers.entityTranslucent(tex);
    }

    public static Identifier headTexture(PlayerListEntry e) {
        if (e == null) return null;
        try {
            var body = e.getSkinTextures().body();
            return body == null ? null : body.texturePath();
        } catch (Throwable t) {
            return null;
        }
    }

    public static String skinUrl(PlayerListEntry e) {
        if (e == null) return null;
        try {
            var profile = e.getProfile();
            if (profile == null) return null;
            var props = profile.properties().get("textures");
            if (props == null || props.isEmpty()) return null;
            var prop = props.iterator().next();
            String json = new String(Base64.getDecoder().decode(prop.value()), StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject tx = root.getAsJsonObject("textures");
            if (tx == null) return null;
            JsonObject skin = tx.getAsJsonObject("SKIN");
            if (skin == null || !skin.has("url")) return null;
            return skin.get("url").getAsString();
        } catch (Throwable t) {
            return null;
        }
    }

    public static net.minecraft.util.math.Vec3d cameraPos(Camera camera) {
        return camera.getCameraPos();
    }

    public static net.minecraft.util.math.Vec3d entityPos(Entity e) {
        return e.getEntityPos();
    }

    public static PositionedSoundInstance uiSound(SoundEvent e, float pitch, float volume) {
        return PositionedSoundInstance.ui(e, pitch, volume);
    }

    public static Style fontStyle(Identifier font) {
        return Style.EMPTY.withFont(new StyleSpriteSource.Font(font));
    }

    public static void registerWorldRender(Consumer<pl.teamping.WorldFrame> cb) {
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> cb.accept(wrap(ctx)));
    }

    private static pl.teamping.WorldFrame wrap(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return new pl.teamping.WorldFrame() {
            public org.joml.Matrix4f projectionMatrix() {
                float fov = (float) (double) mc.options.getFov().getValue();
                return mc.gameRenderer.getBasicProjectionMatrix(fov);
            }
            public net.minecraft.util.math.Vec3d cameraPos() {
                return mc.gameRenderer.getCamera().getCameraPos();
            }
            public net.minecraft.client.util.math.MatrixStack matrixStack() { return ctx.matrices(); }
            public net.minecraft.client.render.VertexConsumerProvider consumers() { return ctx.consumers(); }
        };
    }
}
