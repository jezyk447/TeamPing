package pl.teamping.ui;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

public final class Compat {

    private Compat() {}

    public static boolean isKeyPressed(MinecraftClient mc, int code) {
        return InputUtil.isKeyPressed(mc.getWindow().getHandle(), code);
    }

    public static RenderLayer entityTranslucent(Identifier tex) {
        return RenderLayer.getEntityTranslucent(tex);
    }

    public static Identifier headTexture(PlayerListEntry e) {
        if (e == null) return null;
        try {
            return e.getSkinTextures().texture();
        } catch (Throwable t) {
            return null;
        }
    }

    public static String skinUrl(PlayerListEntry e) {
        if (e == null) return null;
        try {
            return e.getSkinTextures().textureUrl();
        } catch (Throwable t) {
            return null;
        }
    }

    public static net.minecraft.util.math.Vec3d cameraPos(Camera camera) {
        return camera.getPos();
    }

    public static net.minecraft.util.math.Vec3d entityPos(Entity e) {
        return e.getPos();
    }

    public static PositionedSoundInstance uiSound(SoundEvent e, float pitch, float volume) {
        return PositionedSoundInstance.master(e, pitch, volume);
    }

    public static Style fontStyle(Identifier font) {
        return Style.EMPTY.withFont(font);
    }

    public static void registerWorldRender(Consumer<pl.teamping.WorldFrame> cb) {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> cb.accept(wrap(ctx)));
    }

    private static pl.teamping.WorldFrame wrap(WorldRenderContext ctx) {
        return new pl.teamping.WorldFrame() {
            public org.joml.Matrix4f projectionMatrix() { return ctx.projectionMatrix(); }
            public net.minecraft.util.math.Vec3d cameraPos() { return ctx.camera().getPos(); }
            public net.minecraft.client.util.math.MatrixStack matrixStack() { return ctx.matrixStack(); }
            public net.minecraft.client.render.VertexConsumerProvider consumers() { return ctx.consumers(); }
        };
    }
}
