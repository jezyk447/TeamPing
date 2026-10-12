package pl.teamping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class Config {


    
    public String channel = "";

    
    public List<String> channels = new ArrayList<>(List.of(
            "kanal-1", "kanal-2", "kanal-3", "kanal-4"));




    
    public double maxPingDistance = 200.0;
    
    public int lifetimeSeconds = 20;
    
    public double renderDistance = 500.0;
    
    public boolean renderDistanceUnlimited = true;
    
    public int cooldownMs = 1000;
    
    public boolean crossDimension = true;
    
    public Map<String, String> playerFlags = new LinkedHashMap<>();
    
    public double worldBorder = 200000.0;


    
    public int pingKey = 86;
    public int menuKey = 48;


    
    public double scale = 1.0;
    public boolean showSender = true;
    public boolean showDistance = true;
    
    public boolean showHead = true;
    public boolean edgeArrows = true;
    
    public boolean beam = false;


    public boolean sound = true;
    
    public boolean clickSfx = true;
    
    public int pingSound = 1;
    
    public int clickSound = 1;
    
    public double soundVolume = 0.7;


    
    public long savedAt = 0;


    
    public double uiFx = -1;
    public double uiFy = -1;


    
    public int uiAccent = 0;
    
    public int uiAccentCustom = 0;

    
    public int uiBg = 0;
    
    public int uiBgCustom = 0;
    
    public int uiBg2 = 0;
    
    public int uiBg2Custom = 0;

    
    public double uiOpacity = 0.60;
    
    public boolean uiBlur = true;
}
