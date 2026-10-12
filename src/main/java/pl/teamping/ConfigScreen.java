package pl.teamping;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import pl.teamping.ui.Anim;
import pl.teamping.ui.IconBackend;
import pl.teamping.ui.Shapes;
import pl.teamping.ui.Icons;
import pl.teamping.ui.Theme;
import pl.teamping.ui.TpButton;
import pl.teamping.ui.TpChannel;
import pl.teamping.ui.TpPrivateChannel;
import pl.teamping.ui.TpChoice;
import pl.teamping.ui.PanelBlur;
import pl.teamping.ui.TpColorPicker;
import pl.teamping.ui.TpColorRow;
import pl.teamping.ui.TpKeyBind;
import pl.teamping.ui.TpSlider;
import pl.teamping.ui.TpToggle;
import pl.teamping.ui.TpWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ConfigScreen extends pl.teamping.ui.CompatScreen {

    private static final int MAX_W = 476;
    private static final int MAX_H = 316;
    private static final int RAIL_W = 150;
    private static final int HEADER_H = 42;
    private static final int FOOTER_H = 26;
    private static final int PAD = 13;
    private static final int CARD_PAD = 12;
    private static final int NAV_H = 22;
    private static final int GAP = 10;
    private static final int TOOL = 15;

    private int W = MAX_W;
    private int H = MAX_H;

    private record Cat(String label, Identifier icon, String desc, String section) {}

    private static final Cat[] TABS = {
            new Cat("Kanały",   Icons.CHANNELS, "Dołącz do kanału swojej ekipy",   "FUNKCJE"),
            new Cat("Wygląd",   Icons.VISUALS,  "Jak wyglądają znaczniki na HUD",  "FUNKCJE"),
            new Cat("Ping",     Icons.PING,     "Zasięg, czas znacznika i odstęp",  "FUNKCJE"),
            new Cat("Dźwięk",   Icons.GEAR,     "Powiadomienia dźwiękowe",         "INNE"),
            new Cat("Klawisze", Icons.KEYBOARD, "Przypisania klawiszy moda",       "INNE"),
            new Cat("Interfejs", Icons.EYE,  "Kolory i krycie tego okna",        "INNE"),
    };

    private final Screen parent;
    private Config cfg;

    private int winX, winY;
    private int tab = 0;

    private boolean dragging;
    private int dragDX, dragDY;

    private boolean pendingClose = false;

    private boolean searchOpen = false;
    private String query = "";

    private int scroll = 0;
    private int contentH = 0;

    private String lastChannel;
    private String savedJson;
    private long dirtySince = 0;
    private long savedAt = 0;

    private long openedAt = System.nanoTime();
    private long closingAt = 0;
    private final Anim[] navHot = new Anim[TABS.length];
    private final Anim navSel = new Anim(0, Anim.MED);
    private final Anim searchAnim = new Anim(0, Anim.FAST);
    private final Anim toolHot = new Anim(0, Anim.FAST);
    private final Anim closeHot = new Anim(0, Anim.FAST);
    private long lastFrame = System.nanoTime();
    private long builtAt = System.nanoTime();

    private final int[] navRelY = new int[TABS.length];
    private final List<Anchored> anchored = new ArrayList<>();
    private final List<int[]> cards = new ArrayList<>();

    private TpSlider scaleSlider, lifeSlider, renderSlider, aimSlider, cooldownSlider, volumeSlider;
    private TpToggle senderToggle, distToggle, headToggle, arrowsToggle, beamToggle;
    private TpToggle crossToggle, soundToggle, clickSfxToggle;
    private TpChoice soundChoice, clickChoice;
    private TpKeyBind pingKeyBind, menuKeyBind;
    private TpColorRow accentRow, bgRow, bg2Row;
    private TpButton resetButton;

    private long resetArmedAt = 0;
    private TpSlider uiOpacitySlider;
    private TpToggle blurToggle;

    private TpPrivateChannel privateRow;
    private String privateDraft;
    private boolean privateFocus;

    private static final int PICK_NONE = -1, PICK_ACCENT = 0, PICK_BG = 1, PICK_BG2 = 2;
    private static final int POP_W = 214, POP_H = 124;
    private int picking = PICK_NONE;
    private TpColorPicker popupPicker;
    private int popX, popY;

    private record Anchored(TpWidget widget, int relX, int relY) {}

    private int cursorY;
    private int cardStart = -1;

    public ConfigScreen(Screen parent) {
        super(Text.literal("TeamPing"));
        this.parent = parent;
        this.cfg = ConfigManager.copyOf(TeamPingMod.CONFIG);
        this.savedJson = ConfigManager.json(this.cfg);
        this.lastChannel = this.cfg.channel;
        this.privateDraft = privateCode(this.cfg.channel);
        for (int i = 0; i < navHot.length; i++) navHot[i] = new Anim(0, Anim.FAST);
    }

    @Override
    protected void init() {

        W = MAX_W;
        H = MAX_H;

        applyStoredPosition();

        TeamPingMod.CONFIG = cfg;
        navSel.snap(tab);
        openedAt = System.nanoTime();
        closingAt = 0;

        pushRaster();
        try {
            buildTab();
        } finally {
            Theme.RASTER_PX_PER_UNIT = 0;
        }
    }

    private void applyStoredPosition() {
        if (cfg.uiFx < 0 || cfg.uiFy < 0) {
            winX = (this.width - W) / 2;
            winY = (this.height - H) / 2;
        } else {
            winX = (int) Math.round(cfg.uiFx * this.width - W / 2.0);
            winY = (int) Math.round(cfg.uiFy * this.height - H / 2.0);
        }
        clampWindow();
        storePosition();
    }

    private void storePosition() {
        if (this.width > 0 && this.height > 0) {
            cfg.uiFx = (winX + W / 2.0) / this.width;
            cfg.uiFy = (winY + H / 2.0) / this.height;
        }
    }

    private void pushRaster() {
        double gs = BASE_GUI_SCALE;
        try {
            gs = MinecraftClient.getInstance().getWindow().getScaleFactor();
        } catch (Throwable ignored) {
        }
        Theme.RASTER_PX_PER_UNIT = Math.max(1.0, gs * uiK());
    }

    private static final double BASE_GUI_SCALE = 2.0;

    private float uiK() {
        double s = BASE_GUI_SCALE;
        try {
            s = MinecraftClient.getInstance().getWindow().getScaleFactor();
        } catch (Throwable ignored) {
        }
        if (s <= 0) s = BASE_GUI_SCALE;
        double k = BASE_GUI_SCALE / s;

        if (this.width > 8) k = Math.min(k, (this.width - 8.0) / MAX_W);
        if (this.height > 8) k = Math.min(k, (this.height - 8.0) / MAX_H);
        return (float) Math.max(0.05, k);
    }

    private double lx(double mx) {
        double cx = winX + W / 2.0;
        return cx + (mx - cx) / uiK();
    }

    private double ly(double my) {
        double cy = winY + H / 2.0;
        return cy + (my - cy) / uiK();
    }

    private void clampWindow() {

        int sw = Math.round(W * uiK()), sh = Math.round(H * uiK());
        int offX = (W - sw) / 2, offY = (H - sh) / 2;
        winX = Math.max(4 - offX, Math.min(this.width - 4 - sw - offX, winX));
        winY = Math.max(4 - offY, Math.min(this.height - 4 - sh - offY, winY));
    }

    private int contentX() { return winX + RAIL_W + PAD; }
    private int contentW() { return W - RAIL_W - PAD * 2; }
    private int contentTop() { return winY + HEADER_H + 12; }
    private int contentBottom() { return winY + H - FOOTER_H - 6; }
    private int viewH() { return contentBottom() - contentTop(); }
    private int rowX() { return contentX() + CARD_PAD; }
    private int rowW() { return contentW() - CARD_PAD * 2; }
    private int maxScroll() { return Math.max(0, contentH - viewH()); }

    private <T extends TpWidget> T add(T w) {
        addDrawableChild(w);
        w.setOnChanged(this::touch);
        anchored.add(new Anchored(w, w.getX() - winX, w.getY() - winY));
        return w;
    }

    private <T extends TpWidget> T row(T w) {
        add(w);
        cursorY += w.getHeight() + GAP;
        return w;
    }

    private void beginCard() {
        cardStart = cursorY;
    }

    private void endCard() {
        if (cardStart >= 0 && cursorY > cardStart) {
            int top = cardStart - CARD_PAD;
            int bottom = cursorY - GAP + CARD_PAD;
            cards.add(new int[]{contentX() - winX, top - winY, contentW(), bottom - top});
            cursorY = bottom + 14;
        }
        cardStart = -1;
    }

    private boolean pass(String label) {
        if (query.isEmpty()) return true;
        return label.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    private boolean searching() {
        return !query.isEmpty();
    }

    private void layoutNav() {
        int y = winY + 56;
        String lastSection = null;
        for (int i = 0; i < TABS.length; i++) {
            if (!TABS[i].section().equals(lastSection)) {
                if (lastSection != null) y += 8;
                y += 13;
                lastSection = TABS[i].section();
            }
            navRelY[i] = y - winY;
            y += NAV_H + 3;
        }
    }

    private void buildTab() {
        if (privateRow != null) {
            privateDraft = privateRow.getCode();
            privateFocus = privateRow.isFocused();
            privateRow = null;
        }
        clearChildren();
        anchored.clear();
        cards.clear();
        layoutNav();

        scaleSlider = lifeSlider = renderSlider = aimSlider = cooldownSlider = volumeSlider = null;
        senderToggle = distToggle = headToggle = arrowsToggle = null;
        crossToggle = soundToggle = clickSfxToggle = null;
        soundChoice = clickChoice = null;
        pingKeyBind = menuKeyBind = null;
        accentRow = bgRow = bg2Row = null;
        resetButton = null;
        resetArmedAt = 0;
        uiOpacitySlider = null;
        blurToggle = null;

        cursorY = contentTop();
        cardStart = -1;

        if (searching()) {

            beginCard();
            buildLook();
            buildPing();
            buildSound();
            buildKeys();
            buildInterface();
            endCard();
        } else {
            switch (tab) {
                case 0 -> buildChannels();
                case 1 -> { beginCard(); buildLook(); endCard(); }
                case 2 -> { beginCard(); buildPing(); endCard(); }
                case 3 -> { beginCard(); buildSound(); endCard(); }
                case 4 -> { beginCard(); buildKeys(); endCard(); }
                default -> { beginCard(); buildInterface(); endCard(); }
            }
        }

        contentH = Math.max(0, cursorY - contentTop());
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
        applyPositions();
        builtAt = System.nanoTime();
    }

    private void buildChannels() {
        String activeRoster = rosterText();
        for (String nameRaw : cfg.channels) {
            final String name = nameRaw;
            if (!pass(name)) continue;
            boolean on = name.equals(cfg.channel);
            String sub = on ? activeRoster : "kliknij, aby dołączyć";
            row(new TpChannel(rowX(), cursorY, rowW(), name, on, sub, () -> joinChannel(name)));
        }
        if (pass("Kanał prywatny")) {
            boolean on = privateCode(cfg.channel) != null;
            String sub = on ? activeRoster : "ten sam kod = ten sam kanał";
            privateRow = row(new TpPrivateChannel(rowX(), cursorY, rowW(), privateDraft, privateFocus, on, sub,
                    code -> joinChannel(PRIVATE_PREFIX + Integer.parseInt(code))));
        }
        if (cursorY > contentTop()) {

            int top = contentTop() - CARD_PAD;
            int bottom = cursorY - GAP + CARD_PAD;
            cards.add(new int[]{contentX() - winX, top - winY, contentW(), bottom - top});
            cursorY = bottom + 14;
        }
        if (pass("Rozłącz") && !cfg.channel.isEmpty()) {
            row(new TpButton(rowX(), cursorY, 90, "Rozłącz", false, () -> joinChannel("")));
        }
    }

    private void buildLook() {
        if (pass("Rozmiar znacznika")) {
            scaleSlider = row(new TpSlider(rowX(), cursorY, rowW(), "Rozmiar znacznika", 0.5, 3.0, 0.05,
                    cfg.scale, false, v -> String.format(Locale.ROOT, "%.2fx", v)));
        }
        if (pass("Główka gracza")) {
            headToggle = row(new TpToggle(rowX(), cursorY, rowW(), "Główka gracza", cfg.showHead));
        }
        if (pass("Nick gracza")) {
            senderToggle = row(new TpToggle(rowX(), cursorY, rowW(), "Nick gracza", cfg.showSender));
        }
        if (pass("Odległość")) {
            distToggle = row(new TpToggle(rowX(), cursorY, rowW(), "Odległość", cfg.showDistance));
        }
        if (pass("Strzałki przy krawędzi")) {
            arrowsToggle = row(new TpToggle(rowX(), cursorY, rowW(), "Strzałki przy krawędzi", cfg.edgeArrows));
        }
    }

    private void buildPing() {
        if (pass("Czas znacznika")) {
            lifeSlider = row(new TpSlider(rowX(), cursorY, rowW(), "Czas znacznika", 3, 120, 1,
                    cfg.lifetimeSeconds, false, v -> (int) v + " s"));
        }
        if (pass("Zasięg znaczników")) {
            renderSlider = row(new TpSlider(rowX(), cursorY, rowW(), "Zasięg znaczników", 32, 2000, 8,
                    cfg.renderDistance, true, v -> (int) v + " m"));
            renderSlider.setInfinite(cfg.renderDistanceUnlimited);
        }
        if (pass("Zasięg celowania")) {
            aimSlider = row(new TpSlider(rowX(), cursorY, rowW(), "Zasięg celowania", 16, 512, 4,
                    cfg.maxPingDistance, false, v -> (int) v + " m"));
        }
        if (pass("Odstęp między pingami")) {
            cooldownSlider = row(new TpSlider(rowX(), cursorY, rowW(), "Odstęp między pingami", 0, 2000, 50,
                    cfg.cooldownMs, false, v -> (int) v + " ms"));
        }
        if (pass("Pingi z Netheru")) {
            crossToggle = row(new TpToggle(rowX(), cursorY, rowW(), "Pingi z Netheru (1:8)", cfg.crossDimension));
        }
        if (pass("Waypoint")) {
            beamToggle = row(new TpToggle(rowX(), cursorY, rowW(), "Waypoint", cfg.beam));
        }
    }

    private void buildSound() {
        if (pass("Dźwięk pinga")) {
            soundToggle = row(new TpToggle(rowX(), cursorY, rowW(), "Dźwięk pinga", cfg.sound));
        }
        if (pass("Dźwięk pinga")) {
            cursorY += 12;
            soundChoice = row(new TpChoice(rowX(), cursorY, rowW(), Sounds.NAMES, Math.max(0, cfg.pingSound - 1))
                    .label("Dźwięk pinga")
                    .onChange(() -> Sounds.play(soundChoice.getIndex() + 1,
                            volumeSlider != null ? volumeSlider.getValue() : cfg.soundVolume, 1.0f)));
        }
        if (pass("Głośność")) {
            volumeSlider = row(new TpSlider(rowX(), cursorY, rowW(), "Głośność", 0.1, 1.0, 0.05,
                    cfg.soundVolume, false, v -> (int) Math.round(v * 100) + "%"));
        }
        if (pass("Dźwięk kliknięć")) {
            clickSfxToggle = row(new TpToggle(rowX(), cursorY, rowW(), "Dźwięk kliknięć w menu", cfg.clickSfx));
        }
        if (pass("Dźwięk kliknięcia")) {
            cursorY += 12;
            clickChoice = row(new TpChoice(rowX(), cursorY, rowW(), Sounds.CLICK_NAMES, Math.max(0, cfg.clickSound - 1))
                    .label("Dźwięk kliknięcia")
                    .onChange(() -> Sounds.playClick(clickChoice.getIndex())));
        }

        if (pass("Posłuchaj")) {
            if (soundChoice != null) {
                row(new TpButton(rowX(), cursorY, 104, "Posłuchaj ping", false,
                        () -> Sounds.play(soundChoice.getIndex() + 1,
                                volumeSlider != null ? volumeSlider.getValue() : cfg.soundVolume, 1.0f)));
            }
            if (clickChoice != null) {
                row(new TpButton(rowX(), cursorY, 104, "Posłuchaj klik", false,
                        () -> Sounds.playClick(clickChoice.getIndex())));
            }
        }
    }

    private void buildKeys() {
        if (pass("Postaw ping")) {
            pingKeyBind = row(new TpKeyBind(rowX(), cursorY, rowW(), "Postaw ping", cfg.pingKey));
        }
        if (pass("Otwórz to okno")) {
            menuKeyBind = row(new TpKeyBind(rowX(), cursorY, rowW(), "Otwórz to okno", cfg.menuKey));
        }
    }

    private static int[] accentPalette() {
        int[] out = new int[Theme.ACCENTS.length];
        for (int i = 0; i < out.length; i++) out[i] = Theme.ACCENTS[i][0];
        return out;
    }

    private void buildInterface() {
        if (pass("Kolor akcentu")) {
            accentRow = row(new TpColorRow(rowX(), cursorY, rowW(), "Kolor akcentu",
                    accentPalette(), cfg.uiAccent, cfg.uiAccentCustom, () -> openPicker(PICK_ACCENT)));
            accentRow.setOnChanged(() -> {
                cfg.uiAccent = accentRow.getIndex();
                cfg.uiAccentCustom = accentRow.getCustom();
                touch();
            });
        }
        if (pass("Tło panelu")) {
            bgRow = row(new TpColorRow(rowX(), cursorY, rowW(), "Tło panelu",
                    Theme.BACKGROUNDS, cfg.uiBg, cfg.uiBgCustom, () -> openPicker(PICK_BG)));
            bgRow.setOnChanged(() -> {
                cfg.uiBg = bgRow.getIndex();
                cfg.uiBgCustom = bgRow.getCustom();
                touch();
            });
        }
        if (pass("Tło opcji")) {
            bg2Row = row(new TpColorRow(rowX(), cursorY, rowW(), "Tło opcji",
                    Theme.SURFACES, cfg.uiBg2, cfg.uiBg2Custom, () -> openPicker(PICK_BG2)));
            bg2Row.setOnChanged(() -> {
                cfg.uiBg2 = bg2Row.getIndex();
                cfg.uiBg2Custom = bg2Row.getCustom();
                touch();
            });
        }
        if (pass("Krycie panelu")) {
            uiOpacitySlider = row(new TpSlider(rowX(), cursorY, rowW(), "Krycie panelu", 0.4, 1.0, 0.05,
                    cfg.uiOpacity, false, v -> String.format(Locale.ROOT, "%.0f%%", v * 100)));
        }
        if (pass("Rozmycie interfejsu")) {
            blurToggle = row(new TpToggle(rowX(), cursorY, rowW(), "Rozmycie interfejsu", cfg.uiBlur));
        }
        if (pass("Ustawienia domyślne")) {
            cursorY += 6;
            resetButton = row(new TpButton(rowX(), cursorY, 168, "Ustawienia domyślne", false,
                    this::onReset).danger());
        }
    }

    private void onReset() {
        long now = System.currentTimeMillis();
        if (now - resetArmedAt > 5000) {
            resetArmedAt = now;
            if (resetButton != null) resetButton.setLabel("Na pewno? Kliknij ponownie");
            return;
        }
        resetArmedAt = 0;

        String oldChannel = cfg.channel;
        Config fresh = new Config();

        fresh.uiFx = cfg.uiFx;
        fresh.uiFy = cfg.uiFy;

        cfg = fresh;
        TeamPingMod.CONFIG = fresh;
        ConfigManager.save(fresh);
        savedJson = ConfigManager.json(fresh);
        savedAt = System.currentTimeMillis();
        dirtySince = 0;

        if (!java.util.Objects.equals(oldChannel, fresh.channel)) {
            lastChannel = fresh.channel;
            TeamPingMod.restartTransport();
        }

        scroll = 0;
        buildTab();
        Sounds.playClick();
    }

    private int pickedNow(int which) {
        return switch (which) {
            case PICK_BG -> (cfg.uiBgCustom >>> 24) != 0 ? cfg.uiBgCustom : Theme.bg();
            case PICK_BG2 -> (cfg.uiBg2Custom >>> 24) != 0 ? cfg.uiBg2Custom : Theme.card();
            default -> (cfg.uiAccentCustom >>> 24) != 0 ? cfg.uiAccentCustom : Theme.accent();
        };
    }

    private String pickerTitle() {
        return switch (picking) {
            case PICK_BG -> "Własne tło panelu";
            case PICK_BG2 -> "Własne tło opcji";
            default -> "Własny kolor akcentu";
        };
    }

    private void openPicker(int which) {
        picking = which;
        layoutPopup();
        popupPicker = new TpColorPicker(popX + 12, popY + 30, POP_W - 24, POP_H - 62, pickedNow(which));
        popupPicker.setOnChanged(() -> {
            applyPicked(popupPicker.getColor());
            touch();
        });
    }

    private void layoutPopup() {
        popX = winX + (W - POP_W) / 2;
        popY = winY + (H - POP_H) / 2;
        if (popupPicker != null) {
            popupPicker.setX(popX + 12);
            popupPicker.setY(popY + 30);
        }
    }

    private void applyPicked(int argb) {
        switch (picking) {
            case PICK_BG -> {
                cfg.uiBgCustom = argb;
                if (bgRow != null) bgRow.setCustom(argb);
            }
            case PICK_BG2 -> {
                cfg.uiBg2Custom = argb;
                if (bg2Row != null) bg2Row.setCustom(argb);
            }
            default -> {
                cfg.uiAccentCustom = argb;
                if (accentRow != null) accentRow.setCustom(argb);
            }
        }
    }

    private void closePicker() {
        picking = PICK_NONE;
        popupPicker = null;
    }

    private int[] pickerDoneBox() {
        int bw = 62, bh = 16;
        return new int[]{popX + POP_W - 12 - bw, popY + POP_H - 10 - bh, bw, bh};
    }

    private void drawPicker(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (picking == PICK_NONE || popupPicker == null) return;
        layoutPopup();

        Theme.glassPanel(ctx, popX, popY, POP_W, POP_H, 12);
        Theme.head(ctx, pickerTitle(), popX + 12, popY + 10, Theme.TEXT);

        popupPicker.render(ctx, mouseX, mouseY, delta);

        Theme.small(ctx, popupPicker.hex(), popX + 12, popY + POP_H - 20, Theme.TEXT_FAINT);

        int[] b = pickerDoneBox();
        boolean hot = inBox(mouseX, mouseY, b[0], b[1], b[2], b[3]);
        Theme.pill(ctx, b[0], b[1], b[2], b[3], 7, hot ? Theme.accentHover() : Theme.accent());
        Theme.textCentered(ctx, "Gotowe", b[0] + b[2] / 2, Theme.midY(b[1], b[3]), 0xFFFFFFFF);
    }

    private void applyPositions() {
        for (Anchored a : anchored) {
            a.widget().setX(winX + a.relX());
            a.widget().setY(winY + a.relY() - scroll);
        }
    }

    private static final String PRIVATE_PREFIX = "priv-";

    private static String privateCode(String channel) {
        if (channel == null || !channel.matches("priv-\\d{1,4}")) return null;
        return channel.substring(PRIVATE_PREFIX.length());
    }

    private void joinChannel(String name) {
        collect();
        cfg.channel = name;
        commit();
        buildTab();
    }

    private void touch() {
        if (dirtySince == 0) dirtySince = System.currentTimeMillis();
    }

    private void collect() {
        if (scaleSlider != null) cfg.scale = scaleSlider.getValue();
        if (senderToggle != null) cfg.showSender = senderToggle.getValue();
        if (distToggle != null) cfg.showDistance = distToggle.getValue();
        if (headToggle != null) cfg.showHead = headToggle.getValue();
        if (arrowsToggle != null) cfg.edgeArrows = arrowsToggle.getValue();
        if (beamToggle != null) cfg.beam = beamToggle.getValue();

        if (lifeSlider != null) cfg.lifetimeSeconds = lifeSlider.getIntValue();
        if (renderSlider != null) {
            cfg.renderDistance = renderSlider.getValue();
            cfg.renderDistanceUnlimited = renderSlider.isInfinite();
        }
        if (aimSlider != null) cfg.maxPingDistance = aimSlider.getValue();
        if (cooldownSlider != null) cfg.cooldownMs = cooldownSlider.getIntValue();
        if (crossToggle != null) cfg.crossDimension = crossToggle.getValue();

        if (soundToggle != null) cfg.sound = soundToggle.getValue();
        if (clickSfxToggle != null) cfg.clickSfx = clickSfxToggle.getValue();
        if (soundChoice != null) cfg.pingSound = soundChoice.getIndex() + 1;
        if (clickChoice != null) cfg.clickSound = clickChoice.getIndex() + 1;
        if (volumeSlider != null) cfg.soundVolume = volumeSlider.getValue();

        if (accentRow != null) {
            cfg.uiAccent = accentRow.getIndex();
            cfg.uiAccentCustom = accentRow.getCustom();
        }
        if (bgRow != null) {
            cfg.uiBg = bgRow.getIndex();
            cfg.uiBgCustom = bgRow.getCustom();
        }
        if (bg2Row != null) {
            cfg.uiBg2 = bg2Row.getIndex();
            cfg.uiBg2Custom = bg2Row.getCustom();
        }
        if (uiOpacitySlider != null) cfg.uiOpacity = uiOpacitySlider.getValue();
        if (blurToggle != null) cfg.uiBlur = blurToggle.getValue();

        if (pingKeyBind != null) cfg.pingKey = pingKeyBind.getKey();
        if (menuKeyBind != null) cfg.menuKey = menuKeyBind.getKey();
    }

    private void commit() {
        storePosition();
        boolean channelChanged = !Objects.equals(lastChannel, cfg.channel);
        ConfigManager.save(cfg);
        if (channelChanged) {
            TeamPingMod.restartTransport();
            lastChannel = cfg.channel;
        }
        savedJson = ConfigManager.json(cfg);
        dirtySince = 0;
        savedAt = System.currentTimeMillis();
    }

    @Override
    public void close() {
        collect();
        storePosition();
        if (!ConfigManager.json(cfg).equals(savedJson)) commit();
        startClose();
    }

    private void startClose() {
        if (pendingClose) return;
        pendingClose = true;
        closingAt = System.nanoTime();
    }

    private boolean blurFallback = false;

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {

        pushRaster();
        long now = System.nanoTime();
        double dt = (now - lastFrame) / 1_000_000_000.0;
        lastFrame = now;
        navSel.update(dt);
        searchAnim.update(dt);
        toolHot.update(dt);
        closeHot.update(dt);
        for (Anim a : navHot) a.update(dt);
        searchAnim.set(searchOpen ? 1 : 0);

        if (dirtySince != 0) {
            collect();
            storePosition();
            if (ConfigManager.json(cfg).equals(savedJson)) {
                dirtySince = 0;
            } else if (System.currentTimeMillis() - dirtySince > 180) {
                commit();
            }
        }

        int pxw = winX, pyw = winY;
        clampWindow();
        if (pxw != winX || pyw != winY) {
            layoutNav();
            applyPositions();
        }

        double since = (now - builtAt) / 1_000_000_000.0;
        for (int i = 0; i < anchored.size(); i++) {
            anchored.get(i).widget().setAppear((since - i * 0.035) / 0.20);
        }

        double prog = ((pendingClose ? now - closingAt : now - openedAt) / 1_000_000_000.0)
                / Anim.POP_SECONDS;
        double k = pendingClose ? Anim.popOut(prog) : Anim.popIn(prog);
        Theme.GLOBAL_ALPHA = pendingClose ? Anim.popOutAlpha(prog) : Anim.popInAlpha(prog);

        double kk = k * uiK();
        double bw = W * kk, bh = H * kk;
        double bx = winX + W / 2.0 - bw / 2.0;
        double by = winY + H / 2.0 - bh / 2.0;
        if (cfg.uiBlur && !PanelBlur.draw(ctx, bx, by, bw, bh, Theme.panelRadius() * kk)) {
            super.renderBackground(ctx, mouseX, mouseY, delta);
        }

        IconBackend.pushPopup(ctx, winX + W / 2f, winY + H / 2f, (float) kk, 0f);

        int lmx = (int) Math.round(lx(mouseX));
        int lmy = (int) Math.round(ly(mouseY));

        drawPanel(ctx, lmx, lmy);

        ctx.enableScissor(contentX() - CARD_PAD - 2, contentTop() - CARD_PAD - 2,
                contentX() + contentW() + 2, contentBottom() + 2);
        super.render(ctx, lmx, lmy, delta);
        ctx.disableScissor();

        if (searching() && anchored.isEmpty()) {
            Theme.text(ctx, "Nic nie znaleziono dla „" + query + "”",
                    contentX(), contentTop() + 6, Theme.TEXT_FAINT);
        }

        drawScrollbar(ctx);
        drawFooter(ctx);
        drawPicker(ctx, lmx, lmy, delta);

        IconBackend.popMatrix(ctx);
        Theme.GLOBAL_ALPHA = 1.0;
        Theme.RASTER_PX_PER_UNIT = 0;

        if (pendingClose && prog >= 1.0 && this.client != null) {
            pendingClose = false;
            this.client.setScreen(parent);
        }
    }

    private void drawPanel(DrawContext ctx, int mouseX, int mouseY) {
        Theme.glassPanel(ctx, winX, winY, W, H, Theme.panelRadius());

        drawSidebar(ctx, mouseX, mouseY);
        drawHeader(ctx, mouseX, mouseY);

        ctx.enableScissor(contentX() - CARD_PAD - 2, contentTop() - CARD_PAD - 2,
                contentX() + contentW() + 2, contentBottom() + 2);
        for (int[] c : cards) {
            Theme.card(ctx, winX + c[0], winY + c[1] - scroll, c[2], c[3], Theme.cardRadius());
        }
        ctx.disableScissor();
    }

    private static String shortVersion() {
        String v = TeamPingMod.VERSION;
        int a = v.indexOf('.');
        if (a < 0) return v;
        int b = v.indexOf('.', a + 1);
        if (b < 0) return v;
        String patch = v.substring(b + 1);
        return patch.equals("0") ? v.substring(0, b) : v;
    }

    private void drawSidebar(DrawContext ctx, int mouseX, int mouseY) {
        int lx = winX + 12;

        int logoS = 26, logoY = winY + 14;
        Theme.roundRect(ctx, lx, logoY, logoS, logoS, 8, Theme.withAlpha(0x000000, 90));
        Icons.draw(ctx, Icons.LOGO, lx + 2, logoY + 2, logoS - 4, 0xFFFFFFFF);

        int bx = lx + logoS + 9;
        Theme.brand(ctx, "TeamPing", bx, logoY + 1, Theme.TEXT);

        String ver = "Wersja " + shortVersion();
        int vx = bx + (Theme.widthBrand("TeamPing") - Theme.widthSmall(ver)) / 2;
        Theme.small(ctx, ver, vx, logoY + 15, Theme.TEXT_FAINT);

        int navX = winX + 8, navW = RAIL_W - 16;
        String lastSection = null;
        for (int i = 0; i < TABS.length; i++) {
            int ry = winY + navRelY[i];
            if (!TABS[i].section().equals(lastSection)) {
                lastSection = TABS[i].section();
                Theme.sectionLabel(ctx, lastSection, navX + 6, ry - 11, Theme.TEXT);
            }
            boolean active = i == tab && !searching();
            boolean hover = mouseX >= navX && mouseX < navX + navW && mouseY >= ry && mouseY < ry + NAV_H;
            navHot[i].set(hover && !active ? 1 : 0);

            if (active) {

                double s = navSel.get();
                int last = TABS.length - 1;
                int i0 = Math.max(0, Math.min(last, (int) Math.floor(s)));
                int i1 = Math.min(last, i0 + 1);
                double f = Anim.clamp01(s - i0);
                int sy = winY + (int) Math.round(navRelY[i0] + (navRelY[i1] - navRelY[i0]) * f);
                Theme.pill(ctx, navX, sy, navW, NAV_H, 7, Theme.accent());
            } else if (navHot[i].get() > 0.01) {
                Theme.roundRect(ctx, navX, ry, navW, NAV_H, 7, Theme.fade(Theme.HOVER, navHot[i].get()));
            }

            int color = active ? 0xFFFFFFFF
                    : Theme.lerpColor(Theme.TEXT_DIM, Theme.TEXT, navHot[i].get());
            Icons.draw(ctx, TABS[i].icon(), navX + 8, ry + (NAV_H - 13) / 2, 13, color);
            Theme.text(ctx, TABS[i].label(), navX + 27, Theme.midY(ry, NAV_H), color);
        }

        int av = 19, uy = winY + H - av - 12;
        drawAvatar(ctx, lx, uy, av);
        Theme.name(ctx, playerName(), lx + av + 8, Theme.midY(uy, av) - 1, Theme.TEXT);

    }

    private void drawAvatar(DrawContext ctx, int x, int y, int s) {
        int r = 5;
        try {
            var nh = this.client.getNetworkHandler();
            String name = playerName();
            var e = (nh != null) ? nh.getPlayerListEntry(name) : null;
            Identifier skin = pl.teamping.ui.Compat.headTexture(e);
            if (skin != null) {

                int tile = Theme.opSurface(Theme.card());
                Theme.roundRect(ctx, x + 1, y + 1, s - 2, s - 2, r - 1, tile);
                Icons.drawRegion(ctx, skin, x, y, s, s, 8f, 8f, 8, 8, 64, 64, 0xFFFFFFFF);
                Icons.drawRegion(ctx, skin, x, y, s, s, 40f, 8f, 8, 8, 64, 64, 0xFFFFFFFF);
                Shapes.cornerCutout(ctx, x, y, s, s, r, tile);
                return;
            }
        } catch (Throwable ignored) {}
        Theme.roundRect(ctx, x, y, s, s, r, Theme.accentSoft());
        Icons.draw(ctx, Icons.USER, x + 3, y + 3, s - 6, Theme.accentText());
    }

    private void drawHeader(DrawContext ctx, int mouseX, int mouseY) {
        int hx = winX + RAIL_W + PAD;
        int hy = winY + 12;

        int box = 22;
        Theme.roundRect(ctx, hx, hy, box, box, 7, Theme.card());
        Identifier icon = searching() ? Icons.SEARCH : TABS[tab].icon();
        Icons.draw(ctx, icon, hx + (box - 13) / 2, hy + (box - 13) / 2, 13, Theme.TEXT);

        int tx = hx + box + 8;
        int toolsX = winX + W - PAD - TOOL;
        int searchX = toolsX - TOOL - 3;
        double so = Anim.easeOut(searchAnim.get());

        if (so < 0.99) {

            int a = (int) Math.round(255 * (1 - so));
            String title = searching() ? "Szukaj" : TABS[tab].label();
            int titleY = hy + 2;
            int baseY = Theme.baselineOfHead(titleY);
            Theme.head(ctx, title, tx, titleY, Theme.withAlpha(Theme.TEXT, a));
            int dx = tx + Theme.widthDisplay(title) + 6;
            Theme.text(ctx, "•", dx, baseY, Theme.withAlpha(Theme.TEXT_FAINT, a));
            String desc = searching() ? query : TABS[tab].desc();
            Theme.text(ctx, ellipsis(desc, searchX - (dx + 10) - 6), dx + 10, baseY,
                    Theme.withAlpha(Theme.TEXT_DIM, a));
        }

        if (so > 0.01) {

            int fx = tx, fw = searchX - tx - 6, fh = 18;
            int fy = hy + (box - fh) / 2;
            int a = (int) Math.round(255 * so);
            Theme.roundRect(ctx, fx, fy, fw, fh, 6, Theme.withAlpha(Theme.field(), (int) (0x47 * so)));
            Theme.roundOutline(ctx, fx, fy, fw, fh, 6, Theme.withAlpha(Theme.accent(), (int) (140 * so)));
            Icons.draw(ctx, Icons.SEARCH, fx + 5, fy + (fh - 10) / 2, 10, Theme.withAlpha(Theme.TEXT_FAINT, a));
            String shown = query.isEmpty() ? "Szukaj ustawień…" : query;
            int col = query.isEmpty() ? Theme.TEXT_FAINT : Theme.TEXT;
            Theme.text(ctx, ellipsis(shown, fw - 24), fx + 19, Theme.midY(fy, fh), Theme.withAlpha(col, a));

            if (searchOpen && (System.currentTimeMillis() / 500) % 2 == 0) {

                int cx = query.isEmpty()
                        ? fx + 16
                        : fx + 19 + Theme.width(ellipsis(query, fw - 24)) + 1;
                ctx.fill(cx, fy + 4, cx + 1, fy + fh - 4, Theme.withAlpha(Theme.accentText(), a));
            }
        }

        boolean sHot = inBox(mouseX, mouseY, searchX, hy + 3, TOOL, TOOL);
        boolean cHot = inBox(mouseX, mouseY, toolsX, hy + 3, TOOL, TOOL);
        toolHot.set(sHot ? 1 : 0);
        closeHot.set(cHot ? 1 : 0);
        tool(ctx, searchX, hy + 3, Icons.SEARCH, 10, toolHot.get(),
                searchOpen ? Theme.accentText() : Theme.TEXT_DIM, Theme.TEXT);
        tool(ctx, toolsX, hy + 3, Icons.XMARK, 9, closeHot.get(), Theme.TEXT_DIM, Theme.DANGER);

    }

    private void tool(DrawContext ctx, int x, int y, Identifier icon, int size, double t,
                      int cold, int warm) {
        if (t > 0.01) Theme.roundRect(ctx, x, y, TOOL, TOOL, 4, Theme.fade(0x1FFFFFFF, t));
        int off = (TOOL - size) / 2;
        Icons.draw(ctx, icon, x + off, y + off, size, Theme.lerpColor(cold, warm, t));
    }

    private static boolean inBox(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void drawScrollbar(DrawContext ctx) {
        int max = maxScroll();
        if (max <= 0) return;
        int trackX = contentX() + contentW() + 3;
        int trackY = contentTop();
        int trackH = viewH();
        int thumbH = Math.max(16, (int) (trackH * (viewH() / (double) contentH)));
        int thumbY = trackY + (int) ((trackH - thumbH) * (scroll / (double) max));
        Theme.roundRect(ctx, trackX, trackY, 2, trackH, 1, Theme.HAIRLINE);
        Theme.roundRect(ctx, trackX, thumbY, 2, thumbH, 1, Theme.TEXT_FAINT);
    }

    private void drawFooter(DrawContext ctx) {
        int fy = winY + H - FOOTER_H;

        String status = TeamPingMod.transportStatus();
        Theme.small(ctx, ellipsis(status, contentW() - 90), contentX(), fy + 10, Theme.TEXT_FAINT);

        long age = System.currentTimeMillis() - savedAt;
        if (savedAt > 0 && age < 1400) {
            double a = age < 1000 ? 1.0 : 1.0 - (age - 1000) / 400.0;
            int col = Theme.fade(Theme.OK, a);
            int rx = winX + W - PAD;
            Theme.textRight(ctx, "Zapisano", rx, fy + 9, col);
            Icons.draw(ctx, Icons.CHECK, rx - Theme.width("Zapisano") - 12, fy + 9, 8, col);
        }
    }

    private String ellipsis(String s, int maxW) {
        if (s == null) return "";
        if (maxW <= 8) return "";
        String out = s;
        while (out.length() > 1 && Theme.width(out) > maxW) {
            out = out.substring(0, out.length() - 1);
        }
        return out.equals(s) ? s : out.substring(0, Math.max(1, out.length() - 1)) + "…";
    }

    private String rosterText() {
        List<String> who = TeamPingMod.roster();
        if (who.isEmpty()) return "tylko Ty";
        String s = String.join(", ", who);
        return s.length() > 34 ? s.substring(0, 32) + "…" : s;
    }

    private String playerName() {
        try {
            String n = this.client.getSession().getUsername();
            return n == null || n.isEmpty() ? "Gracz" : n;
        } catch (Exception e) {
            return "Gracz";
        }
    }

    @Override
    protected boolean cMouseClicked(double rawX, double rawY, int button) {
        if (pendingClose) return true;
        double mx = lx(rawX), my = ly(rawY);

        if (picking != PICK_NONE && popupPicker != null) {
            int[] b = pickerDoneBox();
            if (inBox(mx, my, b[0], b[1], b[2], b[3])) {
                Sounds.playClick();
                closePicker();
                return true;
            }
            if (popupPicker.cForwardClick(mx, my, button)) return true;
            if (!inBox(mx, my, popX, popY, POP_W, POP_H)) closePicker();
            return true;
        }
        TpKeyBind listening = listeningBind();
        if (listening != null) {
            listening.captureMouse(button);
            return true;
        }

        if (privateRow != null) privateRow.unfocus();

        if (button == 0) {
            int hy = winY + 12;
            int toolsX = winX + W - PAD - TOOL;
            int searchX = toolsX - TOOL - 3;

            if (inBox(mx, my, toolsX, hy + 3, TOOL, TOOL)) {
                Sounds.playClick();
                close();
                return true;
            }
            if (inBox(mx, my, searchX, hy + 3, TOOL, TOOL)) {
                Sounds.playClick();
                toggleSearch();
                return true;
            }

            int navX = winX + 8, navW = RAIL_W - 16;
            if (mx >= navX && mx < navX + navW) {
                for (int i = 0; i < TABS.length; i++) {
                    int ry = winY + navRelY[i];
                    if (my >= ry && my < ry + NAV_H) {
                        if (i != tab || searching()) {
                            Sounds.playClick();
                            collect();
                            tab = i;
                            query = "";
                            searchOpen = false;
                            scroll = 0;
                            navSel.set(i);
                            buildTab();
                        }
                        return true;
                    }
                }
            }

            if (my >= winY && my < winY + HEADER_H && mx >= winX && mx < winX + W) {
                dragging = true;
                dragDX = (int) rawX - winX;
                dragDY = (int) rawY - winY;
                return true;
            }
        }
        return superMouseClicked(mx, my, button);
    }

    private void toggleSearch() {
        searchOpen = !searchOpen;
        if (!searchOpen && !query.isEmpty()) {
            query = "";
            scroll = 0;
            buildTab();
        }
    }

    @Override
    protected boolean cMouseDragged(double rawX, double rawY, int button, double dx, double dy) {
        if (picking != PICK_NONE && popupPicker != null) {
            return popupPicker.cForwardDrag(lx(rawX), ly(rawY), button, dx / uiK(), dy / uiK());
        }
        if (dragging && button == 0) {
            winX = (int) rawX - dragDX;
            winY = (int) rawY - dragDY;
            clampWindow();
            storePosition();
            layoutNav();
            applyPositions();
            touch();
            return true;
        }
        return superMouseDragged(lx(rawX), ly(rawY), button, dx / uiK(), dy / uiK());
    }

    @Override
    protected boolean cMouseReleased(double rawX, double rawY, int button) {
        if (picking != PICK_NONE && popupPicker != null) {
            return popupPicker.cForwardRelease(lx(rawX), ly(rawY), button);
        }
        if (dragging && button == 0) {
            dragging = false;
            return true;
        }
        return superMouseReleased(lx(rawX), ly(rawY), button);
    }

    @Override
    public boolean mouseScrolled(double rawX, double rawY, double horizontal, double vertical) {
        double mx = lx(rawX), my = ly(rawY);
        if (maxScroll() > 0 && mx >= contentX() - CARD_PAD && mx <= contentX() + contentW()
                && my >= contentTop() - CARD_PAD && my <= contentBottom()) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.round(vertical * 18)));
            applyPositions();
            return true;
        }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }

    @Override
    protected boolean cCharTyped(char chr, int modifiers) {
        if (privateRow != null && privateRow.type(chr)) return true;
        if (searchOpen && chr >= ' ' && chr != 127) {
            query += chr;
            scroll = 0;
            buildTab();
            return true;
        }
        return superCharTyped(chr, modifiers);
    }

    @Override
    protected boolean cKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (picking != PICK_NONE && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            closePicker();
            return true;
        }
        TpKeyBind listening = listeningBind();
        if (listening != null) {
            listening.captureKey(keyCode);
            return true;
        }
        if (privateRow != null && privateRow.isFocused()) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_ESCAPE -> privateRow.unfocus();
                case GLFW.GLFW_KEY_BACKSPACE -> privateRow.backspace();
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> privateRow.enter();
                default -> { }
            }
            return true;
        }
        if (searchOpen) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                toggleSearch();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!query.isEmpty()) {
                    query = query.substring(0, query.length() - 1);
                    scroll = 0;
                    buildTab();
                }
                return true;
            }
        }
        return superKeyPressed(keyCode, scanCode, modifiers);
    }

    private TpKeyBind listeningBind() {
        if (pingKeyBind != null && pingKeyBind.isListening()) return pingKeyBind;
        if (menuKeyBind != null && menuKeyBind.isListening()) return menuKeyBind;
        return null;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
