package net.example.infiniteaura.modules;

import net.example.infiniteaura.InfiniteAuraSettings;
import net.example.infiniteaura.client.Module;
import net.minecraft.util.math.Vec3d;

public class Flight extends Module {
    private final InfiniteAuraSettings settings = InfiniteAuraSettings.INSTANCE;

    @Override
    public void onTick() {
        if (!settings.enabled || !settings.flight || mc.player == null) return;

        // 1. Cancel Gravity (Set Y velocity to 0 if not holding jump/sneak)
        mc.player.getAbilities().flying = false; // Ensure vanilla flying is off to avoid conflicts
        
        double speed = 1.0; // Flight Speed
        
        // 2. Handle Vertical Movement
        double y = 0;
        if (mc.options.jumpKey.isPressed()) {
            y = speed;
        } else if (mc.options.sneakKey.isPressed()) {
            y = -speed;
        }

        // 3. Handle Horizontal Movement
        // We set velocity directly for instant response
        Vec3d forward = Vec3d.fromPolar(0, mc.player.getYaw()).normalize();
        Vec3d right = Vec3d.fromPolar(0, mc.player.getYaw() + 90).normalize();
        
        double velX = 0;
        double velZ = 0;

        if (mc.options.forwardKey.isPressed()) {
            velX += forward.x * speed;
            velZ += forward.z * speed;
        }
        if (mc.options.backKey.isPressed()) {
            velX -= forward.x * speed;
            velZ -= forward.z * speed;
        }
        if (mc.options.leftKey.isPressed()) {
            velX -= right.x * speed;
            velZ -= right.z * speed;
        }
        if (mc.options.rightKey.isPressed()) {
            velX += right.x * speed;
            velZ += right.z * speed;
        }

        mc.player.setVelocity(velX, y, velZ);
    }
}