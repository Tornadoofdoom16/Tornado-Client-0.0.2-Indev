package net.example.infiniteaura.fairplay;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fair-Play mode configuration and singleton manager.
 * 
 * Controls opt-in status and provides access to announcer/listener.
 *
 * NOTE: This class now includes a non-blocking local loopback emitter used
 * for testing. The emitter submits signal delivery to a single-threaded
 * executor so announcer calls do not block the attack path.
 */
public class FairPlayConfig {
    
    private static FairPlayConfig instance = null;
    
    // Settings
    public boolean fairPlayEnabled = false;
    public boolean announcerEnabled = false;
    public boolean listenerEnabled = false;
    public boolean telemetryVisible = false;
    
    // Components
    private FairPlayAnnouncer announcer;
    private FairPlayListener listener;
    
    // Executor for non-blocking loopback emissions (local testing)
    private ExecutorService emitterExecutor;
    // Optional network emission (chat-based) for multi-client testing
    private boolean networkEmitEnabled = false;
    
    private FairPlayConfig() {}
    
    public static synchronized FairPlayConfig getInstance() {
        if (instance == null) {
            instance = new FairPlayConfig();
            instance.initialize();
        }
        return instance;
    }
    
    /**
     * Initialize components with default emitter. The default emitter
     * performs a non-blocking loopback: it logs the emission then
     * submits delivery to the local listener for immediate testing.
     */
    private void initialize() {
        DefenseCoordinator coordinator = new DefenseCoordinator();

        this.listener = new FairPlayListener(coordinator, listenerEnabled);

        // Single-threaded executor keeps emissions ordered and non-blocking
        this.emitterExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "fairplay-emitter");
            t.setDaemon(true);
            return t;
        });

        this.announcer = new FairPlayAnnouncer(
            signal -> {
                // Non-blocking: submit emission + loopback delivery
                try {
                    emitterExecutor.submit(() -> {
                        // Log announce for visibility
                        long now = System.currentTimeMillis();
                        System.out.println(String.format("[FairPlay][emit@%d] Announced: %s", now, signal));

                        // Loopback: deliver to local listener for testing
                        try {
                            if (listener != null) {
                                boolean accepted = listener.onSignalReceived(signal);
                                long handledAt = System.currentTimeMillis();
                                // Instrumentation: record emit and reception
                                try {
                                    FairPlayInstrumentation.getInstance().recordEmit(signal.nonce, signal.actionType.id, now, signal.senderHash);
                                    FairPlayInstrumentation.getInstance().recordReception(signal.nonce, handledAt, accepted);
                                } catch (Exception ignored) {}

                                if (accepted) {
                                    System.out.println(String.format("[FairPlay][handled@%d] Local defense ACTIVATED for: %s (emit->handle %dms)", handledAt, signal, handledAt - now));
                                } else {
                                    System.out.println(String.format("[FairPlay][handled@%d] Local listener IGNORED signal: %s (emit->handle %dms)", handledAt, signal, handledAt - now));
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("[FairPlay] Error delivering loopback signal: " + e.getMessage());
                        }

                        // If network emission is enabled, schedule a chat message to be sent on the client thread
                        if (networkEmitEnabled) {
                            try {
                                net.minecraft.client.MinecraftClient.getInstance().execute(() -> {
                                    try {
                                        if (net.minecraft.client.MinecraftClient.getInstance().getNetworkHandler() != null) {
                                            String payload = String.format("{\"fp\":1,\"t\":\"%s\",\"w\":%d,\"n\":\"%s\",\"s\":\"%s\"}", signal.actionType.id, signal.urgencyWindowMs, signal.nonce, signal.senderHash == null ? "" : signal.senderHash);
                                            net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket chat = new net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket("[FairPlay]" + payload);
                                            net.minecraft.client.MinecraftClient.getInstance().getNetworkHandler().sendPacket(chat);
                                        }
                                    } catch (Exception e) {
                                        System.out.println("[FairPlay] Failed to send network emit: " + e.getMessage());
                                    }
                                });
                            } catch (Exception e) {
                                System.out.println("[FairPlay] Failed scheduling network emit: " + e.getMessage());
                            }
                        }
                    });
                } catch (Exception e) {
                    // Ensure announcer never blocks or throws
                    System.out.println("[FairPlay] Failed to submit loopback emission: " + e.getMessage());
                }
            },
            "1.0.0", // version
            announcerEnabled
        );
    }
    
    public FairPlayAnnouncer getAnnouncer() {
        return announcer;
    }
    
    public FairPlayListener getListener() {
        return listener;
    }

    /**
     * Shutdown internal resources. Not required for normal mod lifecycle,
     * but useful for tests or clean shutdown hooks.
     */
    public void shutdown() {
        try {
            if (emitterExecutor != null) emitterExecutor.shutdownNow();
        } catch (Exception ignored) {}
    }

    public void setNetworkEmitEnabled(boolean enabled) {
        this.networkEmitEnabled = enabled;
    }
    
    /**
     * Toggle fair-play mode globally.
     */
    public void setEnabled(boolean enabled) {
        this.fairPlayEnabled = enabled;
    }
    
    /**
     * Enable/disable announcer.
     */
    public void setAnnouncerEnabled(boolean enabled) {
        this.announcerEnabled = enabled;
    }
    
    /**
     * Enable/disable listener.
     */
    public void setListenerEnabled(boolean enabled) {
        this.listenerEnabled = enabled;
    }
    
    /**
     * Show/hide telemetry overlay.
     */
    public void setTelemetryVisible(boolean visible) {
        this.telemetryVisible = visible;
    }
}
