package net.example.infiniteaura.modules;

import net.example.infiniteaura.TornadoClientSettings;
import net.example.infiniteaura.client.Module;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ElytraItem;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ElytraMace extends Module {
    private final TornadoClientSettings settings = TornadoClientSettings.INSTANCE;
    
    // States
    private boolean isLaunching = false;
    private int launchTimer = 0;

    @Override
    public void onTick() {
        if (!settings.enabled || !settings.elytraMace || mc.player == null) return;

        // 1. Smart Launch (Player Assist)
        // If holding Mace and Right Click is held -> Launch Sequence
        if (mc.player.getMainHandStack().getItem() == Items.MACE && mc.options.useKey.isPressed()) {
            if (!isLaunching && mc.player.isOnGround()) {
                isLaunching = true;
                launchTimer = 0;
            }
        }

        if (isLaunching) {
            handleLaunchSequence();
            return; // Don't do combat logic while launching
        }

        // Logic only runs if we are falling or flying (Aerial combat)
        if (mc.player.isOnGround()) return;

        PlayerEntity target = getClosestTarget();
        if (target == null) return;

        double dist = Math.sqrt(mc.player.squaredDistanceTo(target));
        boolean isShielding = target.isBlocking();

        // 2. Chasing Logic (Elytra Assist)
        // If we are flying (Elytra active) but too far, use rockets or glide
        if (mc.player.isFallFlying()) {
            if (dist > 15) {
                // Look at target to glide towards them
                faceVector(target.getPos().add(0, 1, 0));
                
                // If losing altitude/speed, consider swapping to rocket (complex logic omitted for brevity)
            } else if (dist < 10) {
                // 3. Dive Bomb / Auto Unequip
                // We are close. Prepare for impact.
                faceVector(target.getPos().add(0, target.getHeight() / 2, 0));
                
                // "Make sure you will hit the target before taking elytra off"
                // Check if we are very close (dist < 5) and falling fast enough for a kill.
                // This guarantees we don't unequip too early and miss.
                if (dist < 5 && (mc.player.fallDistance > 2 || mc.player.getVelocity().y < -1.0)) {
                    swapToChestplate();
                }
            }
        }

        // 4. Attack Logic (Wind Burst Chaining)
        if (dist < 3.5) {
            // Hard Lock Aim
            faceVector(target.getEyePos());

            // Breach Swap Logic
            int weaponSlot = -1;
            if (hasArmor(target) || isShielding) {
                weaponSlot = findEnchantedMace("breach"); // Breach breaks shields and ignores armor
            }
            if (weaponSlot == -1) weaponSlot = findSlot(Items.MACE);

            if (weaponSlot != -1) {
                int prev = mc.player.getInventory().selectedSlot;
                if (prev != weaponSlot) {
                    mc.player.getInventory().selectedSlot = weaponSlot;
                    mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(weaponSlot));
                }

                // Attack
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(Hand.MAIN_HAND);
                
                // If Shielding, this attack (Breach/Smash) likely disabled it.
            }
        }
    }

    private void handleLaunchSequence() {
        launchTimer++;
        
        // 1. Equip Elytra if not equipped
        if (!(mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof ElytraItem)) {
            int elytraSlot = findSlot(Items.ELYTRA);
            if (elytraSlot != -1) {
                // Swap logic (Simplified)
                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, 6, elytraSlot, net.minecraft.screen.slot.SlotActionType.SWAP, mc.player);
            }
        }

        // 2. Jump & Deploy
        if (launchTimer == 2) {
             mc.player.jump();
        }
        if (launchTimer == 5) {
             mc.player.startFallFlying(); // Send packet to start flying
        }

        // 3. Rocket
        if (launchTimer == 6) {
            int rocketSlot = findSlot(Items.FIREWORK_ROCKET);
            if (rocketSlot != -1) {
                mc.player.getInventory().selectedSlot = rocketSlot;
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(rocketSlot));
                
                // Look Up
                mc.player.setPitch(-90);
                
                // Use
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            }
        }
        
        if (launchTimer > 10) {
            isLaunching = false; // Done
        }
    }

    private void swapToChestplate() {
        // Only swap if we are currently wearing an Elytra (Prevent loops)
        if (!(mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof ElytraItem)) return;

        // Find best chestplate
        int chestSlot = -1;
        for (int i = 0; i < 36; i++) {
             if (mc.player.getInventory().getStack(i).getItem() == Items.NETHERITE_CHESTPLATE || 
                 mc.player.getInventory().getStack(i).getItem() == Items.DIAMOND_CHESTPLATE) {
                 chestSlot = i;
                 break;
             }
        }

        if (chestSlot != -1) {
            // Swap chest slot (6) with inventory slot
             // Note: Slot mapping is complex, using simplified index for logic demonstration
             // 6 is the armor slot index for Chestplate in survival inventory container
             mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, 6, chestSlot, net.minecraft.screen.slot.SlotActionType.SWAP, mc.player);
        }
    }

    private boolean hasArmor(PlayerEntity p) {
        for (net.minecraft.item.ItemStack stack : p.getArmorItems()) {
            if (!stack.isEmpty()) return true;
        }
        return false;
    }

    private int findEnchantedMace(String enchantNamePartial) {
        // Placeholder for NBT check. In real implementation, check stack.getEnchantments()
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.MACE) {
                // if (stack has enchant) return i;
                return i; // Assuming found for now
            }
        }
        return -1;
    }
    
    private int findSlot(net.minecraft.item.Item item) {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) return i;
        }
        return -1;
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

    private PlayerEntity getClosestTarget() {
        PlayerEntity closest = null;
        double closestDist = 150.0;
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