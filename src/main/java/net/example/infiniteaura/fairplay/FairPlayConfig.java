package net.example.infiniteaura.fairplay;

/**
 * Fair-Play mode configuration and singleton manager.
 * 
 * Controls opt-in status and provides access to announcer/listener.
 */
public class FairPlayConfig {
    
    private static FairPlayConfig instance = null;
    
    // Settings
    public boolean fairPlayEnabled = false;
    public boolean announcerEnabled = false;
    public boolean listenerEnabled = false;
    public boolean telemetryVisible = false;
    
    // Components
    private FairPlayAnnouncer announcer;
    private FairPlayListener listener;
    
    private FairPlayConfig() {}
    
    public static synchronized FairPlayConfig getInstance() {
        if (instance == null) {
            instance = new FairPlayConfig();
            instance.initialize();
        }
        return instance;
    }
    
    /**
     * Initialize components with default emitter.
     */
    private void initialize() {
        DefenseCoordinator coordinator = new DefenseCoordinator();
        
        this.listener = new FairPlayListener(coordinator, listenerEnabled);
        this.announcer = new FairPlayAnnouncer(
            signal -> {
                // Default emitter: log to system out
                System.out.println("[FairPlay] Announced: " + signal);
            },
            "1.0.0", // version
            announcerEnabled
        );
    }
    
    public FairPlayAnnouncer getAnnouncer() {
        return announcer;
    }
    
    public FairPlayListener getListener() {
        return listener;
    }
    
    /**
     * Toggle fair-play mode globally.
     */
    public void setEnabled(boolean enabled) {
        this.fairPlayEnabled = enabled;
    }
    
    /**
     * Enable/disable announcer.
     */
    public void setAnnouncerEnabled(boolean enabled) {
        this.announcerEnabled = enabled;
    }
    
    /**
     * Enable/disable listener.
     */
    public void setListenerEnabled(boolean enabled) {
        this.listenerEnabled = enabled;
    }
    
    /**
     * Show/hide telemetry overlay.
     */
    public void setTelemetryVisible(boolean visible) {
        this.telemetryVisible = visible;
    }
}
