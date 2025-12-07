package net.example.infiniteaura.modules;

import net.example.infiniteaura.InfiniteAuraSettings;
import net.example.infiniteaura.client.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

public class AntiMace extends Module {
    private final InfiniteAuraSettings settings = InfiniteAuraSettings.INSTANCE;

    @Override
    public void onTick() {
        if (!settings.enabled || !settings.antiMaceDodge || mc.player == null || mc.world == null) return;

        for (Entity e : mc.world.getEntities()) {
            if (e instanceof PlayerEntity p && e != mc.player) {
                if (settings.friendsList.contains(p.getName().getString())) continue;

                // 1. Specific Check: Are they holding a Mace?
                // This lets us lower the sensitivity without dodging random players jumping.
                if (p.getMainHandStack().getItem() != Items.MACE) continue;

                if (mc.player.squaredDistanceTo(e) < 25) { // 5 blocks
                    // 2. Lower Threshold: -0.1 catches normal gravity falls much earlier.
                    // Previous -0.5 was too strict for normal gameplay.
                    if (p.getVelocity().y < -0.1) {
                        if (mc.getNetworkHandler() != null) {
                            Vec3d myPos = mc.player.getPos();
                            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                                myPos.x, myPos.y + 5.0, myPos.z, false
                            ));
                        }
                        return; // Dodge once per tick max
                    }
                }
            }
        }
    }
}