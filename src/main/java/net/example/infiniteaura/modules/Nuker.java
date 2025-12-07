package net.example.infiniteaura.modules;

import net.example.infiniteaura.InfiniteAuraSettings;
import net.example.infiniteaura.client.Module;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Nuker extends Module {
    private final InfiniteAuraSettings settings = InfiniteAuraSettings.INSTANCE;

    @Override
    public void onTick() {
        if (!settings.enabled || !settings.nuker || mc.player == null || mc.world == null) return;

        int range = settings.nukerRange;
        int blocksBroken = 0;
        BlockPos playerPos = mc.player.getBlockPos();

        // 1. Scan for blocks
        List<BlockPos> targetBlocks = new ArrayList<>();
        
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    
                    // Skip air and unbreakable blocks (Bedrock)
                    if (mc.world.getBlockState(pos).isAir() || mc.world.getBlockState(pos).getHardness(mc.world, pos) == -1.0f) {
                        continue;
                    }
                    
                    targetBlocks.add(pos);
                }
            }
        }

        // 2. Sort by distance (Closest first prevents Reach kicks)
        Vec3d eyePos = mc.player.getEyePos();
        targetBlocks.sort(Comparator.comparingDouble(pos -> 
            pos.toCenterPos().squaredDistanceTo(eyePos)
        ));

        // 3. Break Blocks (Limit per tick)
        for (BlockPos pos : targetBlocks) {
            if (blocksBroken >= settings.nukerSpeed) break; // Speed Limit

            if (mc.getNetworkHandler() != null) {
                // START_DESTROY packet tells server we started mining
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP
                ));
                
                // STOP_DESTROY packet tells server we finished
                // Sending both instantly is the "Instant Mine" exploit
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP
                ));
                
                // Optional: Swing hand to look legit
                mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            }
            
            blocksBroken++;
        }
    }
}