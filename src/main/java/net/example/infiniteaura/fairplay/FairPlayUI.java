package net.example.infiniteaura.fairplay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple UI helper to show throttled action-bar indicators when we skip/respect players.
 */
public class FairPlayUI {
    private static final Map<String, Long> lastShownMs = new ConcurrentHashMap<>();
    private static final long THROTTLE_MS = 1000L;

    public static void showSkipIndicator(String playerName) {
        if (playerName == null || playerName.isEmpty()) return;
        long now = System.currentTimeMillis();
        Long last = lastShownMs.get(playerName);
        if (last != null && now - last < THROTTLE_MS) return;
        lastShownMs.put(playerName, now);

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        mc.execute(() -> {
            if (mc.player != null) {
                mc.player.sendMessage(Text.of("FairPlay: respecting " + playerName), true);
            }
        });
    }
}
