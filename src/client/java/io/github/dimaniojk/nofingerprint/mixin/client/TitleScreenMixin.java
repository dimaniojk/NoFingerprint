package io.github.dimaniojk.nofingerprint.mixin.client;

import io.github.dimaniojk.nofingerprint.config.JarIntegrityChecker;
import io.github.dimaniojk.nofingerprint.config.TamperWarningScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Polls the jar integrity check on the title screen and shows the tamper
 * warning as soon as the async check completes with a mismatch.
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    @Unique private boolean nofingerprint$integrityHandled = false;

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void nofingerprint$pollIntegrityCheck(CallbackInfo ci) {
        if (nofingerprint$integrityHandled || !JarIntegrityChecker.isCheckComplete()) {
            return;
        }
        nofingerprint$integrityHandled = true;
        if (this.minecraft != null && JarIntegrityChecker.isTamperDetected()) {
            JarIntegrityChecker.markShown();
            //? if >=26.2 {
            /*this.minecraft.setScreenAndShow(new TamperWarningScreen(this));*/
            //?} else {
            this.minecraft.setScreen(new TamperWarningScreen(this));
            //?}
        }
    }
}
