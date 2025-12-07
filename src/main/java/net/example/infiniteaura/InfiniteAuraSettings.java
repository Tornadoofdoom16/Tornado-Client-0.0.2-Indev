package net.example.infiniteaura;

import java.util.ArrayList;
import java.util.List;

public class InfiniteAuraSettings {
    public static final InfiniteAuraSettings INSTANCE = new InfiniteAuraSettings();

    // Toggles
    public boolean enabled = false;
    
    // Rage Combat
    public boolean maceOnly = true;     
    public boolean autoSword = true;
    public boolean anchorAura = false;
    public boolean crystalAura = false; 
    public boolean trapper = false;  
    public boolean cartPvp = false;
    
    // Legit Combat
    public boolean legitCrystal = false;
    public boolean legitKillAura = false;
    public boolean elytraMace = false; // NEW
    public boolean pvpBot = false;     // NEW
    
    // Defense
    public boolean retaliation = false; 
    public boolean totemRevenge = true; 
    public boolean autoTotem = true;    
    public boolean dualTotems = false; 
    public boolean antiMaceDodge = false; 

    // Movement
    public boolean flight = false;
    public boolean speed = false;
    public boolean noFall = false;
    public boolean airJump = false; 
    public boolean jesus = false;   
    public boolean nuker = false;

    // Utility
    public boolean lowTotemAlert = false; 
    public boolean pearlBot = false;
    public boolean baritoneBot = false;
    public boolean useTargetList = false;

    public String alertBotName = "BotNameHere"; 
    
    public int delayTicks = 10;
    public int attackPackets = 3;
    public int fallHeight = 80;
    public double range = 100.0;
    public int trapperSpeed = 2; 

    public List<String> targetList = new ArrayList<>();
    public List<String> friendsList = new ArrayList<>(); 

    private InfiniteAuraSettings() {}
}