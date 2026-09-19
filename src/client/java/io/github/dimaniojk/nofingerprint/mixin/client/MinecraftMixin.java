package io.github.dimaniojk.nofingerprint.mixin.client;

import io.github.dimaniojk.nofingerprint.NoFingerprint;
import io.github.dimaniojk.nofingerprint.config.NoFingerprintConfig;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Core Minecraft client hooks for telemetry blocking.
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "allowsTelemetry", at = @At("HEAD"), cancellable = true)
    private void nofingerprint$disableTelemetry(CallbackInfoReturnable<Boolean> info) {
        if (NoFingerprintConfig.getInstance().shouldDisableTelemetry()) {
            NoFingerprint.LOGGER.debug("[NoFingerprint] Blocking telemetry");
            info.setReturnValue(false);
        }
    }
}
