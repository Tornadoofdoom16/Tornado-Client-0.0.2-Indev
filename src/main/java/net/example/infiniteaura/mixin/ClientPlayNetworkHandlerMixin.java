package net.example.infiniteaura.mixin;

import net.example.infiniteaura.client.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.chat.Text;
import net.minecraft.text.LiteralText;
import net.example.infiniteaura.client.ModuleManager;
import net.example.infiniteaura.fairplay.FairPlaySignal;
import net.example.infiniteaura.fairplay.FairPlayInstrumentation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    
    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    private void onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        Entity entity = packet.getEntity(mc.world);
        // Status 35 = Totem Activation
        if (packet.getStatus() == 35) {
            if (entity == mc.player) {
                // New: Talk to the Module Manager
                ModuleManager.INSTANCE.infiniteAura.triggerRevenge();
            }
        }
    }

    @Inject(method = "onGameMessage", at = @At("HEAD"))
    private void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        handleIncomingChat(packet.getMessage().getString(), packet.getSender() == null ? "" : packet.getSender().getName());
    }

    @Inject(method = "onChatMessage", at = @At("HEAD"))
    private void onChatMessage(ChatMessageS2CPacket packet, CallbackInfo ci) {
        // ChatMessageS2CPacket API differs by mappings; try to extract text
        try {
            String msg = packet.getContent().getString();
            handleIncomingChat(msg, packet.getSender() == null ? "" : packet.getSender().getName());
        } catch (Exception e) {
            // Fallback: toString
            handleIncomingChat(packet.toString(), "");
        }
    }

    private void handleIncomingChat(String message, String senderName) {
        if (message == null) return;

        // Forward generic chat messages to existing modules that expect them
        ModuleManager.INSTANCE.baritoneBot.onChatMessage(message, senderName);
        ModuleManager.INSTANCE.pearlBot.onChatMessage(message);

        // Parse FairPlay payloads: messages starting with "[FairPlay]"
        if (!message.startsWith("[FairPlay]")) return;
        String payload = message.substring("[FairPlay]".length()).trim();
        // Minimal JSON-like parsing for fields we expect: t (type), w (window), n (nonce)
        String actionId = null;
        long window = 0;
        String nonce = null;
        String senderHash = null;
        try {
            int ti = payload.indexOf("\"t\":");
            if (ti >= 0) {
                int start = payload.indexOf('"', ti + 4) + 1;
                int end = payload.indexOf('"', start);
                actionId = payload.substring(start, end);
            }
            int wi = payload.indexOf("\"w\":");
            if (wi >= 0) {
                int start = wi + 4;
                int end = start;
                while (end < payload.length() && Character.isDigit(payload.charAt(end))) end++;
                window = Long.parseLong(payload.substring(start, end));
            }
            int ni = payload.indexOf("\"n\":");
            if (ni >= 0) {
                int start = payload.indexOf('"', ni + 4) + 1;
                int end = payload.indexOf('"', start);
                nonce = payload.substring(start, end);
            }
            int si = payload.indexOf("\"s\":");
            if (si >= 0) {
                int start = payload.indexOf('"', si + 4) + 1;
                int end = payload.indexOf('"', start);
                senderHash = payload.substring(start, end);
            }
        } catch (Exception ignored) {}

        if (actionId == null || nonce == null) return;

        FairPlaySignal.ActionType actionType = FairPlaySignal.ActionType.fromId(actionId);
        // Prefer explicit senderHash if provided, otherwise fall back to senderName
        String idForSender = (senderHash != null && !senderHash.isEmpty()) ? senderHash : senderName;
        FairPlaySignal signal = new FairPlaySignal(actionType, window, true, "1.0.0", idForSender, nonce, System.currentTimeMillis());

        // Deliver to the FairPlayModule and record reception instrumentation
        try {
            long recvTs = System.currentTimeMillis();
            boolean accepted = false;
            if (ModuleManager.INSTANCE.fairPlayModule != null) {
                accepted = ModuleManager.INSTANCE.fairPlayModule.onSignalReceived(signal);
            }
            FairPlayInstrumentation.getInstance().recordReception(signal.nonce, recvTs, accepted);
        } catch (Exception e) {
            System.out.println("[FairPlay] Failed to handle incoming FairPlay chat payload: " + e.getMessage());
        }
    }
}