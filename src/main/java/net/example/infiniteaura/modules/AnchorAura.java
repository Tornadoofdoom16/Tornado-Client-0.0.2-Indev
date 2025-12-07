package net.example.infiniteaura.modules;

import net.example.infiniteaura.InfiniteAuraSettings;
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

public class AnchorAura extends Module {
    private final InfiniteAuraSettings settings = InfiniteAuraSettings.INSTANCE;

    @Override
    public void onTick() {
        if (!settings.enabled || !settings.anchorAura || mc.player == null) return;

        PlayerEntity target = getClosestTarget();
        if (target == null) return;

        int anchorSlot = findSlot(Items.RESPAWN_ANCHOR);
        int glowstoneSlot = findSlot(Items.GLOWSTONE);
        if (anchorSlot == -1 || glowstoneSlot == -1) return;

        // Place Anchor above target head or at feet
        BlockPos placePos = target.getBlockPos().up(2); 
        if (!mc.world.getBlockState(placePos).isAir()) placePos = target.getBlockPos();

        int originalSlot = mc.player.getInventory().selectedSlot;

        if (mc.getNetworkHandler() != null) {
            Vec3d targetVec = new Vec3d(placePos.getX() + 0.5, placePos.getY(), placePos.getZ() + 0.5);

            // 1. TP to Spot
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(targetVec.x, targetVec.y - 1, targetVec.z, false));

            // 2. Place Anchor
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(anchorSlot));
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(targetVec, Direction.UP, placePos, false), 0));

            // 3. Charge Anchor (Interact with Glowstone)
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(glowstoneSlot));
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(targetVec, Direction.UP, placePos, false), 0));
            
            // 4. Detonate (Interact with Anchor again while holding glowstone or hand)
            // Some servers require empty hand or non-glowstone item to detonate, but usually charging item works if full.
            // Packet Spam for Totem Bypass
            for(int i=0; i < settings.attackPackets; i++) {
                mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(targetVec, Direction.UP, placePos, false), 0));
            }

            // 5. TP Back
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
            
            // 6. Reset Slot
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));
        }
    }

    private int findSlot(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) return i;
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