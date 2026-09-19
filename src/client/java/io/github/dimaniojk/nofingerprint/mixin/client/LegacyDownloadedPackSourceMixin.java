package io.github.dimaniojk.nofingerprint.mixin.client;

//? if <1.20.2 {
/*
import io.github.dimaniojk.nofingerprint.NoFingerprint;
import io.github.dimaniojk.nofingerprint.config.NoFingerprintConfig;
import io.github.dimaniojk.nofingerprint.protection.LangOnlyPackResources;
import io.github.dimaniojk.nofingerprint.protection.PackStripHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.resources.DownloadedPackSource;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.io.File;
import java.util.UUID;

// 1.20.1: pre-multi-pack era. DownloadedPackSource lives in client.resources
// (not client.resources.server). Pack.ResourcesSupplier is a SAM with a single
// open(String) method — lambda-friendly.
@Mixin(DownloadedPackSource.class)
public abstract class LegacyDownloadedPackSourceMixin {

    @ModifyExpressionValue(
        method = "downloadAndSelectResourcePack",
        at = @At(value = "NEW", target = "(Ljava/io/File;Ljava/lang/String;)Ljava/io/File;"))
    private File nofingerprint$perAccountPackFile(File original) {
        if (!NoFingerprintConfig.getInstance().shouldIsolatePackCache()) return original;
        String accountUuid = nofingerprint$activeAccountUuid();
        if (accountUuid == null) return original;
        File parent = original.getParentFile();
        if (parent == null) return original;
        File perAccountDir = new File(parent, accountUuid);
        if (!perAccountDir.exists() && !perAccountDir.mkdirs()) {
            NoFingerprint.LOGGER.debug("[NoFingerprint] Failed to create per-account pack cache dir {}", perAccountDir);
            return original;
        }
        return new File(perAccountDir, original.getName());
    }

    @ModifyVariable(
        method = "setServerPack",
        at = @At(value = "STORE"),
        ordinal = 0)
    private Pack.ResourcesSupplier nofingerprint$wrapServerSupplier(Pack.ResourcesSupplier original) {
        if (!NoFingerprintConfig.getInstance().shouldWrapServerPacks()) return original;
        if (!PackStripHandler.isWrapped(PackStripHandler.LEGACY_SERVER_PACK_UUID)) return original;
        return name -> new LangOnlyPackResources(
            original.open(name), PackStripHandler.LEGACY_SERVER_PACK_UUID);
    }

    private static String nofingerprint$activeAccountUuid() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return null;
            User user = mc.getUser();
            if (user == null) return null;
            UUID uuid = user.getProfileId();
            return uuid == null ? null : uuid.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
*///?} elif <1.20.3 {
/*
import io.github.dimaniojk.nofingerprint.NoFingerprint;
import io.github.dimaniojk.nofingerprint.config.NoFingerprintConfig;
import io.github.dimaniojk.nofingerprint.protection.LangOnlyPackResources;
import io.github.dimaniojk.nofingerprint.protection.PackStripHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.resources.DownloadedPackSource;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.io.File;
import java.util.UUID;

// 1.20.2: same pack source class shape as 1.20.1 (still client.resources,
// downloadAndSelectResourcePack + setServerPack with new File(File, String) inline),
// but Pack.ResourcesSupplier already gained the multi-pack 2-method shape:
//     openPrimary(String) + openFull(String, Pack.Info)
// So the cache mixin is identical, but the supplier wrap uses an anonymous class.
@Mixin(DownloadedPackSource.class)
public abstract class LegacyDownloadedPackSourceMixin {

    @ModifyExpressionValue(
        method = "downloadAndSelectResourcePack",
        at = @At(value = "NEW", target = "(Ljava/io/File;Ljava/lang/String;)Ljava/io/File;"))
    private File nofingerprint$perAccountPackFile(File original) {
        if (!NoFingerprintConfig.getInstance().shouldIsolatePackCache()) return original;
        String accountUuid = nofingerprint$activeAccountUuid();
        if (accountUuid == null) return original;
        File parent = original.getParentFile();
        if (parent == null) return original;
        File perAccountDir = new File(parent, accountUuid);
        if (!perAccountDir.exists() && !perAccountDir.mkdirs()) {
            NoFingerprint.LOGGER.debug("[NoFingerprint] Failed to create per-account pack cache dir {}", perAccountDir);
            return original;
        }
        return new File(perAccountDir, original.getName());
    }

    @ModifyVariable(
        method = "setServerPack",
        at = @At(value = "STORE"),
        ordinal = 0)
    private Pack.ResourcesSupplier nofingerprint$wrapServerSupplier(Pack.ResourcesSupplier original) {
        if (!NoFingerprintConfig.getInstance().shouldWrapServerPacks()) return original;
        if (!PackStripHandler.isWrapped(PackStripHandler.LEGACY_SERVER_PACK_UUID)) return original;
        return new Pack.ResourcesSupplier() {
            @Override
            public PackResources openPrimary(String id) {
                return new LangOnlyPackResources(original.openPrimary(id), PackStripHandler.LEGACY_SERVER_PACK_UUID);
            }

            @Override
            public PackResources openFull(String id, Pack.Info info) {
                return new LangOnlyPackResources(original.openFull(id, info), PackStripHandler.LEGACY_SERVER_PACK_UUID);
            }
        };
    }

    private static String nofingerprint$activeAccountUuid() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return null;
            User user = mc.getUser();
            if (user == null) return null;
            UUID uuid = user.getProfileId();
            return uuid == null ? null : uuid.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
*///?} else {
import net.minecraft.network.protocol.PacketUtils;
import org.spongepowered.asm.mixin.Mixin;

// 1.20.3+: per-account cache isolation is handled by DownloadQueueMixin on the
// multi-pack code path, and lang-strip is handled by DownloadedPackSourceMixin's
// loadRequestedPacks wrap. Empty stub on a stable always-present class so the
// mixins.json entry resolves cleanly on every build target.
@Mixin(PacketUtils.class)
public abstract class LegacyDownloadedPackSourceMixin {
}
//?}
