package com.floweytf.fma.mixin;

import com.floweytf.fma.FMAClient;
import com.floweytf.fma.events.*;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(
        method = "handleRespawn",
        at = @At(
            value = "HEAD"
        )
    )
    private void onRecvRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        ClientRespawnEvent.EVENT.invoker().onRespawn();
    }

    @Inject(
        method = "setTitleText",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;setTitle(Lnet/minecraft/network/chat/Component;)V"
        ),
        cancellable = true
    )
    private void onRecvTitle(ClientboundSetTitleTextPacket packet, CallbackInfo ci) {
        FMAClient.LOGGER.debug("Recv title: {}", packet.getText());
        if (ClientSetTitleEvent.TITLE.invoker().onSetTitle(packet.getText()) != EventResult.CONTINUE) {
            ci.cancel();
        }
    }

    @Inject(
        method = "handleTabListCustomisation",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread" +
                "(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;" +
                "Lnet/minecraft/util/thread/BlockableEventLoop;)V",
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    private void onSetTabCustomization(ClientboundTabListPacket packet, CallbackInfo ci) {
        if (ClientReceiveTabListCustomizationEvent.EVENT.invoker().onEvent(
            packet.getHeader().getString().isEmpty() ? null : packet.getHeader(),
            packet.getFooter().getString().isEmpty() ? null : packet.getFooter()
        ) != EventResult.CONTINUE) {
            ci.cancel();
        }
    }

    @Inject(
        method = "setSubtitleText",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;setSubtitle(Lnet/minecraft/network/chat/Component;)V"
        ),
        cancellable = true
    )
    private void onRecvSubtitle(ClientboundSetSubtitleTextPacket packet, CallbackInfo ci) {
        FMAClient.LOGGER.debug("Recv subtitle: {}", packet.getText());
        if (ClientSetTitleEvent.SUBTITLE.invoker().onSetTitle(packet.getText()) != EventResult.CONTINUE) {
            ci.cancel();
        }
    }

    @Inject(
        method = "setActionBarText",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;setOverlayMessage(Lnet/minecraft/network/chat/Component;Z)V"
        ),
        cancellable = true
    )
    private void onRecvActionBarText(ClientboundSetActionBarTextPacket packet, CallbackInfo ci) {
        FMAClient.LOGGER.debug("Recv action bar: {}", packet.getText());
        if (ClientSetTitleEvent.ACTIONBAR.invoker().onSetTitle(packet.getText()) != EventResult.CONTINUE) {
            ci.cancel();
        }
    }

    @Inject(
        method = "handleSystemChat",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/chat/ChatListener;handleSystemMessage" +
                "(Lnet/minecraft/network/chat/Component;Z)V"
        ),
        cancellable = true
    )
    private void onRecvChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        FMAClient.LOGGER.debug("Recv chat: {}", packet.content());
        if (ClientReceiveSystemChatEvent.EVENT.invoker().onMessage(packet.content()) != EventResult.CONTINUE) {
            ci.cancel();
        }
    }

    @Inject(
        method = "handleLogin",
        at = @At("TAIL")
    )
    private void onJoinServer(ClientboundLoginPacket packet, CallbackInfo ci) {
        ClientJoinServerEvent.EVENT.invoker().onJoin();
    }
}
