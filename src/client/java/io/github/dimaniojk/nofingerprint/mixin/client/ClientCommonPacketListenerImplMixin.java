package io.github.dimaniojk.nofingerprint.mixin.client;

//? if >=1.20.3 {
import io.github.dimaniojk.nofingerprint.PrivacyLogger;
import io.github.dimaniojk.nofingerprint.detection.TrackPackDetector;
import io.github.dimaniojk.nofingerprint.lang.NoFingerprintLang;
import io.github.dimaniojk.nofingerprint.lang.NoFingerprintStrings;
import io.github.dimaniojk.nofingerprint.protection.PackStripHandler;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Multi-pack era (1.20.3+): drives TrackPack detection and pack-strip state from
// server pack push/pop. Stripping itself happens in DownloadedPackSourceMixin.
@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonPacketListenerImplMixin {

    @Inject(method = "handleResourcePackPush", at = @At("HEAD"))
    private void onResourcePackPush(ClientboundResourcePackPushPacket packet, CallbackInfo ci) {
        String url = packet.url();

        TrackPackDetector.recordRequest(url, packet.hash());

        if (TrackPackDetector.isFingerprinting() && TrackPackDetector.consumeNotifyPatternOnce()) {
            PrivacyLogger.alert(PrivacyLogger.AlertType.DANGER,
                NoFingerprintLang.tr(NoFingerprintStrings.ALERT_TRACKPACK_PATTERN));
            PrivacyLogger.toast(PrivacyLogger.AlertType.DANGER,
                NoFingerprintLang.tr(NoFingerprintStrings.TOAST_TRACKPACK));
        }

        PackStripHandler.onPackPush(packet.id(), url, packet.required());
    }

    @Inject(method = "handleResourcePackPop", at = @At("HEAD"))
    private void nofingerprint$onResourcePackPop(ClientboundResourcePackPopPacket packet, CallbackInfo ci) {
        PackStripHandler.onPop(packet.id());
    }
}
//?} elif >=1.20.2 {
/*
import io.github.dimaniojk.nofingerprint.PrivacyLogger;
import io.github.dimaniojk.nofingerprint.detection.TrackPackDetector;
import io.github.dimaniojk.nofingerprint.lang.NoFingerprintLang;
import io.github.dimaniojk.nofingerprint.lang.NoFingerprintStrings;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundResourcePackPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 1.20.2 is a hybrid version: it gained the config-phase split (so the resource-pack
// handler moved from ClientPacketListener to ClientCommonPacketListenerImpl and the
// packet moved from .game to .common) but it's still the single-pack era — multi-pack
// stacking with ClientboundResourcePackPush/PopPacket arrived in 1.20.3.
//
// Pack-strip on 1.20.2 is wired via LegacyDownloadedPackSourceMixin (DownloadedPackSource
// is identical to 1.20.1's shape). This mixin only handles TrackPack fingerprint
// detection — PackStripHandler.onPackPush is intentionally not called because the legacy
// packet carries no per-pack UUID, matching the 1.20.1 path in ClientPacketListenerMixin.
@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonPacketListenerImplMixin {

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
    }
}
*///?} else {
/*
import net.minecraft.network.protocol.PacketUtils;
import org.spongepowered.asm.mixin.Mixin;

// 1.20.1: pack push comes through legacy ClientboundResourcePackPacket on the
// game listener — see nofingerprint$onLegacyResourcePack in ClientPacketListenerMixin.
// ClientCommonPacketListenerImpl doesn't exist yet (introduced 1.20.2 with config phase).
@Mixin(PacketUtils.class)
public abstract class ClientCommonPacketListenerImplMixin {
}
*///?}
