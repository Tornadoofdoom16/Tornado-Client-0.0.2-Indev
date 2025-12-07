package net.example.infiniteaura.fairplay;

/**
 * Defense plan derived from a FairPlaySignal.
 * 
 * Encodes the defensive response to an incoming OP action:
 * - defense type (phase, dual-totem, stagger)
 * - duration and cooldown constraints
 * - UI cue for transparency
 * 
 * Designed to be immutable and deterministic.
 */
public class DefensePlan {
    
    public enum DefenseType {
        PHASE_DODGE("phase_dodge", 150, 200),
        DUAL_TOTEM("dual_totem", 300, 400),
        STAGGER_GUARD("stagger_guard", 200, 300),
        THROTTLE_DEFENSE("throttle", 100, 500),
        NONE("none", 0, 0);
        
        public final String id;
        public final long durationMs;
        public final long cooldownMs;
        
        DefenseType(String id, long durationMs, long cooldownMs) {
            this.id = id;
            this.durationMs = durationMs;
            this.cooldownMs = cooldownMs;
        }
    }
    
    public final FairPlaySignal triggeringSignal;
    public final DefenseType defenseType;
    public final String visualCue;
    public final long activatedAtMs;
    public final boolean isSpamResponse;
    
    public DefensePlan(
        FairPlaySignal triggeringSignal,
        DefenseType defenseType,
        String visualCue,
        boolean isSpamResponse
    ) {
        this.triggeringSignal = triggeringSignal;
        this.defenseType = defenseType;
        this.visualCue = visualCue;
        this.activatedAtMs = System.currentTimeMillis();
        this.isSpamResponse = isSpamResponse;
    }
    
    /**
     * Check if this plan's cooldown window has expired.
     */
    public boolean isCooldownExpired() {
        long elapsedMs = System.currentTimeMillis() - activatedAtMs;
        return elapsedMs >= defenseType.cooldownMs;
    }
    
    /**
     * Check if this plan's active duration has elapsed.
     */
    public boolean isActive() {
        long elapsedMs = System.currentTimeMillis() - activatedAtMs;
        return elapsedMs < defenseType.durationMs;
    }
    
    @Override
    public String toString() {
        return String.format(
            "DefensePlan[type=%s, cue=%s, spam=%s, age=%dms]",
            defenseType.id,
            visualCue,
            isSpamResponse,
            System.currentTimeMillis() - activatedAtMs
        );
    }
}
