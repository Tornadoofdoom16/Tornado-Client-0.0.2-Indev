package net.example.infiniteaura.modules;

import net.example.infiniteaura.TornadoClientSettings;
import net.example.infiniteaura.client.Module;
import net.example.infiniteaura.client.ModuleManager;
import net.example.infiniteaura.fairplay.FairPlaySignal;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class CrystalAura extends Module {
    private final TornadoClientSettings settings = TornadoClientSettings.INSTANCE;

    @Override
    public void onTick() {
        if (!settings.enabled || !settings.crystalAura || mc.player == null) return;

        PlayerEntity target = getClosestTarget();
        if (target == null) return;

        int crystalSlot = findSlot(Items.END_CRYSTAL);
        if (crystalSlot == -1) return;

        // Find valid placement block (Obsidian/Bedrock) near target
        BlockPos placePos = findCrystalBase(target);
        if (placePos == null) return;

        int originalSlot = mc.player.getInventory().selectedSlot;

        // Announce impending crystal place/OP action for local FairPlay testing
        try {
            if (ModuleManager.INSTANCE.fairPlayModule != null && mc.player != null) {
                String senderHash = mc.player.getUuid().toString();
                ModuleManager.INSTANCE.fairPlayModule.announceAction(FairPlaySignal.ActionType.CRYSTAL_PLACE, 200L, senderHash);
            }
        } catch (Exception ignored) {}

        if (mc.getNetworkHandler() != null) {
            Vec3d placeVec = new Vec3d(placePos.getX() + 0.5, placePos.getY() + 1, placePos.getZ() + 0.5);

            // 1. TP to Base
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(placeVec.x, placeVec.y + 1, placeVec.z, false));

            // 2. Place Crystal
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(crystalSlot));
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(placeVec, Direction.UP, placePos, false), 0));

            // 3. Attack Crystal (Insta-Break)
            // We need to guess the entity ID or just attack area. 
            // Since we can't easily guess entity ID in one tick before server spawns it, 
            // "Insta" crystal usually requires waiting 1 tick OR attacking any nearby crystal.
            // For true 1-tick, we often just attack valid crystals already existing.
            
            // For this implementation, we will try to attack any crystal at that spot immediately
            for (EndCrystalEntity crystal : mc.world.getEntitiesByClass(EndCrystalEntity.class, mc.world.getBlockState(placePos).getCollisionShape(mc.world, placePos).getBoundingBox().offset(placePos).expand(1), c -> true)) {
                 mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, false));
            }
            
            // 4. TP Back
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));

            // 5. Reset Slot
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));
        }
    }

    private BlockPos findCrystalBase(PlayerEntity target) {
        BlockPos tPos = target.getBlockPos();
        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos check = tPos.add(x, y, z);
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
        double closestDist = settings.range * settings.range;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p != mc.player && !settings.friendsList.contains(p.getName().getString())) {
                // Passive respect: skip if player recently announced FairPlay
                if (settings.fairPlayRespectSignals && ModuleManager.INSTANCE.fairPlayModule != null) {
                    String id = p.getUuid().toString();
                    if (ModuleManager.INSTANCE.fairPlayModule.shouldRespectSender(id)) {
                        net.example.infiniteaura.fairplay.FairPlayUI.showSkipIndicator(p.getName().getString());
                        continue;
                    }
                }
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