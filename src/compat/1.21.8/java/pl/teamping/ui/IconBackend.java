package pl.teamping.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;


public final class IconBackend {

    private IconBackend() {}

    
    public static void draw(DrawContext ctx, Identifier tex, int x, int y, int w, int h,
                            int texW, int texH, int color) {

        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex, x, y, 0.0f, 0.0f, w, h, texW, texH, texW, texH, color);
    }

    
    public static void drawRegion(DrawContext ctx, Identifier tex, int x, int y, int w, int h,
                                  float u, float v, int regionW, int regionH,
                                  int texW, int texH, int color) {
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, regionW, regionH, texW, texH, color);
    }

    
    public static void pushScale(DrawContext ctx, float s) {
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(s, s);
    }

    public static void popMatrix(DrawContext ctx) {
        ctx.getMatrices().popMatrix();
    }

    
    public static void pushPopup(DrawContext ctx, float cx, float cy, float s, float ty) {
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(0f, ty);
        m.translate(cx, cy);
        m.scale(s, s);
        m.translate(-cx, -cy);
    }




    
    public static void pushRotate(DrawContext ctx, float tx, float ty, float angleRad) {
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(tx, ty);
        m.rotate(angleRad);
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


    
    private static final class CaptureTexture extends AbstractTexture {
        CaptureTexture(GpuTexture t, GpuTextureView v) {
            this.glTexture = t;
            this.glTextureView = v;
        }
    }

    private static CaptureTexture capTex;
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
                GpuTexture t = RenderSystem.getDevice().createTexture("teamping-blur",
                        GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                        TextureFormat.RGBA8, pw, ph, 1, 1);
                t.setTextureFilter(FilterMode.LINEAR, false);

                t.setAddressMode(AddressMode.CLAMP_TO_EDGE);
                GpuTextureView v = RenderSystem.getDevice().createTextureView(t);
                capTex = new CaptureTexture(t, v);
                mc.getTextureManager().registerTexture(capId, capTex);
            }

            int srcY = fbH - (pyTop + ph);
            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
                    fb.getColorAttachment(), capTex.getGlTexture(), 0, 0, 0, px, srcY, pw, ph);
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
        NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> name, img);
        tex.setFilter(true, false);
        Identifier id = Identifier.of("teamping", name);
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
        return id;
    }

    
    public static Identifier uploadHead(String name, NativeImage img) {
        NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> name, img);
        tex.setFilter(false, false);
        Identifier id = Identifier.of("teamping", name);
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
        return id;
    }

    
    public static Identifier uploadIcon(String name, NativeImage img) {
        NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> name, img);
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
            NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "teamping_white", img);
            tex.setFilter(false, false);
            WHITE = Identifier.of("teamping", "white");
            MinecraftClient.getInstance().getTextureManager().registerTexture(WHITE, tex);
        }
        return WHITE;
    }
}
