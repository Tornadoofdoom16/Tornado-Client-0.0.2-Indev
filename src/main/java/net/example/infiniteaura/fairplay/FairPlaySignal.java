package net.example.infiniteaura.fairplay;

import java.util.UUID;

/**
 * Core signal contract for fair-play handshake protocol.
 * 
 * This immutable record encodes an impending OP action with metadata
 * sufficient for compatible clients to pre-activate defense before
 * the server resolves the hit.
 * 
 * Design constraints:
 * - No permanent invulnerability hints
 * - Must be opt-in only
 * - Validation must catch malformed/spoofed signals
 */
public class FairPlaySignal {
    
    /**
     * Action type enumeration.
     */
    public enum ActionType {
        REACH_IMPULSE("reach_impulse"),
        MACE_SLAM("mace_slam"),
        BYPASS_WINDOW("bypass_window"),
        CRYSTAL_PLACE("crystal_place"),
        UNKNOWN("unknown");
        
        public final String id;
        
        ActionType(String id) {
            this.id = id;
        }
        
        public static ActionType fromId(String id) {
            for (ActionType t : values()) {
                if (t.id.equals(id)) return t;
            }
            return UNKNOWN;
        }
    }
    
    public final ActionType actionType;
    public final long urgencyWindowMs;
    public final boolean fairPlayMode;
    public final String version;
    public final String nonce;
    public final long emittedAtMs;
    public final String senderHash; // lightweight integrity hint
    
    public FairPlaySignal(
        ActionType actionType,
        long urgencyWindowMs,
        boolean fairPlayMode,
        String version,
        String senderHash
    ) {
        this.actionType = actionType;
        this.urgencyWindowMs = urgencyWindowMs;
        this.fairPlayMode = fairPlayMode;
        this.version = version;
        this.nonce = UUID.randomUUID().toString().substring(0, 8);
        this.emittedAtMs = System.currentTimeMillis();
        this.senderHash = senderHash;
    }
    
    /**
     * Validates signal format, recency, and integrity.
     * 
     * @param maxAgeMs Maximum acceptable age in milliseconds
     * @return true if signal is valid and within temporal bounds
     */
    public boolean isValid(long maxAgeMs) {
        long ageMs = System.currentTimeMillis() - emittedAtMs;
        
        // Reject stale signals
        if (ageMs > maxAgeMs) {
            return false;
        }
        
        // Reject unknown action types
        if (actionType == ActionType.UNKNOWN) {
            return false;
        }
        
        // Reject non-opt-in signals (prevent stealth abuse)
        if (!fairPlayMode) {
            return false;
        }
        
        // Reject implausible urgency windows
        if (urgencyWindowMs < 50 || urgencyWindowMs > 500) {
            return false;
        }
        
        // Reject missing nonce
        if (nonce == null || nonce.isEmpty()) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public String toString() {
        return String.format(
            "FairPlaySignal[type=%s, window=%dms, fair=%s, age=%dms, nonce=%s]",
            actionType.id,
            urgencyWindowMs,
            fairPlayMode,
            System.currentTimeMillis() - emittedAtMs,
            nonce
        );
    }
}
