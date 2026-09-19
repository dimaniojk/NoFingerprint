package io.github.dimaniojk.nofingerprint.config;

import io.github.dimaniojk.nofingerprint.NoFingerprint;
import io.github.dimaniojk.nofingerprint.debug.ProbeDiagnostics;
import io.github.dimaniojk.nofingerprint.tracking.ModRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Main configuration manager for NoFingerprint mod.
 * Handles loading, saving, and validating configuration settings.
 * Uses thread-safe singleton pattern with double-checked locking.
 */
public class NoFingerprintConfig {

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();
    private static final Path CONFIG_PATH = ConfigFiles.resolveAndMigrate(
        ConfigFiles.currentConfig(),
        ConfigFiles.LEGACY_CONFIG_NAME,
        "settings"
    );

    public static final boolean EXPLOIT_PREVENTER_LOADED =
        FabricLoader.getInstance().isModLoaded("exploitpreventer");

    // Chat signing and telemetry blocking overlap with dedicated mods. When one is
    // present NoFingerprint stands down on the shared feature (greys the control out) rather
    // than double-stripping signatures / double-disabling telemetry. Unlike EP this is
    // per-feature: No Chat Reports only covers signing; No Prying Eyes covers both.
    // ExploitPreventer overlap is NOT a blanket stand-down — see CompatibilityPolicy.
    public static final boolean NO_CHAT_REPORTS_LOADED =
        FabricLoader.getInstance().isModLoaded("nochatreports");
    public static final boolean NO_PRYING_EYES_LOADED =
        FabricLoader.getInstance().isModLoaded("nopryingeyes");

    /** Chat signing is handled by an external mod (No Chat Reports or No Prying Eyes). */
    public static final boolean CHAT_SIGNING_MANAGED_EXTERNALLY =
        NO_CHAT_REPORTS_LOADED || NO_PRYING_EYES_LOADED;

    /** Telemetry blocking is handled by an external mod (No Chat Reports or No Prying Eyes). */
    public static final boolean TELEMETRY_MANAGED_EXTERNALLY =
        NO_CHAT_REPORTS_LOADED || NO_PRYING_EYES_LOADED;

    // Per-MC-version feature gate: DownloadQueue + multi-pack stacking arrived 1.20.3.
    // 1.20.1/1.20.2 isolate the pack cache via LegacyDownloadedPackSourceMixin instead.
    //? if >=1.20.3 {
    public static final boolean MC_VERSION_HAS_MULTI_PACK = true;
    //?} else {
    /*public static final boolean MC_VERSION_HAS_MULTI_PACK = false;
    *///?}

    // Block Local URLs: HttpUtil.method_15303 (1.20.1/1.20.2) and downloadFile (1.20.3+)
    // both invoke HttpURLConnection.getInputStream — verified on the 1.20.1 mapped jar.
    public static final boolean MC_VERSION_HAS_BLOCK_LOCAL_URLS = true;

    private static volatile NoFingerprintConfig INSTANCE;
    private static final Object LOCK = new Object();

    private SpoofSettings settings = new SpoofSettings();
    private volatile String currentServer = null;

    private NoFingerprintConfig() {
        load();
        if (EXPLOIT_PREVENTER_LOADED) {
            NoFingerprint.LOGGER.info(
                "[NoFingerprint] ExploitPreventer detected — standing down only overlapping HTTP URL "
                    + "and translation hooks. Brand, channels, known-packs, pack-strip, shaders, "
                    + "and pack-cache isolation stay under NoFingerprint."
            );
        }
        if (NO_CHAT_REPORTS_LOADED) {
            NoFingerprint.LOGGER.info(
                "[NoFingerprint] No Chat Reports detected - deferring chat signing and telemetry to it."
            );
        }
        if (NO_PRYING_EYES_LOADED) {
            NoFingerprint.LOGGER.info(
                "[NoFingerprint] No Prying Eyes detected - deferring chat signing and telemetry to it."
            );
        }
        logProbeSnapshot();
    }

    /** Dump feature decisions when {@code -Dnofingerprint.debug.probes=true}. */
    public void logProbeSnapshot() {
        if (!ProbeDiagnostics.enabled()) {
            return;
        }
        CompatibilityPolicy.Decisions d = compatibility();
        ProbeDiagnostics.log(
            "mc={} ep={} ncr={} npe={} spoofAsVanilla={} brand={} channels={} knownPacks={} isolateCache={} blockLocalUrls={} translation={} stripShaders={} unsignedChat={} telemetry={}",
            net.fabricmc.loader.api.FabricLoader.getInstance()
                .getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown"),
            EXPLOIT_PREVENTER_LOADED,
            NO_CHAT_REPORTS_LOADED,
            NO_PRYING_EYES_LOADED,
            settings.isSpoofAsVanilla(),
            d.spoofBrand(),
            d.filterChannels(),
            d.filterKnownPacks(),
            d.isolatePackCache(),
            d.blockLocalPackUrls(),
            d.translationProtection(),
            d.stripModShaders(),
            d.applyUnsignedChat(),
            d.blockTelemetry()
        );
    }

    /** Display name of the mod managing chat signing, or {@code null} if NoFingerprint manages it. */
    public static String chatSigningManagerName() {
        if (NO_CHAT_REPORTS_LOADED) return "No Chat Reports";
        if (NO_PRYING_EYES_LOADED) return "No Prying Eyes";
        return null;
    }

    /** Display name of the mod managing telemetry, or {@code null} if NoFingerprint manages it. */
    public static String telemetryManagerName() {
        if (NO_CHAT_REPORTS_LOADED) return "No Chat Reports";
        if (NO_PRYING_EYES_LOADED) return "No Prying Eyes";
        return null;
    }

    public static NoFingerprintConfig getInstance() {
        if (INSTANCE == null) {
            synchronized (LOCK) {
                if (INSTANCE == null) {
                    INSTANCE = new NoFingerprintConfig();
                }
            }
        }
        return INSTANCE;
    }

    public void load() {
        if (!Files.exists(CONFIG_PATH)) {
            NoFingerprint.LOGGER.info("[NoFingerprint] Creating default config");
            save();
            return;
        }

        try {
            String content = Files.readString(CONFIG_PATH);
            if (content == null || content.trim().isEmpty()) {
                NoFingerprint.LOGGER.warn(
                    "[NoFingerprint] Config file is empty, using defaults"
                );
                settings = new SpoofSettings();
                save();
                return;
            }

            JsonObject json = JsonParser.parseString(content).getAsJsonObject();

            if (json.has("settings")) {
                settings = SpoofSettings.fromJson(
                    json.getAsJsonObject("settings")
                );
            } else if (json.has("defaultSettings")) {
                settings = SpoofSettings.fromJson(
                    json.getAsJsonObject("defaultSettings")
                );
            }

            if (!validateAndCorrectSettings(settings)) {
                NoFingerprint.LOGGER.warn(
                    "[NoFingerprint] Config validation failed, resetting to defaults"
                );
                settings = new SpoofSettings();
                save();
                return;
            }

            NoFingerprint.LOGGER.info(
                "[NoFingerprint] Loaded config - spoofAsVanilla: {}",
                settings.isSpoofAsVanilla()
            );
        } catch (IOException e) {
            NoFingerprint.LOGGER.error(
                "[NoFingerprint] Failed to read config file: {}",
                e.getMessage()
            );
            settings = new SpoofSettings();
            save();
        } catch (
            com.google.gson.JsonSyntaxException
            | IllegalStateException e
        ) {
            NoFingerprint.LOGGER.error(
                "[NoFingerprint] Invalid JSON in config file: {}",
                e.getMessage()
            );
            settings = new SpoofSettings();
            save();
        }
    }

    /**
     * Validates and auto-corrects settings if necessary.
     * This method modifies invalid settings to valid defaults.
     *
     * @param settings The settings to validate
     * @return true if settings were valid or have been corrected, false if settings is null
     */
    private boolean validateAndCorrectSettings(SpoofSettings settings) {
        if (settings == null) {
            NoFingerprint.LOGGER.error("[NoFingerprint] Validation failed: settings is null");
            return false;
        }

        boolean modified = false;

        if (settings.getSigningMode() == null) {
            NoFingerprint.LOGGER.warn(
                "[NoFingerprint] Invalid signing mode, resetting to SIGN"
            );
            settings.setSigningMode(SpoofSettings.SigningMode.SIGN);
            modified = true;
        }

        if (settings.getWhitelistMode() == null) {
            NoFingerprint.LOGGER.warn(
                "[NoFingerprint] Invalid whitelist mode, resetting to AUTO"
            );
            settings.setWhitelistMode(SpoofSettings.WhitelistMode.AUTO);
            modified = true;
        }

        // Spoof-as-vanilla is the master mode: it implies "block ALL custom payloads",
        // so an active whitelist (Auto/Custom) would be moot. Force the whitelist to
        // OFF (block all) whenever spoofAsVanilla is on. setSpoofAsVanilla normally
        // handles the swap, but hand-edited configs can still land here inconsistent.
        if (settings.isSpoofAsVanilla() && settings.getWhitelistMode() != SpoofSettings.WhitelistMode.OFF) {
            NoFingerprint.LOGGER.warn(
                "[NoFingerprint] active whitelist incompatible with spoofAsVanilla; forcing whitelist OFF (block all)"
            );
            settings.setWhitelistMode(SpoofSettings.WhitelistMode.OFF);
            modified = true;
        }

        // Block-All is no longer user-selectable: when spoof-vanilla is off, migrate
        // any leftover OFF mode (from older configs that exposed it as a tri-state)
        // back to AUTO so the user isn't stuck in an unreachable state.
        if (!settings.isSpoofAsVanilla() && settings.getWhitelistMode() == SpoofSettings.WhitelistMode.OFF) {
            NoFingerprint.LOGGER.info(
                "[NoFingerprint] Migrating legacy whitelist OFF (block all) to AUTO — option is no longer user-selectable"
            );
            settings.setWhitelistMode(SpoofSettings.WhitelistMode.AUTO);
            modified = true;
        }

        if (modified) {
            save();
        }

        return true;
    }

    public void save() {
        try {
            JsonObject json = new JsonObject();
            json.add("settings", settings.toJson());

            Files.createDirectories(CONFIG_PATH.getParent());

            Path tempFile = CONFIG_PATH.resolveSibling(
                CONFIG_PATH.getFileName() + ".tmp"
            );
            Files.writeString(tempFile, GSON.toJson(json));
            Files.move(
                tempFile,
                CONFIG_PATH,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                java.nio.file.StandardCopyOption.ATOMIC_MOVE
            );
        } catch (IOException e) {
            NoFingerprint.LOGGER.error(
                "[NoFingerprint] Failed to save config to {}: {}",
                CONFIG_PATH,
                e.getMessage()
            );
        }
        // Null-guard avoids constructor's save→getInstance recursion; NoFingerprintClient seeds the first build.
        if (INSTANCE != null) {
            ModRegistry.rebuildDependencyClosure();
        }
    }

    public SpoofSettings getSettings() {
        return settings;
    }

    public void setCurrentServer(String server) {
        this.currentServer = normalizeAddress(server);
    }

    public String getCurrentServer() {
        return currentServer;
    }

    CompatibilityPolicy.Decisions compatibility() {
        return CompatibilityPolicy.decide(new CompatibilityPolicy.Context(
            EXPLOIT_PREVENTER_LOADED,
            NO_CHAT_REPORTS_LOADED,
            NO_PRYING_EYES_LOADED,
            settings.isSpoofAsVanilla(),
            settings.isIsolatePackCache(),
            settings.isBlockLocalPackUrls(),
            settings.isTranslationProtectionEnabled(),
            settings.isStripModShaders(),
            settings.shouldNotSign(),
            settings.isDisableTelemetry()
        ));
    }

    // Identity protection
    /**
     * Whether the brand string should be overridden to "vanilla".
     * ExploitPreventer does not spoof brand — this is never EP-gated.
     */
    public boolean shouldSpoofBrand() {
        return compatibility().spoofBrand();
    }

    /**
     * Whether channel spoofing/filtering should be active. Independent of brand —
     * the filter mode (block-all vs whitelist) is chosen by isVanillaMode/
     * isFabricMode in SpoofSettings. ExploitPreventer does not filter channels.
     */
    public boolean shouldSpoofChannels() {
        return compatibility().filterChannels();
    }

    /** ExploitPreventer does not filter known-packs. */
    public boolean shouldSpoofKnownPacks() {
        return compatibility().filterKnownPacks();
    }

    public String getEffectiveBrand() {
        return settings.getEffectiveBrand();
    }

    // Resource pack protection
    /**
     * Per-account pack cache. EP overlaps on the same path but the two mixins
     * coexist (see {@link CompatibilityPolicy}); NF stays in charge of the toggle.
     */
    public boolean shouldIsolatePackCache() {
        return compatibility().isolatePackCache();
    }

    /**
     * Local/private pack URL blocking. Stood down when ExploitPreventer is loaded
     * because both wrap the same {@code HttpUtil.getInputStream} call.
     */
    public boolean shouldBlockLocalPackUrls() {
        return compatibility().blockLocalPackUrls();
    }

    /** Per-mod shader strip. EP lacks this defense. */
    public boolean shouldStripModShaders() {
        return compatibility().stripModShaders();
    }

    /** True when any server-pack wrapping feature is active (whole-pack strip or per-mod shader strip). */
    public boolean shouldWrapServerPacks() {
        return shouldStripPack() || shouldStripModShaders();
    }

    // Key resolution protection
    /**
     * Stood down when ExploitPreventer is loaded — both wrap
     * {@code TranslatableContents.decompose} / the component codec.
     */
    public boolean isTranslationProtectionEnabled() {
        return compatibility().translationProtection();
    }

    public boolean isMeteorFix() {
        return settings.isMeteorFix();
    }

    // Bypass Server Pack Requirement
    public SpoofSettings.StripMode getPackStripMode() {
        return settings.getPackStripMode();
    }

    /** EP has no pack-strip equivalent. */
    public boolean shouldStripPack() {
        return compatibility().stripPack();
    }

    /** The consent overlay prompt should show for an incoming push. */
    public boolean shouldShowPackOverlay() {
        return settings.getPackStripMode() == SpoofSettings.StripMode.ASK;
    }

    // Alerts and logging
    public boolean shouldShowAlerts() {
        return settings.isShowAlerts();
    }

    public boolean shouldShowToasts() {
        return settings.isShowToasts();
    }

    public boolean isLogDetections() {
        return settings.isLogDetections();
    }

    public boolean isDebugAlerts() {
        return settings.isDebugAlerts();
    }

    // Chat signing
    public SpoofSettings.SigningMode getSigningMode() {
        return settings.getSigningMode();
    }

    public boolean shouldNotSign() {
        return compatibility().applyUnsignedChat();
    }

    // Privacy
    public boolean shouldDisableTelemetry() {
        return compatibility().blockTelemetry();
    }

    /**
     * Normalizes a server address by converting to lowercase and removing default port.
     * @param address The server address to normalize
     * @return Normalized address or "unknown" if null
     */
    public static String normalizeAddress(String address) {
        if (address == null) return "unknown";
        String normalized = address.toLowerCase().trim();
        if (normalized.endsWith(":25565")) {
            normalized = normalized.substring(0, normalized.length() - 6);
        }
        return normalized;
    }
}
