package net.example.infiniteaura.modules;

import net.example.infiniteaura.InfiniteAuraSettings;
import net.example.infiniteaura.client.Module;

public class Speed extends Module {
    private final InfiniteAuraSettings settings = InfiniteAuraSettings.INSTANCE;

    @Override
    public void onTick() {
        if (!settings.enabled || !settings.speed || mc.player == null) return;

        // Only apply if moving
        if (mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0) {
            if (mc.player.isOnGround()) {
                mc.player.jump(); // Auto-jump for "Bunny Hop" style speed
            }
            
            // Multiply air/ground velocity
            // 1.2 is a safe-ish fast speed. Higher values (1.5+) might rubberband.
            double speedMultiplier = 1.2; 
            
            mc.player.setVelocity(
                mc.player.getVelocity().x * speedMultiplier,
                mc.player.getVelocity().y,
                mc.player.getVelocity().z * speedMultiplier
            );
        }
    }
}