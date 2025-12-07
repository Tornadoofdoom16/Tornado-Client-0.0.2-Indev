package net.example.infiniteaura.fairplay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.Items;

/**
 * Executes defensive actions on the client when the coordinator activates a DefensePlan.
 *
 * All actions are scheduled on the client thread to ensure thread-safety with
 * player/inventory/interactions.
 */
public class FairPlayDefenseExecutor {

    public static void execute(DefensePlan plan) {
        if (plan == null || plan.defenseType == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;

        // Schedule on client thread
        mc.execute(() -> {
            try {
                switch (plan.defenseType) {
                    case PHASE_DODGE -> doPhaseDodge(mc);
                    case DUAL_TOTEM -> doDualTotem(mc);
                    case STAGGER_GUARD -> doStaggerGuard(mc);
                    case THROTTLE_DEFENSE -> doThrottle(mc);
                    case NONE -> {}
                }
            } catch (Exception e) {
                System.out.println("[FairPlay] DefenseExecutor error: " + e.getMessage());
            }
        });
    }

    private static void doPhaseDodge(MinecraftClient mc) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        // Send a small lateral/vertical teleport and return to try to avoid incoming hits.
        double ox = mc.player.getX();
        double oy = mc.player.getY();
        double oz = mc.player.getZ();

        // Small upward and forward hop
        double nx = ox;
        double ny = oy + 0.5;
        double nz = oz;

        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(nx, ny, nz, false));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(ox, oy, oz, mc.player.isOnGround()));

        System.out.println("[FairPlay] Performed phase dodge.");
    }

    private static void doDualTotem(MinecraftClient mc) {
        if (mc.player == null || mc.interactionManager == null) return;

        // Try to ensure both hands have totems. Prefer offhand (autoTotem already handles it),
        // but we'll attempt to move a totem into main hand quickly.
        int slot = findTotemSlot(mc);
        if (slot == -1) return;

        // If totem already in main hand, nothing to do
        if (mc.player.getMainHandStack().getItem() == Items.TOTEM_OF_UNDYING && mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;

        // Move to offhand (45) with interaction clicks like AutoTotem.moveTotem
        try {
            int containerSlot = slot < 9 ? slot + 36 : slot;
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, containerSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, 45, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, containerSlot, 0, SlotActionType.PICKUP, mc.player);

            // Then try to place one in main hand if dual totems desired: swap into main hotbar
            if (mc.player.getMainHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                int slot2 = findTotemSlot(mc);
                if (slot2 != -1) {
                    if (slot2 < 9) mc.player.getInventory().selectedSlot = slot2;
                    else mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot2, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
                }
            }
        } catch (Exception e) {
            System.out.println("[FairPlay] Dual totem action failed: " + e.getMessage());
        }
    }

    private static int findTotemSlot(MinecraftClient mc) {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) return i;
        }
        return -1;
    }

    private static void doStaggerGuard(MinecraftClient mc) {
        // Minimal implementation: small jump to reduce hit chance and swing
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.3, mc.player.getZ(), false));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
        System.out.println("[FairPlay] Performed stagger guard.");
    }

    private static void doThrottle(MinecraftClient mc) {
        // Throttle: no immediate movement; could set flags to reduce auto-attacks.
        System.out.println("[FairPlay] Throttle defense activated (no-op).");
    }
}
