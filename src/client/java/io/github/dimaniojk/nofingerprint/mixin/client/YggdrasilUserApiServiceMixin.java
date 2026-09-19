package io.github.dimaniojk.nofingerprint.mixin.client;

import io.github.dimaniojk.nofingerprint.NoFingerprint;
import io.github.dimaniojk.nofingerprint.config.NoFingerprintConfig;
import com.mojang.authlib.minecraft.TelemetrySession;
import com.mojang.authlib.yggdrasil.YggdrasilUserApiService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.Executor;

/**
 * Mixin to disable telemetry session creation.
 * 
 * This prevents the creation of telemetry sessions that would send data to Mojang.
 * By returning TelemetrySession.DISABLED, no telemetry data is collected or sent.
 * 
 * Based on No Chat Reports by Aizistral-Studios:
 * https://github.com/Aizistral-Studios/No-Chat-Reports
 */
@Mixin(value = YggdrasilUserApiService.class, remap = false)
public class YggdrasilUserApiServiceMixin {
    
    /**
     * Disable telemetry session creation by returning a disabled session.
     */
    @Inject(method = "newTelemetrySession", at = @At("HEAD"), cancellable = true)
    private void nofingerprint$disableTelemetrySession(Executor executor, CallbackInfoReturnable<TelemetrySession> info) {
        if (NoFingerprintConfig.getInstance().shouldDisableTelemetry()) {
            NoFingerprint.LOGGER.debug("[NoFingerprint] Returning disabled TelemetrySession");
            info.setReturnValue(TelemetrySession.DISABLED);
        }
    }
}

