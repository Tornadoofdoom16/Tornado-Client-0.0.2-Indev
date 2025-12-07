package net.example.infiniteaura.fairplay;

/**
 * Listener that receives and processes incoming FairPlaySignals.
 * 
 * Validates format, checks rate-limiting, and routes to DefenseCoordinator.
 * 
 * Designed to be called from packet/network event handlers.
 */
public class FairPlayListener {
    
    private static final long SIGNAL_MAX_AGE_MS = 100; // Signals older than 100ms are stale
    
    private final DefenseCoordinator coordinator;
    private final RateLimiter rateLimiter;
    private final boolean enabled;
    
    public FairPlayListener(DefenseCoordinator coordinator, boolean enabled) {
        this.coordinator = coordinator;
        this.rateLimiter = new RateLimiter();
        this.enabled = enabled;
    }
    
    /**
     * Process an incoming signal.
     * 
     * @param signal The FairPlaySignal to validate and handle
     * @return true if signal was accepted and activated a defense
     */
    public boolean onSignalReceived(FairPlaySignal signal) {
        if (!enabled) {
            return false;
        }
        
        // 1. Validate format and recency
        if (!signal.isValid(SIGNAL_MAX_AGE_MS)) {
            return false;
        }
        
        // 2. Check rate-limiting on sender
        boolean isSpamming = rateLimiter.checkAndUpdateSpam(
            signal.senderHash,
            signal
        );
        
        // 3. Map signal to defense
        DefensePlan plan = DefenseMapper.mapSignalToDefense(signal, isSpamming);
        
        // 4. Coordinate activation
        return coordinator.proposeActivation(plan);
    }
    
    /**
     * Prune expired defenses.
     * 
     * Call once per client tick.
     */
    public void onTick() {
        coordinator.prunExpired();
    }
    
    /**
     * Get active defenses (for UI/telemetry).
     */
    public java.util.List<DefensePlan> getActivePlans() {
        return coordinator.getActivePlans();
    }
    
    /**
     * Get telemetry log.
     */
    public java.util.List<String> getTelemetryLog() {
        return coordinator.getTelemetryLog();
    }
}
