package net.example.infiniteaura.modules;

import net.example.infiniteaura.TornadoClientSettings;
import net.example.infiniteaura.client.Module;
import net.example.infiniteaura.client.ModuleManager;
import net.example.infiniteaura.fairplay.FairPlaySignal;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class DoorTP extends Module {
    private final TornadoClientSettings settings = TornadoClientSettings.INSTANCE;

    public void onTick() {
        if (previewTicks > 0) {
            previewTicks--;
            if (mc.world != null && previewDest != null) {
                for (int i = 0; i < 8; i++) {
                    double ox = (mc.random.nextDouble() - 0.5) * 0.8;
                    double oy = mc.random.nextDouble() * 0.8;
                    double oz = (mc.random.nextDouble() - 0.5) * 0.8;
                    mc.world.addParticle(ParticleTypes.END_ROD, previewDest.x + ox, previewDest.y + oy, previewDest.z + oz, 0.0, 0.0, 0.0);
                }
            }
            if (previewTicks == 0) performTeleport();
        }
    }

    // Called by keybind in TornadoClient
    public void activate() {
        if (!settings.enabled || !settings.doorTp) return;
        if (mc.player == null || mc.world == null) return;

        if (!(mc.crosshairTarget instanceof BlockHitResult bhr)) return;

        // Start scanning from the block position forward along the vector from player eye to block center
        Vec3d eye = mc.player.getCameraPosVec(mc.getTickDelta());
        Vec3d blockCenter = Vec3d.ofCenter(bhr.getBlockPos());
        Vec3d dir = blockCenter.subtract(eye).normalize();

        int maxSteps = Math.min( settings.doorTpRange, 70 );
        Vec3d probe = blockCenter;
        BlockPos found = null;

        for (int i = 1; i <= maxSteps; i++) {
            probe = probe.add(dir.x, dir.y, dir.z);
            BlockPos ppos = new BlockPos(Math.floor(probe.x), Math.floor(probe.y), Math.floor(probe.z));
            // find first spot with at least two air blocks (head + feet)
            if (mc.world.getBlockState(ppos).isAir() && mc.world.getBlockState(ppos.up()).isAir()) {
                // ensure not inside liquid (basic check)
                if (!mc.world.getBlockState(ppos.down()).getFluidState().isEmpty()) continue;
                found = ppos;
                break;
            }
        }

        if (found == null) return;

        Vec3d dest = Vec3d.ofCenter(found);

        // Emit FairPlay signal for DOOR_TP
        try {
            if (ModuleManager.INSTANCE.fairPlayModule != null && mc.player != null) {
                String senderHash = mc.player.getUuid().toString();
                ModuleManager.INSTANCE.fairPlayModule.announceAction(FairPlaySignal.ActionType.DOOR_TP, 200L, senderHash);
            }
        } catch (Exception ignored) {}

        // Start preview then teleport
        previewDest = dest;
        previewTicks = 10;
        if (mc.player != null) mc.player.sendMessage(Text.of("DoorTP preview: executing in 0.5s"), true);
    }

    private int previewTicks = 0;
    private Vec3d previewDest = null;

    private void performTeleport() {
        if (previewDest == null) return;
        try {
            if (mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(previewDest.x, previewDest.y, previewDest.z, true));
            }
            mc.player.setPosition(previewDest.x, previewDest.y, previewDest.z);
        } catch (Exception e) {
            // ignore
        } finally {
            previewDest = null;
        }
    }
}
