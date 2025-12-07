package net.example.infiniteaura.modules;

import net.example.infiniteaura.TornadoClientSettings;
import net.example.infiniteaura.client.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class CartPVP extends Module {
    private final TornadoClientSettings settings = TornadoClientSettings.INSTANCE;
    
    private int stage = 0;
    private int delayTimer = 0;

    @Override
    public void onTick() {
        // This module doesn't run passively. It waits for the "activate" signal (keybind).
        // However, if stage > 0, it means the macro is running.
        if (stage == 0) return;

        if (delayTimer > 0) {
            delayTimer--;
            return;
        }

        performStage();
    }

    // Called when you press the Keybind
    public void activate() {
        if (!settings.enabled || !settings.cartPvp) return;
        stage = 1;
        delayTimer = 0;
    }

    private void performStage() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            reset();
            return;
        }

        int railSlot = findSlot(Items.RAIL);
        int cartSlot = findSlot(Items.TNT_MINECART);
        int bowSlot = findSlot(Items.BOW);

        if (railSlot == -1 || cartSlot == -1 || bowSlot == -1) {
            mc.player.sendMessage(net.minecraft.text.Text.of("§cMissing Items for Cart PVP!"), true);
            reset();
            return;
        }

        BlockPos placePos = mc.player.getBlockPos(); // Place at feet for suicide/trap, or look pos?
        // For legit PVP, usually you look at a block and press the macro.
        // Let's assume we place on the block we are looking at, or feet if looking at air.
        if (mc.crosshairTarget instanceof BlockHitResult bhr) {
             placePos = bhr.getBlockPos().up();
        }

        switch (stage) {
            case 1: // Place Rail
                mc.player.getInventory().selectedSlot = railSlot;
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(placePos.down()), Direction.UP, placePos.down(), false));
                stage++;
                delayTimer = 1; // 1 tick delay for server to register rail
                break;

            case 2: // Place Cart
                mc.player.getInventory().selectedSlot = cartSlot;
                // Aim at the rail we just placed
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(placePos), Direction.UP, placePos, false));
                stage++;
                delayTimer = 1;
                break;

            case 3: // Draw Bow
                mc.player.getInventory().selectedSlot = bowSlot;
                
                // Find the cart we just placed (or any TNT cart nearby)
                Entity targetCart = null;
                for (Entity e : mc.world.getEntitiesByClass(TntMinecartEntity.class, mc.player.getBoundingBox().expand(5), e -> true)) {
                    targetCart = e;
                    break;
                }

                if (targetCart != null) {
                    lookAt(targetCart);
                    mc.options.useKey.setPressed(true); // Start charging
                    
                    // Calculate charge needed. Close range = short charge.
                    // 3-5 ticks is enough to ignite a TNT cart usually.
                    stage++;
                    delayTimer = 4; 
                } else {
                    // Cart didn't spawn yet? Wait 1 more tick
                    // Or abort
                    delayTimer = 1;
                }
                break;

            case 4: // Release
                mc.options.useKey.setPressed(false);
                mc.interactionManager.stopUsingItem(mc.player);
                reset(); // Done
                break;
        }
    }

    private void reset() {
        stage = 0;
        delayTimer = 0;
        mc.options.useKey.setPressed(false);
    }

    private void lookAt(Entity target) {
        Vec3d targetCenter = target.getBoundingBox().getCenter();
        double dX = targetCenter.x - mc.player.getX();
        double dY = targetCenter.y - mc.player.getEyeY();
        double dZ = targetCenter.z - mc.player.getZ();
        double dist = Math.sqrt(dX * dX + dZ * dZ);

        float yaw = (float) (Math.atan2(dZ, dX) * 57.29577951308232) - 90.0F;
        float pitch = (float) (-(Math.atan2(dY, dist) * 57.29577951308232));

        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }

    private int findSlot(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) return i;
        }
        return -1;
    }
}