package com.dayssky.mma.mixin;

import com.dayssky.mma.events.ClientJoinServerEvent;
import com.dayssky.mma.events.ClientReceiveSystemChatEvent;
import com.dayssky.mma.events.ClientReceiveTabListCustomizationEvent;
import com.dayssky.mma.events.ClientRespawnEvent;
import com.dayssky.mma.events.ClientSetTitleEvent;
import com.dayssky.mma.events.EventResult;
import com.dayssky.mma.util.SafeExceptionLogger;
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
    private static final SafeExceptionLogger mma$EH = new SafeExceptionLogger("PacketEvents");

    @Inject(
            method = {"handleRespawn"},
            at = {@At("HEAD")}
    )
    private void onRecvRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        mma$EH.runSafely(() -> ((ClientRespawnEvent) ClientRespawnEvent.EVENT.invoker()).onRespawn(), () -> "packet=" + packet.toString());
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
        mma$EH.runSafely(() -> {
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
        mma$EH.runSafely(
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
        mma$EH.runSafely(() -> {
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
        mma$EH.runSafely(() -> {
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
        mma$EH.runSafely(() -> {
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
        mma$EH.runSafely(() -> ((ClientJoinServerEvent) ClientJoinServerEvent.EVENT.invoker()).onJoin(), () -> "packet=" + packet.toString());
    }
}
