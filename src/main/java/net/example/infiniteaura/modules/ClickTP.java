package net.example.infiniteaura.modules;

import net.example.infiniteaura.TornadoClientSettings;
import net.example.infiniteaura.client.Module;
import net.example.infiniteaura.client.ModuleManager;
import net.example.infiniteaura.fairplay.FairPlaySignal;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class ClickTP extends Module {
    private final TornadoClientSettings settings = TornadoClientSettings.INSTANCE;

    public void onTick() {
        if (previewTicks > 0) {
            previewTicks--;
            // spawn some particles at destination to indicate preview
            if (mc.world != null && previewDest != null) {
                for (int i = 0; i < 8; i++) {
                    double ox = (mc.random.nextDouble() - 0.5) * 0.6;
                    double oy = mc.random.nextDouble() * 0.6;
                    double oz = (mc.random.nextDouble() - 0.5) * 0.6;
                    mc.world.addParticle(ParticleTypes.END_ROD, previewDest.x + ox, previewDest.y + oy, previewDest.z + oz, 0.0, 0.0, 0.0);
                }
            }
            if (previewTicks == 0) {
                performTeleport();
            }
        }
    }

    // Called by keybind in TornadoClient
    private int previewTicks = 0;
    private Vec3d previewDest = null;

    public void activate() {
        if (!settings.enabled || !settings.clickTp) return;
        if (mc.player == null || mc.world == null) return;

        if (!(mc.crosshairTarget instanceof BlockHitResult bhr)) return;

        BlockPos targetBlock = bhr.getBlockPos();
        // Prefer landing on top of the block (center)
        BlockPos landing = targetBlock.up();

        // If landing is not air, try to find first air above up to 3 blocks
        for (int i = 0; i < 6; i++) {
            BlockPos check = targetBlock.up(i + 1);
            if (mc.world.getBlockState(check).isAir()) { landing = check; break; }
        }

        Vec3d dest = Vec3d.ofCenter(landing);

        // Emit FairPlay signal (allow compatible clients to pre-defend)
        try {
            if (ModuleManager.INSTANCE.fairPlayModule != null && mc.player != null) {
                String senderHash = mc.player.getUuid().toString();
                ModuleManager.INSTANCE.fairPlayModule.announceAction(FairPlaySignal.ActionType.CLICK_TP, 150L, senderHash);
            }
        } catch (Exception ignored) {}

        // Start a short preview (10 ticks) before teleporting so user sees destination
        previewDest = dest;
        previewTicks = 10; // half-second preview
        if (mc.player != null) {
            mc.player.sendMessage(Text.of("Teleport preview: executing in 0.5s"), true);
        }
    }

    private void performTeleport() {
        if (previewDest == null) return;
        try {
            if (mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(previewDest.x, previewDest.y, previewDest.z, true));
            }
            mc.player.setPosition(previewDest.x, previewDest.y, previewDest.z);
        } catch (Exception e) {
            // best effort
        } finally {
            previewDest = null;
        }
    }
}
