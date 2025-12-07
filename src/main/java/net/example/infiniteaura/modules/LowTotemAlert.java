package net.example.infiniteaura.modules;

import net.example.infiniteaura.TornadoClientSettings;
import net.example.infiniteaura.client.Module;
import net.minecraft.item.Items;

public class LowTotemAlert extends Module {
    private final TornadoClientSettings settings = TornadoClientSettings.INSTANCE;
    private boolean hasAlerted = false;

    @Override
    public void onTick() {
        if (!settings.enabled || !settings.lowTotemAlert || mc.player == null) return;

        int totemCount = 0;
        // Count Inventory
        for (int i = 0; i < 46; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                totemCount += mc.player.getInventory().getStack(i).getCount();
            }
        }
        // Count Offhand
        if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
            totemCount += mc.player.getOffHandStack().getCount();
        }

        // Logic
        if (totemCount <= 2 && !hasAlerted) {
            if (!settings.alertBotName.isEmpty()) {
                // Send whisper to the bot
                mc.player.networkHandler.sendChatCommand("msg " + settings.alertBotName + " tp " + mc.player.getName().getString());
                mc.player.sendMessage(net.minecraft.text.Text.of("§cLow Totems! Alerted bot."), true);
                hasAlerted = true;
            }
        } else if (totemCount > 2) {
            hasAlerted = false; // Reset when we get more totems
        }
    }
}