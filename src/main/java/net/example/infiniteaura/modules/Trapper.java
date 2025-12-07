package net.example.infiniteaura.modules;

import net.example.infiniteaura.TornadoClientSettings;
import net.example.infiniteaura.client.Module;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class Trapper extends Module {
    private final TornadoClientSettings settings = TornadoClientSettings.INSTANCE;

    @Override
    public void onTick() {
        if (!settings.enabled || !settings.trapper || mc.world == null || mc.player == null) return;

        // 1. Find Target
        PlayerEntity target = getClosestTarget();
        if (target == null) return;

        // 2. Find Trap Positions (Feet + Head)
        BlockPos targetPos = target.getBlockPos();
        BlockPos[] trapOffsets = {
            targetPos.north(), targetPos.south(), targetPos.east(), targetPos.west(), // Feet Surround
            targetPos.up().north(), targetPos.up().south(), targetPos.up().east(), targetPos.up().west(), // Head Surround
            targetPos.up(2) // Roof
        };

        // 3. Prepare Blocks
        int obsidianSlot = findBlockSlot();
        if (obsidianSlot == -1) return;
        int originalSlot = mc.player.getInventory().selectedSlot;

        int blocksPlaced = 0;
        
        // 4. Execution Loop
        if (mc.getNetworkHandler() != null) {
            // Switch to Block
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(obsidianSlot));

            for (BlockPos pos : trapOffsets) {
                if (blocksPlaced >= settings.trapperSpeed) break; // Speed Limit

                if (mc.world.getBlockState(pos).isReplaceable()) {
                    Vec3d vecPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    
                    // INSTA TP: Move to block to place it
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        vecPos.x, vecPos.y + 1, vecPos.z, false
                    ));

                    // Place Packet
                    mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(
                        Hand.MAIN_HAND, new BlockHitResult(vecPos, Direction.UP, pos, false), 0
                    ));
                    
                    blocksPlaced++;
                }
            }

            // TP Back
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()
            ));

            // Switch Back
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));
        }
    }

    private int findBlockSlot() {
        // Prioritize Obsidian
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.OBSIDIAN) return i;
        }
        return -1;
    }

    private PlayerEntity getClosestTarget() {
        PlayerEntity closest = null;
        double closestDist = settings.range * settings.range;
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