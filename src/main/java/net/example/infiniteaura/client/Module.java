package net.example.infiniteaura.client;

import net.minecraft.client.MinecraftClient;

public abstract class Module {
    protected final MinecraftClient mc = MinecraftClient.getInstance();

    public void onTick() {
        // Override this in specific modules
    }

    public void onEnable() {
        // Run when turned on
    }

    public void onDisable() {
        // Run when turned off
    }
}