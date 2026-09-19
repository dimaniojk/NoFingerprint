package io.github.dimaniojk.nofingerprint.mixin.client;

import io.github.dimaniojk.nofingerprint.detection.TrackPackDetector;
import io.github.dimaniojk.nofingerprint.protection.TranslationProtectionHandler;
import net.minecraft.client.gui.screens.ConnectScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Resets detection state when connecting to a new server.
 *
 * <p>Previously this mixin also pre-initialised profile keys for the ON_DEMAND
 * signing mode (so the keys would be ready if the server required secure chat
 * mid-session). That path was removed when ON_DEMAND was deleted — the SIGN
 * mode lets vanilla MC's own key-pair lifecycle handle this naturally.</p>
 */
@Mixin(ConnectScreen.class)
public class ConnectScreenMixin {

    @Inject(method = "startConnecting", at = @At("HEAD"))
    private static void nofingerprint$resetState(CallbackInfo ci) {
        TrackPackDetector.reset();
        TranslationProtectionHandler.clearCache();
    }
}
