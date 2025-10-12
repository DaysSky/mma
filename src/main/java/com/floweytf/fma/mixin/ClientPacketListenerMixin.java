package com.floweytf.fma.mixin;

import com.floweytf.fma.events.ClientJoinServerEvent;
import com.floweytf.fma.events.ClientReceiveSystemChatEvent;
import com.floweytf.fma.events.ClientReceiveTabListCustomizationEvent;
import com.floweytf.fma.events.ClientRespawnEvent;
import com.floweytf.fma.events.ClientSetTitleEvent;
import com.floweytf.fma.events.EventResult;
import com.floweytf.fma.util.SafeExceptionLogger;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientPacketListener.class})
public class ClientPacketListenerMixin {
    @Unique
    private static final SafeExceptionLogger fma$EH = new SafeExceptionLogger("PacketEvents");

    @Inject(
            method = {"handleRespawn"},
            at = {@At("HEAD")}
    )
    private void onRecvRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        fma$EH.runSafely(() -> ((ClientRespawnEvent) ClientRespawnEvent.EVENT.invoker()).onRespawn(), () -> "packet=" + packet.toString());
    }

    @Inject(
            method = {"setTitleText"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Gui;setTitle(Lnet/minecraft/network/chat/Component;)V"
            )},
            cancellable = true
    )
    private void onRecvTitle(ClientboundSetTitleTextPacket packet, CallbackInfo ci) {
        fma$EH.runSafely(() -> {
            if (((ClientSetTitleEvent) ClientSetTitleEvent.TITLE.invoker()).onSetTitle(packet.getText()) != EventResult.CONTINUE) {
                ci.cancel();
            }
        }, () -> "packet=" + packet.toString());
    }

    @Inject(
            method = {"handleTabListCustomisation"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/util/thread/BlockableEventLoop;)V",
                    shift = Shift.AFTER
            )},
            cancellable = true
    )
    private void onSetTabCustomization(ClientboundTabListPacket packet, CallbackInfo ci) {
        fma$EH.runSafely(
                () -> {
                    if (((ClientReceiveTabListCustomizationEvent) ClientReceiveTabListCustomizationEvent.EVENT.invoker())
                            .onEvent(
                                    packet.getHeader().getString().isEmpty() ? null : packet.getHeader(), packet.getFooter().getString().isEmpty() ? null : packet.getFooter()
                            )
                            != EventResult.CONTINUE) {
                        ci.cancel();
                    }
                },
                () -> "packet=" + packet.toString()
        );
    }

    @Inject(
            method = {"setSubtitleText"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Gui;setSubtitle(Lnet/minecraft/network/chat/Component;)V"
            )},
            cancellable = true
    )
    private void onRecvSubtitle(ClientboundSetSubtitleTextPacket packet, CallbackInfo ci) {
        fma$EH.runSafely(() -> {
            if (((ClientSetTitleEvent) ClientSetTitleEvent.SUBTITLE.invoker()).onSetTitle(packet.getText()) != EventResult.CONTINUE) {
                ci.cancel();
            }
        }, () -> "packet=" + packet.toString());
    }

    @Inject(
            method = {"setActionBarText"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Gui;setOverlayMessage(Lnet/minecraft/network/chat/Component;Z)V"
            )},
            cancellable = true
    )
    private void onRecvActionBarText(ClientboundSetActionBarTextPacket packet, CallbackInfo ci) {
        fma$EH.runSafely(() -> {
            if (((ClientSetTitleEvent) ClientSetTitleEvent.ACTIONBAR.invoker()).onSetTitle(packet.getText()) != EventResult.CONTINUE) {
                ci.cancel();
            }
        }, () -> "packet=" + packet.toString());
    }

    @Inject(
            method = {"handleSystemChat"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/chat/ChatListener;handleSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"
            )},
            cancellable = true
    )
    private void onRecvChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        fma$EH.runSafely(() -> {
            if (((ClientReceiveSystemChatEvent) ClientReceiveSystemChatEvent.EVENT.invoker()).onMessage(packet.content()) != EventResult.CONTINUE) {
                ci.cancel();
            }
        }, () -> "packet=" + packet.toString());
    }

    @Inject(
            method = {"handleLogin"},
            at = {@At("TAIL")}
    )
    private void onJoinServer(ClientboundLoginPacket packet, CallbackInfo ci) {
        fma$EH.runSafely(() -> ((ClientJoinServerEvent) ClientJoinServerEvent.EVENT.invoker()).onJoin(), () -> "packet=" + packet.toString());
    }
}
