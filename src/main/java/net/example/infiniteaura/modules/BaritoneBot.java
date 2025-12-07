package net.example.infiniteaura.modules;

import net.example.infiniteaura.TornadoClientSettings;
import net.example.infiniteaura.client.Module;

public class BaritoneBot extends Module {
    private final TornadoClientSettings settings = TornadoClientSettings.INSTANCE;

    @Override
    public void onTick() {
        // No tick logic needed, it's event driven
    }

    // Called by the Mixin when a chat message is received
    public void onChatMessage(String message, String senderName) {
        if (!settings.enabled || !settings.baritoneBot || mc.player == null) return;

        // 1. Strict Friend Check
        // If the sender is NOT in our friends list, ignore them.
        if (!settings.friendsList.contains(senderName)) return;

        // 2. Check for Baritone Command
        // Baritone commands typically start with '#'
        if (message.startsWith("#")) {
            
            // 3. Inject Command
            // We send the message as if we typed it. 
            // Baritone (if installed) hooks into the outgoing chat packet, 
            // intercepts this, and executes the command locally.
            mc.player.networkHandler.sendChatMessage(message);
            
            mc.player.sendMessage(net.minecraft.text.Text.of("§b[BaritoneBot] Executing: " + message), true);
        }
    }
}