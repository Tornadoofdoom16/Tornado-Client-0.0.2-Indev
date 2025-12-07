package net.example.infiniteaura.modules;

import net.example.infiniteaura.InfiniteAuraSettings;
import net.example.infiniteaura.client.Module;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class LegitCrystal extends Module {
    private final InfiniteAuraSettings settings = InfiniteAuraSettings.INSTANCE;

    @Override
    public void onTick() {
        if (!settings.enabled || !settings.legitCrystal || mc.player == null || mc.world == null) return;

        PlayerEntity target = getClosestTarget();
        if (target == null) return;

        // 1. Crystal & Base Logic
        int crystalSlot = findSlot(Items.END_CRYSTAL);
        if (crystalSlot == -1) return;

        BlockPos placePos = findCrystalBase(target);
        if (placePos == null) return; // In legit mode, we don't auto-place obsidian to avoid suspicion, or we rely on pre-placed obby.

        int originalSlot = mc.player.getInventory().selectedSlot;

        // 2. Aim Assist (Look at the block)
        Vec3d placeVec = new Vec3d(placePos.getX() + 0.5, placePos.getY() + 1.0, placePos.getZ() + 0.5);
        faceVector(placeVec);

        if (mc.getNetworkHandler() != null) {
            // 3. Place
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(crystalSlot));
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(placeVec, Direction.UP, placePos, false), 0));

            // 4. Break (Fast but legit)
            // Attack the crystal that (hopefully) just spawned or exists there
            for (EndCrystalEntity crystal : mc.world.getEntitiesByClass(EndCrystalEntity.class, mc.world.getBlockState(placePos).getCollisionShape(mc.world, placePos).getBoundingBox().offset(placePos).expand(1), c -> true)) {
                 // Aim at crystal before hitting
                 faceVector(crystal.getPos());
                 mc.interactionManager.attackEntity(mc.player, crystal);
                 mc.player.swingHand(Hand.MAIN_HAND);
            }

            // 5. Switch Back
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));
        }
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

    private BlockPos findCrystalBase(PlayerEntity target) {
        BlockPos tPos = target.getBlockPos();
        // Vanilla Reach only (4 blocks)
        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos check = tPos.add(x, y, z);
                    if (Math.sqrt(mc.player.squaredDistanceTo(check.toCenterPos())) > 4.5) continue;
                    if (mc.world.getBlockState(check).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(check).getBlock() == Blocks.BEDROCK) {
                        if (mc.world.getBlockState(check.up()).isAir()) return check;
                    }
                }
            }
        }
        return null;
    }

    private int findSlot(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) return i;
        }
        return -1;
    }

    private PlayerEntity getClosestTarget() {
        PlayerEntity closest = null;
        double closestDist = 25.0; // 5 blocks squared
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