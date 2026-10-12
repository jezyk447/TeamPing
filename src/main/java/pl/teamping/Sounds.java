package pl.teamping;

import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;


public final class Sounds {

    private Sounds() {}

    
    public static final String[] NAMES = {"Ping 1", "Ping 2", "Ping 3"};

    private static final SoundEvent[] EVENTS = new SoundEvent[NAMES.length];

    
    public static final String[] CLICK_NAMES = {"Klik 1", "Klik 2", "Klik 3"};
    private static final String[] CLICK_IDS = {"click1", "click2", "click3"};
    private static final SoundEvent[] CLICKS = new SoundEvent[CLICK_IDS.length];
    private static final java.util.Random RNG = new java.util.Random();

    
    public static void register() {
        for (int i = 0; i < NAMES.length; i++) {
            EVENTS[i] = SoundEvent.of(Identifier.of(TeamPingMod.MOD_ID, "ping" + (i + 1)));
        }
        for (int i = 0; i < CLICK_IDS.length; i++) {
            CLICKS[i] = SoundEvent.of(Identifier.of(TeamPingMod.MOD_ID, CLICK_IDS[i]));
        }
    }

    
    public static void playClick() {
        if (!TeamPingMod.CONFIG.clickSfx) return;
        int i = TeamPingMod.CONFIG.clickSound - 1;
        if (i < 0 || i >= CLICKS.length) i = 0;
        playClick(i);
    }

    
    public static void playClick(int index) {
        if (index < 0 || index >= CLICKS.length || CLICKS[index] == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getSoundManager() == null) return;
        mc.getSoundManager().play(pl.teamping.ui.Compat.uiSound(CLICKS[index], 1.0f, 0.5f));
    }

    
    public static SoundEvent byNumber(int n) {
        int i = n - 1;
        if (i < 0 || i >= EVENTS.length || EVENTS[i] == null) i = 0;
        return EVENTS[i];
    }

    
    public static void play(int number, double volume, float pitch) {
        SoundEvent e = byNumber(number);
        if (e == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getSoundManager() == null) return;
        mc.getSoundManager().play(pl.teamping.ui.Compat.uiSound(e, pitch, (float) volume));
    }
}
