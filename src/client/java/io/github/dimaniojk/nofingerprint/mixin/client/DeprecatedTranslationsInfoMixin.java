package io.github.dimaniojk.nofingerprint.mixin.client;

//? if >=1.21.4 {
import io.github.dimaniojk.nofingerprint.NoFingerprint;
import io.github.dimaniojk.nofingerprint.tracking.ModRegistry;
import net.minecraft.locale.DeprecatedTranslationsInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

/**
 * Tracks deprecated translation key lifecycle so NoFingerprint's vanilla key set
 * stays in sync with what actually resolves in ClientLanguage.storage.
 */
@Mixin(DeprecatedTranslationsInfo.class)
public abstract class DeprecatedTranslationsInfoMixin {

    @Shadow public abstract List<String> removed();
    @Shadow public abstract Map<String, String> renamed();

    @Inject(method = "applyToMap", at = @At("HEAD"))
    private void nofingerprint$syncVanillaKeys(Map<String, String> translations, CallbackInfo ci) {
        for (String key : removed()) {
            ModRegistry.removeVanillaTranslationKey(key);
        }

        renamed().forEach((fromKey, toKey) -> {
            ModRegistry.removeVanillaTranslationKey(fromKey);
            ModRegistry.recordVanillaTranslationKey(toKey);
        });

        NoFingerprint.LOGGER.debug("[NoFingerprint] Synced deprecated translations: {} removed, {} renamed",
            removed().size(), renamed().size());
    }
}
//?} else {
/*
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.locale.Language;

@Mixin(Language.class)
public class DeprecatedTranslationsInfoMixin {
}
*///?}
