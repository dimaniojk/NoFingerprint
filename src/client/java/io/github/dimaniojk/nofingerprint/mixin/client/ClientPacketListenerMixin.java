package io.github.dimaniojk.nofingerprint.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.dimaniojk.nofingerprint.NoFingerprint;
import io.github.dimaniojk.nofingerprint.PrivacyLogger;
import io.github.dimaniojk.nofingerprint.config.NoFingerprintConfig;
import io.github.dimaniojk.nofingerprint.detection.PacketContext;
import io.github.dimaniojk.nofingerprint.protection.PackStripHandler;
import io.github.dimaniojk.nofingerprint.protection.PackStripOverlay;
import io.github.dimaniojk.nofingerprint.protection.TranslationProtectionHandler;
import io.github.dimaniojk.nofingerprint.util.LocalAddressUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.PacketListener;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.chat.SignedMessageChain;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
//? if <1.20.2 {
/*import io.github.dimaniojk.nofingerprint.detection.TrackPackDetector;
import io.github.dimaniojk.nofingerprint.lang.NoFingerprintLang;
import io.github.dimaniojk.nofingerprint.lang.NoFingerprintStrings;
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket;
*///?}
//? if <1.20.5 {
/*import net.minecraft.network.protocol.game.ClientboundServerDataPacket;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Tracks server connection events for NoFingerprint.
 *
 * <p>Chat-signing scope here is limited to a single mechanism: the
 * {@code nofingerprint$skipSigningEncoder} {@code @WrapOperation} on the
 * {@code SignedMessageChain.Encoder.pack(...)} call inside {@code sendChat},
 * which prevents signing-chain advancement whenever the user has signing OFF.
 * No server-state-triggered upgrades, no system-chat reactions, no auto-resends
 * — those were removed because they uniformly fingerprinted NoFingerprint users.</p>
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Unique
    private static volatile ScheduledExecutorService nofingerprint$scheduler;

    @Unique
    private static volatile ScheduledFuture<?> nofingerprint$pendingTask;

    @Unique
    private static synchronized ScheduledExecutorService nofingerprint$getScheduler() {
        if (nofingerprint$scheduler == null || nofingerprint$scheduler.isShutdown()) {
            nofingerprint$scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "NoFingerprint-Scheduler");
            t.setDaemon(true);
            return t;
        });
        }
        return nofingerprint$scheduler;
    }

    @Unique
    private static synchronized void nofingerprint$shutdownScheduler() {
        if (nofingerprint$pendingTask != null) {
            nofingerprint$pendingTask.cancel(false);
            nofingerprint$pendingTask = null;
        }
        if (nofingerprint$scheduler != null && !nofingerprint$scheduler.isShutdown()) {
            nofingerprint$scheduler.shutdownNow();
            nofingerprint$scheduler = null;
        }
    }

    /**
     * Wrap sub-packet handle() calls inside handleBundlePacket so each sub-packet
     * gets its own packet name context. Without this, all sub-packets inherit the
     * bundle's name since they bypass PacketProcessor$ListenerAndPacket.
     */
    @WrapOperation(
        method = "handleBundlePacket",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/Packet;handle(Lnet/minecraft/network/PacketListener;)V")
    )
    private <T extends PacketListener> void nofingerprint$wrapBundleSubPacketHandle(
            Packet<T> instance, T listener, Operation<Void> original) {
        TranslationProtectionHandler.clearDedup();
        PacketContext.setPacketName(instance);
        original.call(instance, listener);
    }

    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void onLogin(ClientboundLoginPacket packet, CallbackInfo ci) {
        ClientPacketListener listener = (ClientPacketListener)(Object)this;
        String serverAddress = listener.getConnection().getRemoteAddress().toString();

        if (serverAddress.startsWith("/")) {
            serverAddress = serverAddress.substring(1);
        }
        int colonIndex = serverAddress.lastIndexOf(':');
        if (colonIndex > 0) {
            serverAddress = serverAddress.substring(0, colonIndex);
        }

        NoFingerprintConfig.getInstance().setCurrentServer(serverAddress);
        // Used by pack-URL blocking to skip when the game server itself is local/private.
        LocalAddressUtil.serverAddress = serverAddress;

        // Schedule port scan summary after 2 seconds
        nofingerprint$pendingTask = nofingerprint$getScheduler().schedule(() -> {
            Minecraft.getInstance().execute(PrivacyLogger::showPortScanSummary);
        }, 2, TimeUnit.SECONDS);
    }

    //? if <1.20.2 {
    /*// <1.20.2: single-pack server packs come through ClientboundResourcePackPacket on
    // the game listener; on 1.20.2+ this is ClientCommonPacketListenerImplMixin instead.
    // Drives TrackPack detection and pack-strip state (keyed by LEGACY_SERVER_PACK_UUID).
    @Inject(method = "handleResourcePack", at = @At("HEAD"))
    private void nofingerprint$onLegacyResourcePack(ClientboundResourcePackPacket packet, CallbackInfo ci) {
        String url = packet.getUrl();
        String hash = packet.getHash();

        TrackPackDetector.recordRequest(url, hash);

        if (TrackPackDetector.isFingerprinting() && TrackPackDetector.consumeNotifyPatternOnce()) {
            PrivacyLogger.alert(PrivacyLogger.AlertType.DANGER,
                NoFingerprintLang.tr(NoFingerprintStrings.ALERT_TRACKPACK_PATTERN));
            PrivacyLogger.toast(PrivacyLogger.AlertType.DANGER,
                NoFingerprintLang.tr(NoFingerprintStrings.TOAST_TRACKPACK));
        }

        PackStripHandler.onPackPush(PackStripHandler.LEGACY_SERVER_PACK_UUID, url, packet.isRequired());
    }
    *///?}

    /**
     * Wrap the signing encoder call in sendChat() to prevent chain advancement when not signing.
     * Returns null instead of calling the encoder, keeping the chain clean.
     */
    @WrapOperation(
            method = "sendChat",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/SignedMessageChain$Encoder;pack(Lnet/minecraft/network/chat/SignedMessageBody;)Lnet/minecraft/network/chat/MessageSignature;")
    )
    private MessageSignature nofingerprint$skipSigningEncoder(SignedMessageChain.Encoder encoder, SignedMessageBody body, Operation<MessageSignature> original) {
        if (NoFingerprintConfig.getInstance().shouldNotSign()) {
            NoFingerprint.LOGGER.debug("[NoFingerprint] Skipping message signing (chain preserved)");
            return null;
        }
        return original.call(encoder, body);
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void onClose(CallbackInfo ci) {
        nofingerprint$shutdownScheduler();
        PrivacyLogger.resetPortScanTracking();
        PrivacyLogger.clearCooldowns();
        NoFingerprintConfig.getInstance().setCurrentServer(null);
        LocalAddressUtil.serverAddress = null;
        PackStripHandler.clearAll();
        PackStripOverlay.clearQueue();
    }
}
