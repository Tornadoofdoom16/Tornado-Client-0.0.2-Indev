package net.example.infiniteaura.modules;

import net.example.infiniteaura.TornadoClientSettings;
import net.example.infiniteaura.client.Module;

public class AirJump extends Module {
    private final TornadoClientSettings settings = TornadoClientSettings.INSTANCE;

    @Override
    public void onTick() {
        if (!settings.enabled || !settings.airJump || mc.player == null) return;

        // If space is held and we are not on ground (and not already flying)
        if (mc.options.jumpKey.isPressed() && !mc.player.isOnGround() && !mc.player.getAbilities().flying) {
            // Standard AirJump often relies on a cooldown or logic to prevent "flying"
            // But for a simple hack, we just reset Y velocity
            
            // Only jump if falling or steady to allow climbing
            mc.player.jump();
        }
    }
}