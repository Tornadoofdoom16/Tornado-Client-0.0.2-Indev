package net.example.infiniteaura.modules;

import net.example.infiniteaura.TornadoClientSettings;
import net.example.infiniteaura.client.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class InfiniteAura extends Module {
    private final TornadoClientSettings settings = TornadoClientSettings.INSTANCE;
    
    private int timer = 0;
    private LivingEntity singleRetaliationTarget = null;
    private int retaliationTimer = 0;
    private int revengeTimer = 0; 

    public void triggerRevenge() {
        if (settings.enabled && settings.totemRevenge) {
            revengeTimer = 10;
            if (mc.player != null) {
                mc.player.sendMessage(net.minecraft.text.Text.of("§c§lTOTEM POPPED! ENGAGING REVENGE MODE!"), true);
            }
        }
    }

    @Override
    public void onTick() {
        // Check Retaliation
        if (settings.retaliation && mc.player != null) {
            Entity attacker = mc.player.getAttacker();
            if (attacker instanceof LivingEntity livingAttacker) {
                singleRetaliationTarget = livingAttacker;
                retaliationTimer = 20; 
            }
        }

        if (retaliationTimer > 0) retaliationTimer--;
        if (revengeTimer > 0) revengeTimer--; 

        // Check if Main Toggle is On
        if (!settings.enabled && revengeTimer <= 0) return;

        // Timer Logic (Ignore delay if in Revenge Mode)
        if (revengeTimer <= 0) {
            if (timer > 0) { timer--; return; }
            timer = settings.delayTicks;
        }

        // Run Logic (False = Auto Mode)
        executeAura(false); 
    }

    // Called by the manual keybind V
    public void manualStrike() {
        executeAura(true);
    }

    private void executeAura(boolean force) {
        if (mc.player == null || mc.world == null) return;

        int originalSlot = mc.player.getInventory().selectedSlot;
        int weaponSlot = -1;
        boolean usingMace = false;

        // --- 1. DETERMINE WEAPON ---
        if (force) {
            if (mc.player.getMainHandStack().getItem() == Items.MACE) usingMace = true;
        } else {
            boolean revengeOverride = (revengeTimer > 0);
            if (settings.maceOnly || revengeOverride) {
                for (int i = 0; i < 9; i++) {
                    if (mc.player.getInventory().getStack(i).getItem() == Items.MACE) {
                        weaponSlot = i;
                        usingMace = true;
                        break;
                    }
                }
            }
            if (weaponSlot == -1 && settings.autoSword) {
                 for (int i = 0; i < 9; i++) {
                    if (mc.player.getInventory().getStack(i).getItem() instanceof SwordItem) {
                        weaponSlot = i;
                        usingMace = false;
                        break;
                    }
                }
            }
            if ((settings.maceOnly || settings.autoSword) && weaponSlot == -1) return; 
        }

        // --- 2. GET TARGETS ---
        List<LivingEntity> targets = new ArrayList<>();
        if (revengeTimer > 0) targets = getAllTargets();
        else {
            LivingEntity t = getSingleTarget();
            if (t != null) targets.add(t);
        }

        if (targets.isEmpty()) return;

        // --- 3. PACKETS ---
        List<Packet<?>> packetQueue = new ArrayList<>();
        Vec3d originPos = mc.player.getPos();
        
        if (weaponSlot != -1 && weaponSlot != originalSlot) {
            packetQueue.add(new UpdateSelectedSlotC2SPacket(weaponSlot));
        }

        for (LivingEntity target : targets) {
            Vec3d targetPos = target.getPos().add(target.getVelocity().multiply(2)).add(0, 0.5, 0);

            packetQueue.add(new PlayerMoveC2SPacket.PositionAndOnGround(targetPos.x, targetPos.y + 0.1, targetPos.z, false));
            
            if (usingMace) {
                int safeBlocks = getMaxHeightAbove(originPos);
                if (safeBlocks > 5) {
                    double peakY = originPos.y + Math.min(settings.fallHeight, safeBlocks);
                    packetQueue.add(new PlayerMoveC2SPacket.PositionAndOnGround(originPos.x, peakY, originPos.z, false));
                    packetQueue.add(new PlayerMoveC2SPacket.PositionAndOnGround(originPos.x, peakY, originPos.z, false)); 
                    packetQueue.add(new PlayerMoveC2SPacket.PositionAndOnGround(targetPos.x, targetPos.y + 1.0, targetPos.z, false));
                }
            } else {
                packetQueue.add(new PlayerMoveC2SPacket.PositionAndOnGround(targetPos.x, targetPos.y + 0.11, targetPos.z, false));
                packetQueue.add(new PlayerMoveC2SPacket.PositionAndOnGround(targetPos.x, targetPos.y, targetPos.z, false)); 
            }

            for (int i = 0; i < settings.attackPackets; i++) {
                packetQueue.add(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
            }
            packetQueue.add(new net.minecraft.network.packet.c2s.play.HandSwingC2SPacket(Hand.MAIN_HAND));
        }

        packetQueue.add(new PlayerMoveC2SPacket.PositionAndOnGround(originPos.x, originPos.y, originPos.z, mc.player.isOnGround()));

        if (weaponSlot != -1 && weaponSlot != originalSlot) {
            packetQueue.add(new UpdateSelectedSlotC2SPacket(originalSlot));
        }

        if (mc.getNetworkHandler() != null) {
            for (Packet<?> packet : packetQueue) mc.getNetworkHandler().sendPacket(packet);
        }
    }

    private List<LivingEntity> getAllTargets() {
        List<LivingEntity> all = new ArrayList<>();
        double rangeSq = settings.range * settings.range;
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof LivingEntity living && e != mc.player && !living.isDead() && living instanceof PlayerEntity p) {
                if (settings.friendsList.contains(p.getName().getString())) continue; 
                if (mc.player.squaredDistanceTo(living) <= rangeSq) all.add(living);
            }
        }
        return all;
    }

    private LivingEntity getSingleTarget() {
        if (settings.retaliation && singleRetaliationTarget != null && !singleRetaliationTarget.isDead()) {
             if (singleRetaliationTarget instanceof PlayerEntity p && settings.friendsList.contains(p.getName().getString())) return null;
             if (mc.player.squaredDistanceTo(singleRetaliationTarget) <= settings.range * settings.range) return singleRetaliationTarget;
        }
        LivingEntity closest = null;
        double closestDist = settings.range * settings.range;
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof LivingEntity living && e != mc.player && !living.isDead() && living instanceof PlayerEntity p) {
                String name = p.getName().getString();
                if (settings.friendsList.contains(name)) continue; 
                if (settings.useTargetList && !settings.targetList.contains(name)) continue;
                double distSq = mc.player.squaredDistanceTo(e);
                if (distSq < closestDist) { closest = living; closestDist = distSq; }
            }
        }
        return closest;
    }

    private int getMaxHeightAbove(Vec3d pos) {
        BlockPos start = BlockPos.ofFloored(pos);
        for (int i = 1; i <= settings.fallHeight + 10; i++) {
            BlockPos check = start.up(i);
            if (!mc.world.getBlockState(check).isAir()) return i - 1;
        }
        return settings.fallHeight;
    }
}