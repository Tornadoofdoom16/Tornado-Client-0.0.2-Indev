package net.example.infiniteaura.fairplay;

/**
 * Announcer: Emits FairPlaySignals when OP actions are about to fire.
 * 
 * Responsibilities:
 * - Detect state transitions (idle → armed for OP actions)
 * - Emit signals only on significant triggers (not every frame)
 * - Mark signals as fair-play mode (opt-in)
 * - Provide hooks for modules to trigger announcements
 * 
 * Design: Stateful tracker with callback interface.
 */
public class FairPlayAnnouncer {
    
    /**
     * Represents the readiness state of an OP action.
     */
    public enum ActionState {
        IDLE,           // No action pending
        ARMED,          // Action ready to fire
        FIRED,          // Action has executed
    }
    
    public interface SignalEmitter {
        /**
         * Called when a signal should be broadcast.
         * 
         * In real implementation, this would send via chat, custom packets, etc.
         */
        void emit(FairPlaySignal signal);
    }
    
    private final SignalEmitter emitter;
    private final String clientVersion;
    private final boolean enabled;
    private ActionState currentState = ActionState.IDLE;
    
    public FairPlayAnnouncer(SignalEmitter emitter, String clientVersion, boolean enabled) {
        this.emitter = emitter;
        this.clientVersion = clientVersion;
        this.enabled = enabled;
    }
    
    /**
     * Transition state and emit signal if needed.
     * 
     * Call this when detecting OP action state changes.
     * 
     * @param newState The new action state
     * @param actionType The type of action (reach, mace, etc.)
     * @param urgencyWindowMs Time window before action fires (ms)
     * @param senderHash Lightweight integrity hint (e.g., client hash)
     */
    public void announceStateTransition(
        ActionState newState,
        FairPlaySignal.ActionType actionType,
        long urgencyWindowMs,
        String senderHash
    ) {
        if (!enabled || newState == currentState) {
            return;
        }
        
        // Emit signal only on idle → armed transition
        if (currentState == ActionState.IDLE && newState == ActionState.ARMED) {
            FairPlaySignal signal = new FairPlaySignal(
                actionType,
                urgencyWindowMs,
                true, // fair-play mode: always true for announcer
                clientVersion,
                senderHash
            );
            
            emitter.emit(signal);
        }
        
        currentState = newState;
    }
    
    /**
     * Reset state (e.g., when action completes or is cancelled).
     */
    public void reset() {
        currentState = ActionState.IDLE;
    }
    
    /**
     * Get current state.
     */
    public ActionState getState() {
        return currentState;
    }
}
