package net.example.infiniteaura;

import net.example.infiniteaura.client.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class TornadoClient implements ClientModInitializer {

    // Keybinds
    private static KeyBinding toggleKey, uiKey, maceKey, swordKey, singleStrikeKey;
    
    // References
    private final InfiniteAuraSettings settings = InfiniteAuraSettings.INSTANCE;
    private final ModuleManager moduleManager = ModuleManager.INSTANCE;

    @Override
    public void onInitializeClient() {
        // 1. Register Keybinds
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tornadoclient.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.tornadoclient"));
        uiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tornadoclient.ui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_U, "category.tornadoclient"));
        maceKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tornadoclient.mace", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_M, "category.tornadoclient"));
        swordKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tornadoclient.sword", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.tornadoclient"));
        singleStrikeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tornadoclient.single", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.tornadoclient"));

        // 2. Main Loop
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            // Handle Input
            while (uiKey.wasPressed()) client.setScreen(new InfiniteAuraScreen(client.currentScreen));
            
            while (toggleKey.wasPressed()) {
                settings.enabled = !settings.enabled;
                client.player.sendMessage(Text.of("Tornado Client: " + (settings.enabled ? "§aON" : "§cOFF")), true);
            }
            while (maceKey.wasPressed()) {
                settings.maceOnly = !settings.maceOnly;
                client.player.sendMessage(Text.of("Auto Mace: " + settings.maceOnly), true);
            }
            while (swordKey.wasPressed()) {
                settings.autoSword = !settings.autoSword;
                client.player.sendMessage(Text.of("Auto Sword: " + settings.autoSword), true);
            }
            while (singleStrikeKey.wasPressed()) {
                moduleManager.infiniteAura.manualStrike();
            }

            // Run Modules (This runs all your hacks: Flight, Speed, PearlBot, etc.)
            moduleManager.onTick();
        });
    }
}