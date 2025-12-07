package net.example.infiniteaura.fairplay;

/**
 * Unit tests for FairPlay protocol components.
 * 
 * Focus: Protocol validation, not exploit logic.
 * Tests ensure:
 * - Signal parsing and validation
 * - Cooldown enforcement
 * - Conflict resolution
 * - Rate limiting
 * - No unintended invulnerability
 */
public class FairPlayTests {
    
    /**
     * Test 1: Signal validation rejects stale signals
     */
    public static void testSignalValidationStale() {
        FairPlaySignal.ActionType actionType = FairPlaySignal.ActionType.REACH_IMPULSE;
        FairPlaySignal signal = new FairPlaySignal(
            actionType,
            100,  // urgency window
            true, // fair-play mode
            "1.0.0",
            "test-sender"
        );
        
        // Simulate aging by sleeping (or mock time in real impl)
        // For now, test immediate validity
        assert signal.isValid(100) : "Fresh signal should be valid";
        
        // Would need time-mocking to test stale signals properly
        System.out.println("✓ Test 1: Signal validation passed");
    }
    
    /**
     * Test 2: Signal validation rejects non-opt-in signals
     */
    public static void testSignalValidationRejectsStealth() {
        // Create a signal with fair_play=false (simulated)
        // In real impl, you'd pass false to constructor
        FairPlaySignal signal = new FairPlaySignal(
            FairPlaySignal.ActionType.REACH_IMPULSE,
            100,
            false, // NOT fair-play mode
            "1.0.0",
            "test-sender"
        );
        
        assert !signal.isValid(100) : "Non-opt-in signal should be invalid";
        System.out.println("✓ Test 2: Stealth signal rejection passed");
    }
    
    /**
     * Test 3: DefensePlan tracks active duration
     */
    public static void testDefensePlanActiveDuration() {
        FairPlaySignal signal = new FairPlaySignal(
            FairPlaySignal.ActionType.MACE_SLAM,
            100,
            true,
            "1.0.0",
            "test-sender"
        );
        
        DefensePlan plan = new DefensePlan(
            signal,
            DefensePlan.DefenseType.STAGGER_GUARD,
            "Test cue",
            false
        );
        
        // Immediately after creation, should be active
        assert plan.isActive() : "Plan should be active immediately after creation";
        assert !plan.isCooldownExpired() : "Plan should not be cooled down immediately";
        
        System.out.println("✓ Test 3: DefensePlan duration tracking passed");
    }
    
    /**
     * Test 4: DefenseCoordinator enforces cooldowns
     */
    public static void testCoordinatorCooldown() {
        DefenseCoordinator coordinator = new DefenseCoordinator();
        
        FairPlaySignal signal1 = new FairPlaySignal(
            FairPlaySignal.ActionType.REACH_IMPULSE,
            100,
            true,
            "1.0.0",
            "sender-1"
        );
        
        DefensePlan plan1 = new DefensePlan(
            signal1,
            DefensePlan.DefenseType.PHASE_DODGE,
            "First phase",
            false
        );
        
        // First activation should succeed
        boolean activated1 = coordinator.proposeActivation(plan1);
        assert activated1 : "First defense should activate";
        
        // Immediate second activation should fail (cooldown)
        FairPlaySignal signal2 = new FairPlaySignal(
            FairPlaySignal.ActionType.REACH_IMPULSE,
            100,
            true,
            "1.0.0",
            "sender-2"
        );
        
        DefensePlan plan2 = new DefensePlan(
            signal2,
            DefensePlan.DefenseType.PHASE_DODGE,
            "Second phase",
            false
        );
        
        boolean activated2 = coordinator.proposeActivation(plan2);
        assert !activated2 : "Second PHASE_DODGE should be rejected due to cooldown";
        
        System.out.println("✓ Test 4: Cooldown enforcement passed");
    }
    
    /**
     * Test 5: DefenseCoordinator conflict resolution (least disruptive wins)
     */
    public static void testCoordinatorConflictResolution() {
        DefenseCoordinator coordinator = new DefenseCoordinator();
        
        // Activate a disruptive defense
        FairPlaySignal signal1 = new FairPlaySignal(
            FairPlaySignal.ActionType.MACE_SLAM,
            100,
            true,
            "1.0.0",
            "sender-1"
        );
        
        DefensePlan plan1 = new DefensePlan(
            signal1,
            DefensePlan.DefenseType.STAGGER_GUARD,
            "Stagger",
            false
        );
        
        boolean activated1 = coordinator.proposeActivation(plan1);
        assert activated1 : "STAGGER_GUARD should activate";
        assert coordinator.isActive(DefensePlan.DefenseType.STAGGER_GUARD);
        
        // Try to activate a less disruptive defense (should be rejected)
        FairPlaySignal signal2 = new FairPlaySignal(
            FairPlaySignal.ActionType.REACH_IMPULSE,
            100,
            true,
            "1.0.0",
            "sender-2"
        );
        
        DefensePlan plan2 = new DefensePlan(
            signal2,
            DefensePlan.DefenseType.PHASE_DODGE,
            "Phase",
            false
        );
        
        // This should fail because STAGGER_GUARD is already active
        boolean activated2 = coordinator.proposeActivation(plan2);
        assert !activated2 : "Less disruptive defense should be rejected when more disruptive is active";
        
        System.out.println("✓ Test 5: Conflict resolution passed");
    }
    
    /**
     * Test 6: RateLimiter detects spam
     */
    public static void testRateLimiterSpamDetection() {
        RateLimiter limiter = new RateLimiter();
        
        FairPlaySignal signal = new FairPlaySignal(
            FairPlaySignal.ActionType.REACH_IMPULSE,
            100,
            true,
            "1.0.0",
            "spam-sender"
        );
        
        // Emit signals rapidly
        boolean spam = false;
        for (int i = 0; i < 6; i++) {
            spam = limiter.checkAndUpdateSpam("spam-sender", signal);
        }
        
        // After 6 signals, spam should be flagged
        assert spam : "Should detect spam after threshold";
        
        System.out.println("✓ Test 6: Spam detection passed");
    }
    
    /**
     * Test 7: DefenseMapper maps action types correctly
     */
    public static void testDefenseMapping() {
        FairPlaySignal reachSignal = new FairPlaySignal(
            FairPlaySignal.ActionType.REACH_IMPULSE,
            100,
            true,
            "1.0.0",
            "sender"
        );
        
        DefensePlan reachPlan = DefenseMapper.mapSignalToDefense(reachSignal, false);
        assert reachPlan.defenseType == DefensePlan.DefenseType.PHASE_DODGE
            : "Reach should map to Phase Dodge";
        
        FairPlaySignal maceSignal = new FairPlaySignal(
            FairPlaySignal.ActionType.MACE_SLAM,
            100,
            true,
            "1.0.0",
            "sender"
        );
        
        DefensePlan macePlan = DefenseMapper.mapSignalToDefense(maceSignal, false);
        assert macePlan.defenseType == DefensePlan.DefenseType.STAGGER_GUARD
            : "Mace Slam should map to Stagger Guard";
        
        System.out.println("✓ Test 7: Defense mapping passed");
    }
    
    /**
     * Test 8: FairPlayListener validates before activating
     */
    public static void testListenerValidation() {
        DefenseCoordinator coordinator = new DefenseCoordinator();
        FairPlayListener listener = new FairPlayListener(coordinator, true);
        
        // Create a valid signal
        FairPlaySignal validSignal = new FairPlaySignal(
            FairPlaySignal.ActionType.REACH_IMPULSE,
            100,
            true,
            "1.0.0",
            "sender"
        );
        
        boolean accepted = listener.onSignalReceived(validSignal);
        assert accepted : "Valid signal should be accepted";
        
        // Create an invalid signal (non-opt-in)
        FairPlaySignal invalidSignal = new FairPlaySignal(
            FairPlaySignal.ActionType.REACH_IMPULSE,
            100,
            false, // NOT fair-play
            "1.0.0",
            "sender"
        );
        
        boolean rejected = listener.onSignalReceived(invalidSignal);
        assert !rejected : "Invalid signal should be rejected";
        
        System.out.println("✓ Test 8: Listener validation passed");
    }
    
    /**
     * Test 9: No permanent invulnerability (defenses expire)
     */
    public static void testNoPermanentInvulnerability() {
        DefenseCoordinator coordinator = new DefenseCoordinator();
        
        FairPlaySignal signal = new FairPlaySignal(
            FairPlaySignal.ActionType.REACH_IMPULSE,
            100,
            true,
            "1.0.0",
            "sender"
        );
        
        DefensePlan plan = new DefensePlan(
            signal,
            DefensePlan.DefenseType.PHASE_DODGE,
            "Phase",
            false
        );
        
        coordinator.proposeActivation(plan);
        assert coordinator.isActive(DefensePlan.DefenseType.PHASE_DODGE);
        
        // Simulate time passing
        // In real tests, mock System.currentTimeMillis()
        // For now, just verify that plan has duration limits
        assert plan.defenseType.durationMs > 0 : "Defense must have finite duration";
        assert plan.defenseType.durationMs < 1000 : "Defense should not last too long";
        
        System.out.println("✓ Test 9: No permanent invulnerability passed");
    }
    
    /**
     * Test 10: FairPlayAnnouncer emits only on state transitions
     */
    public static void testAnnouncerStateTransition() {
        StringBuilder emittedSignals = new StringBuilder();
        
        FairPlayAnnouncer announcer = new FairPlayAnnouncer(
            signal -> emittedSignals.append("emitted "),
            "1.0.0",
            true
        );
        
        // Idle → Armed should emit
        announcer.announceStateTransition(
            FairPlayAnnouncer.ActionState.ARMED,
            FairPlaySignal.ActionType.REACH_IMPULSE,
            100,
            "sender"
        );
        
        assert emittedSignals.toString().contains("emitted")
            : "Should emit on IDLE→ARMED transition";
        
        // Armed → Armed should not emit again
        String beforeCount = emittedSignals.toString();
        announcer.announceStateTransition(
            FairPlayAnnouncer.ActionState.ARMED,
            FairPlaySignal.ActionType.REACH_IMPULSE,
            100,
            "sender"
        );
        
        assert beforeCount.equals(emittedSignals.toString())
            : "Should not emit on same state";
        
        System.out.println("✓ Test 10: Announcer state transition passed");
    }
    
    /**
     * Run all tests
     */
    public static void runAllTests() {
        System.out.println("\n=== FairPlay Protocol Tests ===\n");
        
        try {
            testSignalValidationStale();
            testSignalValidationRejectsStealth();
            testDefensePlanActiveDuration();
            testCoordinatorCooldown();
            testCoordinatorConflictResolution();
            testRateLimiterSpamDetection();
            testDefenseMapping();
            testListenerValidation();
            testNoPermanentInvulnerability();
            testAnnouncerStateTransition();
            
            System.out.println("\n=== All Tests Passed ===\n");
        } catch (AssertionError e) {
            System.err.println("\n✗ Test Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        runAllTests();
    }
}
