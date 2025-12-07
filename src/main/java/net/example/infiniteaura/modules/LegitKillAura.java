package net.example.infiniteaura.modules;

import net.example.infiniteaura.TornadoClientSettings;
import net.example.infiniteaura.client.Module;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class LegitKillAura extends Module {
    private final TornadoClientSettings settings = TornadoClientSettings.INSTANCE;

    @Override
    public void onTick() {
        if (!settings.enabled || !settings.legitKillAura || mc.player == null || mc.world == null) return;

        PlayerEntity target = getClosestTarget();
        if (target == null) return;

        // 1. Aim Assist
        faceVector(target.getEyePos());

        // 2. Distance Maintenance (Spacing / Combo Mode)
        // Only do this if we are on the ground (trying to combo).
        // If we are in the air, we are likely trying to Crit, so we need forward momentum.
        if (mc.player.isOnGround()) {
             double dist = Math.sqrt(mc.player.squaredDistanceTo(target));
             
             // Perfect combo range is usually ~3.0 blocks
             if (dist < 2.5) {
                 // Too close! Stop moving forward or tap S
                 mc.options.forwardKey.setPressed(false);
                 mc.options.backKey.setPressed(true);
             } else if (dist > 3.5) {
                 // Too far, move in
                 mc.options.forwardKey.setPressed(true);
                 mc.options.backKey.setPressed(false);
             }
        }

        // 3. Attack Logic
        if (mc.player.getAttackCooldownProgress(0.5f) >= 1.0f) {
            
            // Shield Breaker
            if (target.isBlocking()) {
                int axeSlot = findAxe();
                if (axeSlot != -1) {
                    int prevSlot = mc.player.getInventory().selectedSlot;
                    mc.player.getInventory().selectedSlot = axeSlot;
                    if (mc.getNetworkHandler() != null)
                        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(axeSlot));

                    attack(target); // Stun Slam

                    mc.player.getInventory().selectedSlot = prevSlot;
                    if (mc.getNetworkHandler() != null)
                        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
                    return; 
                }
            }

            // Normal Attack
            int swordSlot = findSword();
            if (swordSlot != -1 && mc.player.getInventory().selectedSlot != swordSlot) {
                 mc.player.getInventory().selectedSlot = swordSlot; 
            }
            
            attack(target);
        }
    }

    private void attack(PlayerEntity target) {
        // W-Tap logic: Only if on ground (Combos). Don't reset sprint if mid-air (Crits).
        boolean shouldWTap = mc.player.isOnGround() && mc.player.isSprinting();
        
        if (shouldWTap) mc.player.setSprinting(false);
        
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);

        if (shouldWTap) mc.player.setSprinting(true);
    }

    private void faceVector(Vec3d targetVec) {
        Vec3d eyes = mc.player.getEyePos();
        double diffX = targetVec.x - eyes.x;
        double diffY = targetVec.y - eyes.y;
        double diffZ = targetVec.z - eyes.z;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));

        mc.player.setYaw(mc.player.getYaw() + MathHelper.wrapDegrees(yaw - mc.player.getYaw()));
        mc.player.setPitch(mc.player.getPitch() + MathHelper.wrapDegrees(pitch - mc.player.getPitch()));
    }

    private int findAxe() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof AxeItem) return i;
        }
        return -1;
    }

    private int findSword() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof SwordItem) return i;
        }
        return -1;
    }

    private PlayerEntity getClosestTarget() {
        PlayerEntity closest = null;
        double closestDist = 16.0; 
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p != mc.player && !settings.friendsList.contains(p.getName().getString())) {
                double dist = mc.player.squaredDistanceTo(p);
                if (dist < closestDist) {
                    closest = p;
                    closestDist = dist;
                }
            }
        }
        return closest;
    }
}