package net.example.infiniteaura;

import java.util.ArrayList;
import java.util.List;

public class TornadoClientSettings {
    public static final TornadoClientSettings INSTANCE = new TornadoClientSettings();

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

    // Teleport utilities
    public boolean clickTp = false;
    public int clickTpRange = 70; // server-limited, default safe
    public boolean doorTp = false;
    public int doorTpRange = 70;

    // FairPlay
    public boolean fairPlayEnabled = true; // enabled by default for testing loopback
    public boolean fairPlayNetworkEmit = false; // send chat-based signals to server when enabled
    public boolean fairPlayTelemetry = true; // show telemetry logs
    // Respect-mode: skip targets who announced FairPlay recently
    public boolean fairPlayRespectSignals = false; // OFF by default
    public long fairPlayRespectTtlMs = 500L; // default TTL 500ms

    public String alertBotName = "BotNameHere"; 
    
    public int delayTicks = 10;
    public int attackPackets = 3;
    public int fallHeight = 80;
    public double range = 100.0;
    public int trapperSpeed = 2; 

    public List<String> targetList = new ArrayList<>();
    public List<String> friendsList = new ArrayList<>(); 

    private TornadoClientSettings() {}
}