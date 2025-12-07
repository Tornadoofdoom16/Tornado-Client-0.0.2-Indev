package net.example.infiniteaura.fairplay;

import java.util.*;

/**
 * Detects and rate-limits rapid signal emissions from a single sender.
 * 
 * Prevents abuse by tracking signal frequency:
 * - Multiple signals within short windows → spam flag
 * - Spam flag → escalate to throttle defense
 * - Allows cool-down to restore trust
 * 
 * Pure function; thread-safe via synchronized state.
 */
public class RateLimiter {
    
    private static final long SPAM_WINDOW_MS = 1000; // 1 second window
    private static final int SPAM_THRESHOLD = 5; // Triggers spam after 5 signals/sec
    private static final long SPAM_RECOVERY_MS = 5000; // 5 second recovery
    
    private final Map<String, SignalHistory> senderHistories = new HashMap<>();
    
    /**
     * Check if a sender is spamming signals.
     * 
     * Updates internal state; subsequent calls will return false after recovery period.
     * 
     * @param senderHash Unique identifier for the sender
     * @param signal The incoming signal
     * @return true if sender is currently spamming
     */
    public synchronized boolean checkAndUpdateSpam(String senderHash, FairPlaySignal signal) {
        SignalHistory history = senderHistories.computeIfAbsent(
            senderHash,
            k -> new SignalHistory()
        );
        
        history.recordSignal(signal);
        return history.isSpamming();
    }
    
    /**
     * Force-clear spam status for a sender (e.g., after timeout).
     */
    public synchronized void clearSpam(String senderHash) {
        SignalHistory history = senderHistories.get(senderHash);
        if (history != null) {
            history.clearSpamFlag();
        }
    }
    
    /**
     * Inner class tracking per-sender signal history.
     */
    private static class SignalHistory {
        private final Deque<Long> recentSignalTimes = new LinkedList<>();
        private boolean spamFlagActive = false;
        private long spamFlagSetAtMs = 0;
        
        void recordSignal(FairPlaySignal signal) {
            long nowMs = System.currentTimeMillis();
            recentSignalTimes.addLast(nowMs);
            
            // Evict signals outside the spam window
            while (!recentSignalTimes.isEmpty() &&
                   (nowMs - recentSignalTimes.getFirst()) > SPAM_WINDOW_MS) {
                recentSignalTimes.removeFirst();
            }
            
            // Check if we exceed threshold
            if (recentSignalTimes.size() >= SPAM_THRESHOLD) {
                setSpamFlag(nowMs);
            }
        }
        
        boolean isSpamming() {
            if (!spamFlagActive) {
                return false;
            }
            
            // Check if recovery period has elapsed
            long elapsedMs = System.currentTimeMillis() - spamFlagSetAtMs;
            if (elapsedMs > SPAM_RECOVERY_MS) {
                spamFlagActive = false;
                return false;
            }
            
            return true;
        }
        
        void setSpamFlag(long atMs) {
            spamFlagActive = true;
            spamFlagSetAtMs = atMs;
        }
        
        void clearSpamFlag() {
            spamFlagActive = false;
            recentSignalTimes.clear();
        }
    }
}
