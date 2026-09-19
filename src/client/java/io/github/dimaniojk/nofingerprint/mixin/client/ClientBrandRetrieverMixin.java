package io.github.dimaniojk.nofingerprint.mixin.client;

import io.github.dimaniojk.nofingerprint.NoFingerprint;
import io.github.dimaniojk.nofingerprint.config.NoFingerprintConfig;
import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Spoofs the client brand at its source.
 */
@Mixin(ClientBrandRetriever.class)
public class ClientBrandRetrieverMixin {
    
    @Unique
    private static final AtomicBoolean nofingerprint$logged = new AtomicBoolean(false);
    
    @Inject(method = "getClientModName", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onGetClientModName(CallbackInfoReturnable<String> cir) {
        NoFingerprintConfig config = NoFingerprintConfig.getInstance();
        
        if (config.shouldSpoofBrand()) {
            String spoofedBrand = config.getSettings().getEffectiveBrand();
            
            if (nofingerprint$logged.compareAndSet(false, true)) {
                NoFingerprint.LOGGER.debug("[NoFingerprint] ClientBrandRetriever active - spoofing brand as: {}", spoofedBrand);
            }
            
            cir.setReturnValue(spoofedBrand);
        }
    }
}

