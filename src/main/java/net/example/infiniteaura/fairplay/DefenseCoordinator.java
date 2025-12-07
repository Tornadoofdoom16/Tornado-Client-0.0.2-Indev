package net.example.infiniteaura.fairplay;

import java.util.*;

/**
 * Coordinates active defenses and prevents overlapping/conflicting activations.
 * 
 * Responsibilities:
 * - Track currently active DefensePlans
 * - Merge conflicts by picking the least disruptive defense
 * - Apply minimal cooldowns to prevent permanent invulnerability
 * - Emit telemetry (logging) for transparency
 * 
 * Not thread-safe; intended for single-threaded client tick loop.
 */
public class DefenseCoordinator {
    
    private final Queue<DefensePlan> activePlans = new LinkedList<>();
    private final Map<DefensePlan.DefenseType, Long> cooldownExpiryMs = new HashMap<>();
    private final List<String> telemetryLog = new ArrayList<>();
    private final int maxLogSize = 100;
    
    /**
     * Propose activation of a defense.
     * 
     * Merges with current state and applies conflict resolution.
     * 
     * @param plan The proposed DefensePlan
     * @return true if plan was activated, false if rejected due to conflict/cooldown
     */
    public boolean proposeActivation(DefensePlan plan) {
        // Reject if defense type is on cooldown
        Long expiryMs = cooldownExpiryMs.get(plan.defenseType);
        if (expiryMs != null && System.currentTimeMillis() < expiryMs) {
            telemetry("REJECT: " + plan.defenseType.id + " still on cooldown");
            return false;
        }
        
        // Reject if same or more disruptive defense is active
        if (hasConflict(plan)) {
            telemetry("MERGE: Conflict detected, keeping current defense");
            return false;
        }
        
        // Activate
        activePlans.offer(plan);
        cooldownExpiryMs.put(
            plan.defenseType,
            System.currentTimeMillis() + plan.defenseType.cooldownMs
        );
        
        telemetry("ACTIVATE: " + plan);
        return true;
    }
    
    /**
     * Check for conflicts with currently active plans.
     * 
     * Conflict resolution prioritizes:
     * 1. Already-active defenses (do not disrupt)
     * 2. Least-disruptive incoming defense
     */
    private boolean hasConflict(DefensePlan plan) {
        for (DefensePlan active : activePlans) {
            if (active.isActive()) {
                // If same type, always conflict
                if (active.defenseType == plan.defenseType) {
                    return true;
                }
                // If incoming is less disruptive, allow it
                if (isLessDisruptive(plan.defenseType, active.defenseType)) {
                    return false;
                }
                // Otherwise, keep active
                return true;
            }
        }
        return false;
    }
    
    /**
     * Determines if one defense is less disruptive than another.
     * 
     * Hierarchy (least to most disruptive):
     * NONE < THROTTLE < PHASE_DODGE < DUAL_TOTEM < STAGGER_GUARD
     */
    private boolean isLessDisruptive(
        DefensePlan.DefenseType incoming,
        DefensePlan.DefenseType current
    ) {
        int incomingRank = disruptionRank(incoming);
        int currentRank = disruptionRank(current);
        return incomingRank < currentRank;
    }
    
    private int disruptionRank(DefensePlan.DefenseType type) {
        return switch (type) {
            case NONE -> 0;
            case THROTTLE_DEFENSE -> 1;
            case PHASE_DODGE -> 2;
            case DUAL_TOTEM -> 3;
            case STAGGER_GUARD -> 4;
        };
    }
    
    /**
     * Prune expired defenses from active queue.
     * 
     * Call this once per tick to clean up.
     */
    public void prunExpired() {
        Iterator<DefensePlan> iter = activePlans.iterator();
        while (iter.hasNext()) {
            DefensePlan plan = iter.next();
            if (!plan.isActive()) {
                iter.remove();
                telemetry("EXPIRE: " + plan.defenseType.id);
            }
        }
    }
    
    /**
     * Get currently active defenses (snapshot).
     */
    public List<DefensePlan> getActivePlans() {
        return new ArrayList<>(activePlans);
    }
    
    /**
     * Check if a specific defense type is currently active.
     */
    public boolean isActive(DefensePlan.DefenseType type) {
        return activePlans.stream()
            .anyMatch(p -> p.defenseType == type && p.isActive());
    }
    
    /**
     * Internal telemetry logging.
     */
    private void telemetry(String message) {
        String timestamped = String.format(
            "[%d] %s",
            System.currentTimeMillis(),
            message
        );
        telemetryLog.add(timestamped);
        
        // Prevent unbounded log growth
        if (telemetryLog.size() > maxLogSize) {
            telemetryLog.remove(0);
        }
    }
    
    /**
     * Retrieve telemetry log (recent events).
     */
    public List<String> getTelemetryLog() {
        return new ArrayList<>(telemetryLog);
    }
    
    /**
     * Clear telemetry log.
     */
    public void clearTelemetry() {
        telemetryLog.clear();
    }
}
