package net.example.infiniteaura.modules;

import net.example.infiniteaura.TornadoClientSettings;
import net.example.infiniteaura.client.Module;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public class AutoTotem extends Module {
    private final TornadoClientSettings settings = TornadoClientSettings.INSTANCE;

    @Override
    public void onTick() {
        if (!settings.enabled || !settings.autoTotem || mc.player == null) return;

        // Offhand
        if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            int slot = findTotemSlot();
            if (slot != -1) moveTotem(slot, 45);
        }

        // Dual Totems
        if (settings.dualTotems) {
            if (mc.player.getMainHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                int slot = findTotemSlot();
                if (slot != -1) {
                    if (slot < 9) mc.player.getInventory().selectedSlot = slot;
                    else mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
                }
            }
        }
    }

    private int findTotemSlot() {
        for (int i = 0; i < 36; i++) { 
             if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) return i;
        }
        return -1;
    }

    private void moveTotem(int fromSlot, int toSlot) {
        int containerSlot = fromSlot < 9 ? fromSlot + 36 : fromSlot;
        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, containerSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, toSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, containerSlot, 0, SlotActionType.PICKUP, mc.player);
    }
}