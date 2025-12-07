package net.example.infiniteaura.fairplay;

/**
 * FairPlay Module: Cooperative handshake protocol for opt-in defense.
 * 
 * This module manages the lifecycle of fair-play signal emission and reception.
 * 
 * Public documentation:
 * - NOT a stealth system; all signals are marked as opt-in
 * - Does NOT provide permanent invulnerability
 * - MINIMAL cooldowns prevent abuse
 * - Visual/audio cues ensure transparency
 * - Rate-limiting prevents spam
 * 
 * Activation:
 * 1. Server setting: Settings.fairPlayEnabled = true
 * 2. Announcer publishes signals when OP actions arm
 * 3. Listener receives and routes signals to coordinator
 * 4. Coordinator activates minimal, non-stacking defenses
 * 5. Telemetry logs all activations for audit
 */
public class FairPlayModule {
    
    private final FairPlayConfig config;
    private boolean moduleTicking = false;
    
    public FairPlayModule() {
        this.config = FairPlayConfig.getInstance();
    }
    
    /**
     * Enable fair-play mode and start the module.
     */
    public void enable() {
        config.setEnabled(true);
        config.setAnnouncerEnabled(true);
        config.setListenerEnabled(true);
        moduleTicking = true;
    }
    
    /**
     * Disable fair-play mode.
     */
    public void disable() {
        config.setEnabled(false);
        moduleTicking = false;
    }
    
    /**
     * Per-tick update: prune expired defenses, manage state.
     * 
     * Call this from your main client tick loop.
     */
    public void onTick() {
        if (!moduleTicking) return;
        
        config.getListener().onTick();
    }
    
    /**
     * Announce an impending OP action.
     * 
     * Called by your reach/mace/etc. modules when arming.
     */
    public void announceAction(
        FairPlaySignal.ActionType actionType,
        long urgencyWindowMs,
        String senderHash
    ) {
        if (!config.fairPlayEnabled) return;
        
        config.getAnnouncer().announceStateTransition(
            FairPlayAnnouncer.ActionState.ARMED,
            actionType,
            urgencyWindowMs,
            senderHash
        );
    }
    
    /**
     * Receive an incoming fair-play signal (from network).
     * 
     * Call this from packet handlers or mixin hooks.
     */
    public void onSignalReceived(FairPlaySignal signal) {
        if (!config.fairPlayEnabled) return;
        
        config.getListener().onSignalReceived(signal);
    }
    
    /**
     * Get active defenses (for rendering UI).
     */
    public java.util.List<DefensePlan> getActivePlans() {
        return config.getListener().getActivePlans();
    }
    
    /**
     * Get telemetry log.
     */
    public java.util.List<String> getTelemetryLog() {
        return config.getListener().getTelemetryLog();
    }
    
    /**
     * Check if a defense type is currently active.
     */
    public boolean isDefenseActive(DefensePlan.DefenseType type) {
        return config.getListener().getActivePlans().stream()
            .anyMatch(p -> p.defenseType == type && p.isActive());
    }
    
    /**
     * Manual reset (for debugging/UI).
     */
    public void reset() {
        config.getAnnouncer().reset();
    }
}
