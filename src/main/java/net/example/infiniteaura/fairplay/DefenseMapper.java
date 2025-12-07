package net.example.infiniteaura.fairplay;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps incoming FairPlaySignals to appropriate DefensePlans.
 * 
 * This pure function translates action types into defensive responses.
 * It respects rate-limiting (spam detection) and avoids permanent invulnerability.
 * 
 * Thread-safe and deterministic.
 */
public class DefenseMapper {
    
    private static final Map<FairPlaySignal.ActionType, DefensePlan.DefenseType> SIGNAL_DEFENSE_MAP = new HashMap<>();
    
    static {
        SIGNAL_DEFENSE_MAP.put(
            FairPlaySignal.ActionType.REACH_IMPULSE,
            DefensePlan.DefenseType.PHASE_DODGE
        );
        SIGNAL_DEFENSE_MAP.put(
            FairPlaySignal.ActionType.MACE_SLAM,
            DefensePlan.DefenseType.STAGGER_GUARD
        );
        SIGNAL_DEFENSE_MAP.put(
            FairPlaySignal.ActionType.BYPASS_WINDOW,
            DefensePlan.DefenseType.DUAL_TOTEM
        );
        SIGNAL_DEFENSE_MAP.put(
            FairPlaySignal.ActionType.CRYSTAL_PLACE,
            DefensePlan.DefenseType.PHASE_DODGE
        );
    }
    
    /**
     * Derive a DefensePlan from an incoming signal.
     * 
     * @param signal The validated FairPlaySignal
     * @param isSpammingDetected Whether rate-limiting has flagged this sender
     * @return A DefensePlan ready for activation
     */
    public static DefensePlan mapSignalToDefense(
        FairPlaySignal signal,
        boolean isSpammingDetected
    ) {
        // If spam is detected, escalate to throttle mode to prevent abuse
        if (isSpammingDetected) {
            return new DefensePlan(
                signal,
                DefensePlan.DefenseType.THROTTLE_DEFENSE,
                "⚠ Spam throttled",
                true
            );
        }
        
        // Map normal signals to their defenses
        DefensePlan.DefenseType defenseType = SIGNAL_DEFENSE_MAP.getOrDefault(
            signal.actionType,
            DefensePlan.DefenseType.NONE
        );
        
        String visualCue = describeDefense(defenseType, signal.actionType);
        
        return new DefensePlan(
            signal,
            defenseType,
            visualCue,
            false
        );
    }
    
    /**
     * Human-readable description of the defense for UI/telemetry.
     */
    private static String describeDefense(
        DefensePlan.DefenseType defenseType,
        FairPlaySignal.ActionType actionType
    ) {
        return switch (defenseType) {
            case PHASE_DODGE -> "⚡ Phase dodge: " + actionType.id;
            case DUAL_TOTEM -> "🛡 Dual-totem: " + actionType.id;
            case STAGGER_GUARD -> "💢 Stagger guard: " + actionType.id;
            case THROTTLE_DEFENSE -> "🚫 Defense throttled (spam detected)";
            case NONE -> "∅ No defense mapped";
        };
    }
}
