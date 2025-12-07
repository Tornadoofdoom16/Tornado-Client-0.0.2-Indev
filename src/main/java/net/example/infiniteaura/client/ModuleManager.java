package net.example.infiniteaura.client;

import net.example.infiniteaura.modules.*;
import net.example.infiniteaura.fairplay.FairPlayModule;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    public static final ModuleManager INSTANCE = new ModuleManager();
    
    private final List<Module> modules = new ArrayList<>();
    
    // Hard references
    public InfiniteAura infiniteAura;
    public AutoTotem autoTotem;
    public AntiMace antiMace;
    public Flight flight;
    public Speed speed;
    public NoFall noFall;
    public AirJump airJump;
    public Jesus jesus;
    public LowTotemAlert lowTotemAlert;
    public PearlBot pearlBot;
    public BaritoneBot baritoneBot;
    public Trapper trapper;
    public AnchorAura anchorAura;
    public CrystalAura crystalAura; 
    public Nuker nuker;
    public CartPVP cartPVP;
    public LegitCrystal legitCrystal;
    public LegitKillAura legitKillAura;
    public ElytraMace elytraMace; // NEW
    public PVPBot pvpBot;         // NEW
    public ClickTP clickTP;
    public DoorTP doorTP;
    // FairPlay subsystem
    public FairPlayModule fairPlayModule;

    private ModuleManager() {
        infiniteAura = new InfiniteAura();
        autoTotem = new AutoTotem();
        antiMace = new AntiMace();
        flight = new Flight();
        speed = new Speed();
        noFall = new NoFall();
        airJump = new AirJump();
        jesus = new Jesus();
        lowTotemAlert = new LowTotemAlert();
        pearlBot = new PearlBot();
        baritoneBot = new BaritoneBot();
        trapper = new Trapper();
        anchorAura = new AnchorAura();
        crystalAura = new CrystalAura();
        nuker = new Nuker();
        cartPVP = new CartPVP();
        legitCrystal = new LegitCrystal();
        legitKillAura = new LegitKillAura();
        elytraMace = new ElytraMace(); // NEW
        pvpBot = new PVPBot();         // NEW
        clickTP = new ClickTP();
        doorTP = new DoorTP();

        // FairPlay module (not a "Module" subclass, separate lifecycle)
        fairPlayModule = new FairPlayModule();
        // Enable by default so opt-in announcer/listener are active in-client
        try {
            fairPlayModule.enable();
        } catch (Exception ignored) {}

        modules.add(infiniteAura);
        modules.add(autoTotem);
        modules.add(antiMace);
        modules.add(flight);
        modules.add(speed);
        modules.add(noFall);
        modules.add(airJump);
        modules.add(jesus);
        modules.add(lowTotemAlert);
        modules.add(pearlBot);
        modules.add(baritoneBot);
        modules.add(trapper);
        modules.add(anchorAura);
        modules.add(crystalAura);
        modules.add(nuker);
        modules.add(cartPVP);
        modules.add(legitCrystal);
        modules.add(legitKillAura);
        modules.add(elytraMace); // NEW
        modules.add(pvpBot);     // NEW
        modules.add(clickTP);
        modules.add(doorTP);
    }

    public void onTick() {
        for (Module module : modules) {
            module.onTick();
        }

        // Tick the fairplay subsystem each client tick so listeners prune and act
        if (fairPlayModule != null) fairPlayModule.onTick();
    }
}