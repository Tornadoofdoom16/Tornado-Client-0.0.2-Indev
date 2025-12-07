package net.example.infiniteaura.modules;

import net.example.infiniteaura.TornadoClientSettings;
import net.example.infiniteaura.client.Module;
import net.example.infiniteaura.client.ModuleManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;

public class PVPBot extends Module {
    private final TornadoClientSettings settings = TornadoClientSettings.INSTANCE;
    private enum CombatMode { SWORD, MACE_AERIAL, CRYSTAL, CART, IDLE }
    private CombatMode currentMode = CombatMode.IDLE;
    
    // Aerial Bot State
    private int aerialState = 0; // 0=Ground, 1=Takeoff, 2=Ascending, 3=Diving
    private int flightTimer = 0;

    @Override
    public void onTick() {
        if (!settings.enabled || !settings.pvpBot || mc.player == null) return;

        PlayerEntity target = getClosestTarget();
        if (target == null) {
            currentMode = CombatMode.IDLE;
            return; 
        }

        // 1. Analyze Kit & Situation to Pick Strategy
        decideStrategy(target);

        // 2. Execute Strategy
        switch (currentMode) {
            case MACE_AERIAL:
                runAerialBot(target);
                break;

            case CRYSTAL:
                ModuleManager.INSTANCE.legitCrystal.onTick();
                autoMove(target, 4.0);
                break;

            case CART:
                // Only fire macro if stationary or set up
                if (mc.player.age % 15 == 0) ModuleManager.INSTANCE.cartPVP.activate();
                ModuleManager.INSTANCE.cartPVP.onTick();
                break;

            case SWORD:
            default:
                // If target has armor, use Breach Swapping logic (via LegitKillAura or custom here)
                // For Bot, we ensure LegitKillAura is handling the aiming/hitting
                ModuleManager.INSTANCE.legitKillAura.onTick();
                autoMove(target, 3.0);
                break;
        }
    }

    private void decideStrategy(PlayerEntity target) {
        // If we are already in the air flying, stick to Mace Aerial
        if (mc.player.isFallFlying()) {
            currentMode = CombatMode.MACE_AERIAL;
            return;
        }

        // Check Inventory
        boolean hasMace = hasItem(Items.MACE);
        boolean hasElytra = hasItem(Items.ELYTRA) || mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;
        boolean hasRockets = hasItem(Items.FIREWORK_ROCKET);
        boolean hasCrystals = hasItem(Items.END_CRYSTAL);
        
        double dist = Math.sqrt(mc.player.squaredDistanceTo(target));

        if (hasMace && hasElytra && hasRockets && dist > 8) {
            // Far away? Take to the skies!
            currentMode = CombatMode.MACE_AERIAL;
        } else if (hasCrystals && dist < 10) {
            currentMode = CombatMode.CRYSTAL;
        } else if (hasMace && dist < 5) {
            // Close range mace? Probably Sword/Breach mode
            currentMode = CombatMode.SWORD; // LegitKillAura handles breach swapping
        } else {
            currentMode = CombatMode.SWORD;
        }
    }

    private void runAerialBot(PlayerEntity target) {
        // This is the "Pro Player" Elytra logic
        
        // State 0: Ground -> Equip & Jump
        if (!mc.player.isFallFlying()) {
            aerialState = 0;
            
            // Equip Elytra
            if (!(mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof ElytraItem)) {
                int slot = findSlot(Items.ELYTRA);
                if (slot != -1) mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, 6, slot, net.minecraft.screen.slot.SlotActionType.SWAP, mc.player);
            }
            
            // Jump to activate
            if (mc.player.isOnGround()) mc.player.jump();
            else if (mc.player.fallDistance > 0.1) {
                mc.player.startFallFlying(); 
                aerialState = 1;
            }
        }
        
        // State 1/2: Rocket Up
        if (mc.player.isFallFlying()) {
            if (mc.player.getY() < target.getY() + 20) {
                // We need height
                mc.player.setPitch(-90); // Look Up
                
                // Use Rocket every 20 ticks if needed
                if (flightTimer++ > 15) {
                    int rSlot = findSlot(Items.FIREWORK_ROCKET);
                    if (rSlot != -1) {
                        mc.player.getInventory().selectedSlot = rSlot;
                        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(rSlot));
                        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                        flightTimer = 0;
                    }
                }
            } else {
                // High enough -> DIVE!
                // Let ElytraMace module handle the aiming and auto-hit
                ModuleManager.INSTANCE.elytraMace.onTick();
            }
        }
    }

    private void autoMove(PlayerEntity target, double stopDist) {
        // Basic AI movement
        double dist = Math.sqrt(mc.player.squaredDistanceTo(target));
        
        // Look at target
        // (Usually handled by the specific aura modules, but we ensure we face generally right)
        
        if (dist > stopDist) {
            mc.options.forwardKey.setPressed(true);
            if (mc.player.horizontalCollision) mc.player.jump();
            if (mc.player.isTouchingWater()) mc.player.jump();
        } else {
            mc.options.forwardKey.setPressed(false);
        }
    }

    private boolean hasItem(net.minecraft.item.Item item) {
        return findSlot(item) != -1;
    }

    private int findSlot(net.minecraft.item.Item item) {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) return i;
        }
        return -1;
    }
    
    private PlayerEntity getClosestTarget() {
        double closestDist = 100.0;
        PlayerEntity closest = null;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p != mc.player && !settings.friendsList.contains(p.getName().getString())) {
                double dist = mc.player.squaredDistanceTo(p);
                if (dist < closestDist) { closest = p; closestDist = dist; }
            }
        }
        return closest;
    }
}