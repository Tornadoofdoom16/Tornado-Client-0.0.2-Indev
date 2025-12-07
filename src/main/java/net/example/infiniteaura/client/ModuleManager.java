package net.example.infiniteaura.client;

import net.example.infiniteaura.modules.*;

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
    }

    public void onTick() {
        for (Module module : modules) {
            module.onTick();
        }
    }
}