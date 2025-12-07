package net.example.infiniteaura.modules;

import net.example.infiniteaura.TornadoClientSettings;
import net.example.infiniteaura.client.Module;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class NoFall extends Module {
    private final TornadoClientSettings settings = TornadoClientSettings.INSTANCE;

    @Override
    public void onTick() {
        if (!settings.enabled || !settings.noFall || mc.player == null) return;

        // If we are falling fast, tell the server we are on the ground.
        // This tricks the server into resetting our fall distance.
        if (mc.player.fallDistance > 2.5f) {
            if (mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
                // Reset local fall distance to prevent visual shake
                mc.player.fallDistance = 0;
            }
        }
    }
}