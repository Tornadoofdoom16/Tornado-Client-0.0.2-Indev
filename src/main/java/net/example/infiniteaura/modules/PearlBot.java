package net.example.infiniteaura.modules;

import net.example.infiniteaura.InfiniteAuraSettings;
import net.example.infiniteaura.client.Module;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PearlBot extends Module {
    private final InfiniteAuraSettings settings = InfiniteAuraSettings.INSTANCE;
    
    // Map of Owner Name -> Trapdoor Position
    private final Map<String, BlockPos> trackedPearls = new HashMap<>();

    @Override
    public void onTick() {
        if (!settings.enabled || !settings.pearlBot || mc.world == null) return;

        // 1. Scan for Pearls
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof EnderPearlEntity pearl) {
                // Try to find owner
                Entity owner = ((EnderPearlEntity) e).getOwner();
                if (owner != null) {
                    String ownerName = owner.getName().getString();
                    
                    // Look for a trapdoor nearby (3x3 area)
                    BlockPos pearlPos = pearl.getBlockPos();
                    BlockPos trapdoorPos = findTrapdoor(pearlPos);
                    
                    if (trapdoorPos != null) {
                        trackedPearls.put(ownerName, trapdoorPos);
                    }
                }
            }
        }
    }

    private BlockPos findTrapdoor(BlockPos center) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos check = center.add(x, y, z);
                    if (mc.world.getBlockState(check).getBlock() instanceof TrapdoorBlock) {
                        return check;
                    }
                }
            }
        }
        return null;
    }

    // Called by the Chat Mixin
    public void onChatMessage(String message) {
        if (!settings.enabled || !settings.pearlBot) return;

        // Command format: "pull [PlayerName]" or just "pull" (for self if we could detect sender easily)
        // Simplest: "pull Me"
        
        if (message.toLowerCase().startsWith("pull ")) {
            String targetName = message.substring(5).trim();
            
            if (trackedPearls.containsKey(targetName)) {
                BlockPos trapdoor = trackedPearls.get(targetName);
                triggerTrapdoor(trapdoor);
                mc.player.sendMessage(net.minecraft.text.Text.of("§aPulling " + targetName), true);
            }
        }
    }

    private void triggerTrapdoor(BlockPos pos) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        
        Vec3d origin = mc.player.getPos();
        Vec3d target = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

        // 1. Teleport to Trapdoor
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(target.x, target.y, target.z, false));
        
        // 2. Interact (Open)
        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(target, Direction.UP, pos, false), 0));
        
        // 3. Wait/Interact again (Close) - In same tick for speed
        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(target, Direction.UP, pos, false), 0));

        // 4. Teleport Back
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(origin.x, origin.y, origin.z, mc.player.isOnGround()));
    }
}