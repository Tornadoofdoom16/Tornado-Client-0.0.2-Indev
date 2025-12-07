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
    private static KeyBinding fairPlayKey, fairPlayNetKey;
    private static KeyBinding fairPlayRespectKey, fairPlayTtlIncKey, fairPlayTtlDecKey;
    private static KeyBinding clickTpKey, doorTpKey;
    private static KeyBinding clickTpToggleKey, doorTpToggleKey;
    
    // References
    private final TornadoClientSettings settings = TornadoClientSettings.INSTANCE;
    private final ModuleManager moduleManager = ModuleManager.INSTANCE;

    @Override
    public void onInitializeClient() {
        // 1. Register Keybinds
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tornadoclient.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.tornadoclient"));
        uiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tornadoclient.ui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_U, "category.tornadoclient"));
        maceKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tornadoclient.mace", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_M, "category.tornadoclient"));
        swordKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tornadoclient.sword", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.tornadoclient"));
        singleStrikeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tornadoclient.single", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.tornadoclient"));
        fairPlayKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tornadoclient.fairplay", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, "category.tornadoclient"));
        fairPlayNetKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tornadoclient.fairplay.net", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, "category.tornadoclient"));
        fairPlayRespectKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tornadoclient.fairplay.respect", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_SEMICOLON, "category.tornadoclient"));
        fairPlayTtlIncKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tornadoclient.fairplay.ttl_inc", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_COMMA, "category.tornadoclient"));
        fairPlayTtlDecKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tornadoclient.fairplay.ttl_dec", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_PERIOD, "category.tornadoclient"));
        clickTpKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tornadoclient.clicktp", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "category.tornadoclient"));
        doorTpKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tornadoclient.doortp", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, "category.tornadoclient"));
        clickTpToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tornadoclient.clicktp.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "category.tornadoclient"));
        doorTpToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tornadoclient.doortp.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "category.tornadoclient"));

        // 2. Main Loop
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            // Handle Input
            while (uiKey.wasPressed()) client.setScreen(new TornadoClientScreen(client.currentScreen));
            
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

            while (fairPlayKey.wasPressed()) {
                settings.fairPlayEnabled = !settings.fairPlayEnabled;
                if (ModuleManager.INSTANCE.fairPlayModule != null) {
                    if (settings.fairPlayEnabled) ModuleManager.INSTANCE.fairPlayModule.enable();
                    else ModuleManager.INSTANCE.fairPlayModule.disable();
                }
                client.player.sendMessage(Text.of("FairPlay: " + (settings.fairPlayEnabled ? "§aENABLED" : "§cDISABLED")), true);
            }

            while (fairPlayNetKey.wasPressed()) {
                settings.fairPlayNetworkEmit = !settings.fairPlayNetworkEmit;
                // Configure FairPlayConfig to enable/disable network emission
                try {
                    net.example.infiniteaura.fairplay.FairPlayConfig.getInstance().setNetworkEmitEnabled(settings.fairPlayNetworkEmit);
                } catch (Exception ignored) {}
                client.player.sendMessage(Text.of("FairPlay Network Emit: " + settings.fairPlayNetworkEmit), true);
            }
            while (fairPlayRespectKey.wasPressed()) {
                settings.fairPlayRespectSignals = !settings.fairPlayRespectSignals;
                client.player.sendMessage(Text.of("FairPlay Respect Mode: " + (settings.fairPlayRespectSignals ? "§aENABLED" : "§cDISABLED") + " (TTL " + settings.fairPlayRespectTtlMs + "ms)"), true);
            }

            while (fairPlayTtlIncKey.wasPressed()) {
                settings.fairPlayRespectTtlMs = Math.min(5000L, settings.fairPlayRespectTtlMs + 100L);
                client.player.sendMessage(Text.of("FairPlay TTL: " + settings.fairPlayRespectTtlMs + "ms"), true);
            }

            while (fairPlayTtlDecKey.wasPressed()) {
                settings.fairPlayRespectTtlMs = Math.max(100L, settings.fairPlayRespectTtlMs - 100L);
                client.player.sendMessage(Text.of("FairPlay TTL: " + settings.fairPlayRespectTtlMs + "ms"), true);
            }

            while (clickTpKey.wasPressed()) {
                moduleManager.clickTP.activate();
            }

            while (doorTpKey.wasPressed()) {
                moduleManager.doorTP.activate();
            }

            while (clickTpToggleKey.wasPressed()) {
                settings.clickTp = !settings.clickTp;
                client.player.sendMessage(Text.of("ClickTP: " + (settings.clickTp ? "§aENABLED" : "§cDISABLED")), true);
            }

            while (doorTpToggleKey.wasPressed()) {
                settings.doorTp = !settings.doorTp;
                client.player.sendMessage(Text.of("DoorTP: " + (settings.doorTp ? "§aENABLED" : "§cDISABLED")), true);
            }

            // Run Modules (This runs all your hacks: Flight, Speed, PearlBot, etc.)
            moduleManager.onTick();
        });
    }
}