package pl.teamping.ui;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;


public final class IconBackend {

    private IconBackend() {}

    
    public static void draw(DrawContext ctx, Identifier tex, int x, int y, int w, int h,
                            int texW, int texH, int color) {

        ctx.drawTexture(RenderLayer::getGuiTextured, tex, x, y, 0.0f, 0.0f, w, h, texW, texH, texW, texH, color);
    }

    
    public static void drawRegion(DrawContext ctx, Identifier tex, int x, int y, int w, int h,
                                  float u, float v, int regionW, int regionH,
                                  int texW, int texH, int color) {
        ctx.drawTexture(RenderLayer::getGuiTextured, tex, x, y, u, v, w, h, regionW, regionH, texW, texH, color);
    }

    
    public static void pushScale(DrawContext ctx, float s) {
        ctx.getMatrices().push();
        ctx.getMatrices().scale(s, s, 1f);
    }

    public static void popMatrix(DrawContext ctx) {
        ctx.getMatrices().pop();
    }

    
    public static void pushPopup(DrawContext ctx, float cx, float cy, float s, float ty) {
        var m = ctx.getMatrices();
        m.push();
        m.translate(0f, ty, 0f);
        m.translate(cx, cy, 0f);
        m.scale(s, s, 1f);
        m.translate(-cx, -cy, 0f);
    }




    
    public static void pushRotate(DrawContext ctx, float tx, float ty, float angleRad) {
        var m = ctx.getMatrices();
        m.push();
        m.translate(tx, ty, 0f);
        m.multiply(RotationAxis.POSITIVE_Z.rotation(angleRad));
    }



    
    public static int framebufferWidth() {
        try {
            return MinecraftClient.getInstance().getFramebuffer().textureWidth;
        } catch (Throwable t) {
            return 0;
        }
    }

    public static int framebufferHeight() {
        try {
            return MinecraftClient.getInstance().getFramebuffer().textureHeight;
        } catch (Throwable t) {
            return 0;
        }
    }


    private static NativeImageBackedTexture capTex;
    private static Identifier capId;
    private static int capW, capH;
    private static String capError = "-";

    
    public static String lastCaptureError() {
        return capError;
    }

    
    public static Identifier captureScreen(int px, int pyTop, int pw, int ph) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            var fb = mc.getFramebuffer();
            int fbW = fb.textureWidth, fbH = fb.textureHeight;
            if (pw <= 0 || ph <= 0 || fbW <= 0 || fbH <= 0) {
                capError = "pusty bufor gry";
                return null;
            }

            if (px < 0 || pyTop < 0 || px + pw > fbW || pyTop + ph > fbH) {
                capError = "prostokat poza ekranem";
                return null;
            }

            if (capTex == null || capW != pw || capH != ph) {
                if (capId != null) {
                    try { mc.getTextureManager().destroyTexture(capId); } catch (Throwable ignored) {}
                }
                capW = pw;
                capH = ph;
                capId = Identifier.of("teamping", "blur/frame");
                capTex = new NativeImageBackedTexture(new NativeImage(NativeImage.Format.RGBA, pw, ph, false));
                capTex.setFilter(true, false);

                GlStateManager._activeTexture(GL13.GL_TEXTURE0);
                GlStateManager._bindTexture(capTex.getGlId());
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
                mc.getTextureManager().registerTexture(capId, capTex);
            }

            int srcY = fbH - (pyTop + ph);
            fb.beginWrite(false);
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            GlStateManager._bindTexture(capTex.getGlId());
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, px, srcY, pw, ph);
            capError = "-";
            return capId;
        } catch (Throwable t) {
            capError = t.getClass().getSimpleName() + ": " + t.getMessage();
            capTex = null;
            capId = null;
            capW = capH = 0;
            return null;
        }
    }

    
    public static Identifier uploadGlyph(String name, int[] argb, int w, int h) {
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, w, h, false);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setColorArgb(x, y, argb[y * w + x]);
            }
        }
        NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
        tex.setFilter(true, false);
        Identifier id = Identifier.of("teamping", name);
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
        return id;
    }

    
    public static Identifier uploadHead(String name, NativeImage img) {
        NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
        tex.setFilter(false, false);
        Identifier id = Identifier.of("teamping", name);
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
        return id;
    }

    
    public static Identifier uploadIcon(String name, NativeImage img) {
        NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
        tex.setFilter(true, false);
        Identifier id = Identifier.of("teamping", name);
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
        return id;
    }

    public static void destroyTexture(Identifier id) {
        MinecraftClient.getInstance().getTextureManager().destroyTexture(id);
    }


    private static Identifier WHITE;
    public static Identifier whiteTex() {
        if (WHITE == null) {
            NativeImage img = new NativeImage(NativeImage.Format.RGBA, 2, 2, false);
            for (int y = 0; y < 2; y++) for (int x = 0; x < 2; x++) img.setColorArgb(x, y, 0xFFFFFFFF);
            NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
            tex.setFilter(false, false);
            WHITE = Identifier.of("teamping", "white");
            MinecraftClient.getInstance().getTextureManager().registerTexture(WHITE, tex);
        }
        return WHITE;
    }
}
