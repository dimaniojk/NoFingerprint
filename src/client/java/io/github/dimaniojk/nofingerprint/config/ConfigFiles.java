package io.github.dimaniojk.nofingerprint.config;

import io.github.dimaniojk.nofingerprint.NoFingerprint;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves NoFingerprint config filenames.
 *
 * <p>Upstream OpSec stored {@code config/opsec.json} and
 * {@code config/opsec-accounts.json}. Those names are kept only as a one-time
 * migration source so users moving from OpSec do not lose settings or saved
 * accounts. New writes always use the NoFingerprint filenames.
 */
public final class ConfigFiles {
    public static final String CONFIG_NAME = "nofingerprint.json";
    public static final String ACCOUNTS_NAME = "nofingerprint-accounts.json";
    public static final String LEGACY_CONFIG_NAME = "opsec.json";
    public static final String LEGACY_ACCOUNTS_NAME = "opsec-accounts.json";

    private ConfigFiles() {}

    public static Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static Path currentConfig() {
        return configDir().resolve(CONFIG_NAME);
    }

    public static Path currentAccounts() {
        return configDir().resolve(ACCOUNTS_NAME);
    }

    /**
     * Prefer the current filename. If it is missing, copy the upstream OpSec
     * file once and then use the new path.
     */
    public static Path resolveAndMigrate(Path current, String legacyName, String label) {
        if (Files.exists(current)) {
            return current;
        }
        Path legacy = configDir().resolve(legacyName);
        if (!Files.exists(legacy)) {
            return current;
        }
        try {
            Files.copy(legacy, current);
            NoFingerprint.LOGGER.info(
                "[NoFingerprint] Migrated {} from upstream OpSec file {}",
                label,
                legacy.getFileName()
            );
            return current;
        } catch (IOException e) {
            NoFingerprint.LOGGER.warn(
                "[NoFingerprint] Could not copy {} from {}; reading the legacy file this session: {}",
                label,
                legacy.getFileName(),
                e.getMessage()
            );
            return legacy;
        }
    }

    /**
     * Mixin-time lookup that must not write files. Prefers the new config
     * name, then the upstream OpSec name if the user has not launched the
     * renamed mod yet.
     */
    public static Path existingConfigForEarlyRead() {
        Path current = currentConfig();
        if (Files.exists(current)) {
            return current;
        }
        Path legacy = configDir().resolve(LEGACY_CONFIG_NAME);
        if (Files.exists(legacy)) {
            return legacy;
        }
        return current;
    }
}
