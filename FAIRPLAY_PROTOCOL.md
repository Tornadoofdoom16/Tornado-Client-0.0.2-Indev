# FairPlay: Cooperative Handshake Defense Protocol

## Overview

**FairPlay** is an opt-in, cooperative protocol layered on top of PvP mechanics. When your client is about to perform an extreme action (reach, mace), it emits a **deterministic signal** that compatible clients can detect early enough to pre-activate defense. The goal is to restore parity when both sides opt in—shifting from reactive to predictive defense so fights are skill-based.

## Key Principles

- **Opt-in Only**: FairPlay mode is explicit; players choose it
- **No Permanent Invulnerability**: Defenses are brief (150–300ms) and purposeful
- **Rate-Limited**: Spam detection escalates to throttle mode
- **Transparent**: Visual/audio cues make activations visible
- **Anti-Spoofing**: Basic integrity checks reduce hostile signal abuse
- **Public**: Designed for community adoption and balance

## Architecture

### Components

#### `FairPlaySignal`
- **Purpose**: Encodes an impending OP action with timing hints
- **Fields**:
  - `actionType`: REACH_IMPULSE, MACE_SLAM, BYPASS_WINDOW, CRYSTAL_PLACE
  - `urgencyWindowMs`: Time before action fires (50–500ms)
  - `fairPlayMode`: Always true for opt-in signals
  - `nonce`: Unique identifier to prevent duplicates
  - `senderHash`: Lightweight integrity hint
- **Validation**: Checks format, recency, and opt-in flag

#### `DefensePlan`
- **Purpose**: Defensive response mapped from a signal
- **Types**:
  - `PHASE_DODGE`: Brief teleport/phase (150ms); returns to original position
  - `DUAL_TOTEM`: Ensure off-hand redundancy (300ms)
  - `STAGGER_GUARD`: Invulnerability + displacement (200ms)
  - `THROTTLE_DEFENSE`: Spam mitigation (100ms)
- **State**: Duration, cooldown, visual cue, activation timestamp

#### `DefenseMapper`
- **Purpose**: Maps action types to defenses (pure function)
- **Logic**:
  - REACH_IMPULSE → PHASE_DODGE
  - MACE_SLAM → STAGGER_GUARD
  - BYPASS_WINDOW → DUAL_TOTEM
  - CRYSTAL_PLACE → PHASE_DODGE
  - Spam detected → THROTTLE_DEFENSE

#### `DefenseCoordinator`
- **Purpose**: Manages active defenses and conflict resolution
- **Responsibilities**:
  - Track active DefensePlans
  - Apply cooldowns to prevent stacking
  - Pick least disruptive defense on conflict
  - Emit telemetry for transparency
- **Disruptiveness Hierarchy**:
  1. NONE (least)
  2. THROTTLE_DEFENSE
  3. PHASE_DODGE
  4. DUAL_TOTEM
  5. STAGGER_GUARD (most)

#### `RateLimiter`
- **Purpose**: Detect and penalize signal spam
- **Algorithm**:
  - Track signals per sender in 1-second windows
  - Flag as spam after 5 signals/sec
  - Escalate to THROTTLE_DEFENSE for 5 seconds
  - Auto-recover after cool-down

#### `FairPlayListener`
- **Purpose**: Receive, validate, and route signals
- **Flow**:
  1. Validate format and recency (100ms max age)
  2. Check rate-limiting on sender
  3. Map signal to defense via DefenseMapper
  4. Propose activation to DefenseCoordinator
- **Per-tick**: Prune expired defenses

#### `FairPlayAnnouncer`
- **Purpose**: Emit signals when OP actions arm
- **State Machine**:
  - IDLE → ARMED: Emit signal
  - ARMED → FIRED: No signal (action now executing)
  - FIRED → IDLE: Reset
- **Integration**: Called by modules detecting OP action state transitions

#### `FairPlayConfig`
- **Purpose**: Singleton configuration and component factory
- **Settings**:
  - `fairPlayEnabled`: Global toggle
  - `announcerEnabled`: Can emit signals
  - `listenerEnabled`: Can receive signals
  - `telemetryVisible`: Show UI overlay

#### `FairPlayModule`
- **Purpose**: High-level lifecycle manager
- **Public API**:
  - `enable()` / `disable()`
  - `onTick()`: Per-frame updates
  - `announceAction()`: Emit signal
  - `onSignalReceived()`: Receive signal
  - `getActivePlans()`: UI/telemetry
  - `getTelemetryLog()`: Audit trail

## Usage Example

### In Your Main Client Initialization

```java
import net.example.infiniteaura.fairplay.*;

public class TornadoClient implements ClientModInitializer {
    private FairPlayModule fairPlayModule;

    @Override
    public void onInitializeClient() {
        // ... other setup ...
        
        // Initialize FairPlay
        fairPlayModule = new FairPlayModule();
        
        // Wire up to client tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            fairPlayModule.onTick();
        });
    }
}
```

### In Your Reach/Mace Module

```java
public class InfiniteAura extends Module {
    private FairPlayModule fairPlayModule = FairPlayModule.getInstance();

    @Override
    public void onTick() {
        if (!settings.enabled) return;

        // Detect when action is about to fire
        if (isReachPending()) {
            // Announce to compatible clients
            fairPlayModule.announceAction(
                FairPlaySignal.ActionType.REACH_IMPULSE,
                100, // Action fires in ~100ms
                "tornado-client-hash"
            );
            
            // Perform the reach
            performReach();
        }
    }
}
```

### In a Packet Mixin (Receiving Signals)

```java
@Mixin(ClientPlayNetworkHandler.class)
public class NetworkMixin {
    
    private FairPlayModule fairPlayModule = FairPlayModule.getInstance();

    @Inject(method = "onGameJoin", at = @At("HEAD"))
    private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        // If you intercept custom packets containing signals:
        FairPlaySignal signal = parseSignal(packet);
        if (signal != null) {
            fairPlayModule.onSignalReceived(signal);
        }
    }
}
```

## Configuration (In Settings)

```java
public class TornadoClientSettings {
    // Fair-Play Mode
    public boolean fairPlayEnabled = false;
    public boolean fairPlayAnnouncer = false; // Can emit
    public boolean fairPlayListener = false;  // Can receive
    public boolean fairPlayTelemetry = false; // Show overlay
}
```

## Telemetry & Debugging

Active defenses and event logs are available via:

```java
FairPlayModule module = new FairPlayModule();

// Get active defenses
List<DefensePlan> active = module.getActivePlans();
for (DefensePlan plan : active) {
    System.out.println(plan);
}

// Get telemetry log
List<String> log = module.getTelemetryLog();
for (String event : log) {
    System.out.println(event);
}
```

Example telemetry output:
```
[FairPlay] ACTIVATE: DefensePlan[type=phase_dodge, cue=⚡ Phase dodge: reach_impulse, age=12ms]
[FairPlay] EXPIRE: phase_dodge
[FairPlay] ACTIVATE: DefensePlan[type=dual_totem, cue=🛡 Dual-totem: bypass_window, age=5ms]
[FairPlay] REJECT: dual_totem still on cooldown
[FairPlay] MERGE: Conflict detected, keeping current defense
```

## Signal Format (Conceptual)

For network transmission (pseudo-JSON):

```json
{
  "type": "fair_play_signal",
  "action_type": "reach_impulse",
  "urgency_window_ms": 100,
  "fair_play": true,
  "version": "1.0.0",
  "nonce": "a1b2c3d4",
  "emitted_at_ms": 1701990000000,
  "sender_hash": "tornado-client-v1"
}
```

Validation rules:
- `age < 100ms`
- `fair_play == true` (reject stealth signals)
- `action_type` in enumeration
- `urgency_window_ms` in [50, 500]
- `nonce` is non-empty

## Balance & Safeguards

### Prevents Permanent Invulnerability

- Each defense has a **maximum duration** (150–300ms)
- **Cooldowns** prevent stacking
- **Conflict resolution** picks least disruptive defense
- No "always-on shield" mode

### Prevents Spam Abuse

- **Rate limiter** tracks signals per sender in 1-second windows
- **Spam threshold**: 5+ signals/sec → spam flag
- **Penalty**: Escalate to THROTTLE_DEFENSE (100ms, 500ms cooldown)
- **Recovery**: Auto-clear after 5 seconds of clean behavior

### Prevents Spoofing

- **Opt-in flag**: Reject signals with `fair_play == false`
- **Nonce**: Unique per signal, prevents exact duplicates
- **Recency check**: Reject signals older than 100ms
- **Sender hash**: Lightweight integrity hint (not cryptographic)

### Transparent & Auditable

- All activations logged to telemetry
- Visual cues (particles, text) show when defenses trigger
- UI overlay available to show active plans and recent events
- No hidden invulnerability mechanics

## Future Enhancements

- **Custom packet**: Instead of chat/global, use a custom payload for signal transmission
- **Cryptographic signing**: Stronger integrity checks (if community adoption grows)
- **Per-player opt-in whitelist**: Choose which opponents you trust
- **Skill-based variants**: Wider urgency windows for harder actions (e.g., fast crystal place)
- **Cross-platform**: Protocol design allows non-Java clients to participate

## Fair-Play Manifesto

> FairPlay reframes OP mechanics from "who will exploit first" into "can we agree to fight fairly?" By opting in, both players consent to:
> 
> 1. **Predictable defense**: No permanent invulnerability, only tactical brief shields
> 2. **Transparent mechanics**: All activations logged and visible
> 3. **Parity restoration**: Both sides equipped to detect and respond
> 4. **Skill focus**: Timing, positioning, and resource management matter again
> 
> FairPlay is not about "beating the server" or "stealth cheating." It's about shifting the baseline from "everyone exploits" to "everyone plays fairly."

---

**Version**: 1.0.0  
**Last Updated**: 2025-12-07  
**Status**: Stable – Ready for Community Feedback
