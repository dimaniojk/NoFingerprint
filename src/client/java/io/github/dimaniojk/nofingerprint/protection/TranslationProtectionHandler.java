package io.github.dimaniojk.nofingerprint.protection;

import io.github.dimaniojk.nofingerprint.NoFingerprint;
import io.github.dimaniojk.nofingerprint.PrivacyLogger;
import io.github.dimaniojk.nofingerprint.config.NoFingerprintConfig;
import io.github.dimaniojk.nofingerprint.config.SpoofSettings;
import io.github.dimaniojk.nofingerprint.detection.PacketContext;
import io.github.dimaniojk.nofingerprint.lang.NoFingerprintLang;
import io.github.dimaniojk.nofingerprint.lang.NoFingerprintStrings;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Centralized handler for key resolution protection alerts.
 *
 * Alert format:
 * [NoFingerprint] Key resolution probe detected           (header with cooldown)
 * [key.meteor-client.open-gui] 'Right Shift'→'key.meteor-client.open-gui'  (detail, deduped)
 * [key.hotbar.6] 'Q'→'6'
 *
 * - Header: Always deferred until sendDetail confirms something to report
 * - Details: Sent when values are changed (deduped per session)
 * - Debug mode: Details shown for ALL non-vanilla keys including unchanged; header deferred same as normal
 * - Logging: Deduped to prevent spam from multiple render calls
 * - Detection works even if protection is OFF (alerts/logs still show)
 */
public class TranslationProtectionHandler {

    /** The type of interception that triggered the alert. */
    public enum InterceptionType {
        TRANSLATION("Translation"),
        KEYBIND("Keybind");

        private final String displayName;

        InterceptionType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /** Dedup key for detail alerts — type + key name, since Translation and Keybind produce different details */
    private record AlertDedupeKey(InterceptionType type, String keyName) {}

    /** Dedup key for logs — full tuple to preserve log accuracy */
    private record LogDedupeKey(
        InterceptionType type,
        String packetName,
        String keyName,
        String originalValue,
        String spoofedValue
    ) {}

    // Separate deduplication sets for alerts and logging
    private static final Set<AlertDedupeKey> alertedKeys =
        ConcurrentHashMap.newKeySet();
    private static final Set<LogDedupeKey> loggedKeys =
        ConcurrentHashMap.newKeySet();

    // Size limits to prevent unbounded growth
    private static final int MAX_DEDUPE_ENTRIES = 500;

    /**
     * Max characters of a key/value shown in a chat detail alert. Deliberately
     * high so it never touches a legitimate translation/keybind value, but caps
     * a maliciously huge resource-pack value (e.g. a §k-prefixed multi-KB string
     * from a poisoned pack) so the alert can't flood chat or lag the client.
     * Display-only — dedup/log paths still use the full value. Tunable.
     */
    private static final int MAX_ALERT_VALUE_LEN = 256;

    private static volatile long lastHeaderTime = 0;
    private static volatile boolean headerPending = false;

    private static final long HEADER_COOLDOWN_MS = 5000; // 5 seconds between headers

    private TranslationProtectionHandler() {}

    /**
     * Notify that an exploit attempt was detected.
     *
     * Always defers the header until {@link #sendDetail} confirms there is
     * something to report. This prevents the toast/header from firing for
     * packets that only contain vanilla or whitelisted keys.
     */
    public static void notifyExploitDetected() {
        if (!shouldProcess()) {
            return;
        }

        long now = System.currentTimeMillis();

        if (now - lastHeaderTime < HEADER_COOLDOWN_MS) {
            return;
        }

        // Defer header until sendDetail confirms something to show
        headerPending = true;
    }

    /**
     * Emit the header alert, toast, log, and one-time hint.
     * Called either immediately (debug mode) or deferred (normal mode, from sendDetail).
     */
    private static void emitHeader() {
        String source = PacketContext.getPacketName();

        // Chat alert: red, no emoji icon
        if (NoFingerprintConfig.getInstance().shouldShowAlerts()) {
            Minecraft mc = Minecraft.getInstance();
            Runnable sendAlert = () -> {
                if (mc.player != null) {
                    String alertText = NoFingerprintLang.tr(
                        NoFingerprintStrings.ALERT_KEYRESOLUTION
                    );
                    //? if >=26.1 {
                    /*mc.player.sendSystemMessage(
                        Component.literal("[NoFingerprint] ").withStyle(ChatFormatting.DARK_PURPLE)
                            .append(Component.literal(alertText).withStyle(ChatFormatting.RED)));*/
                    //?} else {
                    mc.player.displayClientMessage(
                        Component.literal("[NoFingerprint] ")
                            .withStyle(ChatFormatting.DARK_PURPLE)
                            .append(
                                Component.literal(alertText).withStyle(
                                    ChatFormatting.RED
                                )
                            ),
                        false
                    );
                    //?}
                }
            };
            if (mc.isSameThread()) {
                sendAlert.run();
            } else {
                mc.execute(sendAlert);
            }
        }

        // Toast notification: red, no emoji icon
        if (NoFingerprintConfig.getInstance().shouldShowToasts()) {
            PrivacyLogger.showToastRaw(
                Component.literal(
                    NoFingerprintLang.tr(NoFingerprintStrings.TOAST_KEYRESOLUTION)
                ).withStyle(ChatFormatting.RED),
                null
            );
        }

        if (NoFingerprintConfig.getInstance().isLogDetections()) {
            NoFingerprint.logInfoAsync(
                "[NoFingerprint] Key resolution exploit detected via {}",
                source
            );
        }

        // One-time hint, delayed so it appears after the first alert.
        SpoofSettings settings = NoFingerprintConfig.getInstance().getSettings();
        if (!settings.isAlertHintShown()) {
            settings.setAlertHintShown(true);
            CompletableFuture.runAsync(() -> NoFingerprintConfig.getInstance().save());
            CompletableFuture.delayedExecutor(
                2,
                java.util.concurrent.TimeUnit.SECONDS
            ).execute(() -> {
                Minecraft mc = Minecraft.getInstance();
                mc.execute(() -> {
                    if (mc.player != null) {
                        String hintText = NoFingerprintLang.tr(
                            NoFingerprintStrings.HINT_ALERTS_CAN_BE_DISABLED
                        );
                        //? if >=26.1 {
                        /*mc.player.sendSystemMessage(
                            Component.literal(hintText).withStyle(ChatFormatting.AQUA));*/
                        //?} else {
                        mc.player.displayClientMessage(
                            Component.literal(hintText).withStyle(
                                ChatFormatting.AQUA
                            ),
                            false
                        );
                        //?}
                    }
                });
            });
        }
    }

    /**
     * Send detail alert for a key interception.
     * Deduped per session to prevent spam.
     *
     * In normal mode, flushes the deferred header on the first detail.
     *
     * @param type The interception type (TRANSLATION or KEYBIND)
     * @param keyName The translation/keybind key name
     * @param originalValue What Minecraft would have resolved it to
     * @param spoofedValue What we're returning instead
     */
    public static void sendDetail(
        InterceptionType type,
        String keyName,
        String originalValue,
        String spoofedValue
    ) {
        if (!NoFingerprintConfig.getInstance().shouldShowAlerts()) {
            return;
        }

        // Clear if too large to prevent unbounded growth
        if (alertedKeys.size() >= MAX_DEDUPE_ENTRIES) {
            alertedKeys.clear();
        }

        // Dedupe by type + key name — Translation and Keybind produce different details for the same key
        if (!alertedKeys.add(new AlertDedupeKey(type, keyName))) {
            return;
        }

        // Flush deferred header on first detail
        if (headerPending) {
            headerPending = false;
            lastHeaderTime = System.currentTimeMillis();
            emitHeader();
        }

        // Truncate for display only — a poisoned pack can make these multi-KB.
        String detailText = "[" + truncateForAlert(keyName) + "] '"
            + truncateForAlert(originalValue) + "'→'"
            + truncateForAlert(spoofedValue) + "'";

        // Detail alert: [key.hotbar.6] 'Q'→'6'
        // In debug mode, prepend [Type:packetName] in purple
        if (NoFingerprintConfig.getInstance().isDebugAlerts()) {
            String packetName = PacketContext.getPacketName();
            MutableComponent detail = Component.literal(
                "[" + type.getDisplayName() + ":" + packetName + "] "
            )
                .withStyle(ChatFormatting.DARK_PURPLE)
                .append(
                    Component.literal(detailText).withStyle(ChatFormatting.DARK_GRAY)
                );
            PrivacyLogger.sendKeybindDetail(detail);
        } else {
            PrivacyLogger.sendKeybindDetail(detailText);
        }
    }

    /**
     * Send detail for debug mode only.
     * Called from paths that don't normally send details (unchanged values,
     * protection-disabled). Only fires when debug alerts are enabled.
     *
     * @param type The interception type (TRANSLATION or KEYBIND)
     * @param keyName The translation/keybind key name
     * @param originalValue What Minecraft would have resolved it to
     * @param spoofedValue What we're returning (may be same as original)
     */
    public static void sendDetailDebug(
        InterceptionType type,
        String keyName,
        String originalValue,
        String spoofedValue
    ) {
        if (!NoFingerprintConfig.getInstance().isDebugAlerts()) return;
        sendDetail(type, keyName, originalValue, spoofedValue);
    }

    /**
     * Log detection details.
     * Deduped to prevent spam from multiple render calls.
     *
     * @param type The interception type (TRANSLATION or KEYBIND)
     * @param keyName The translation/keybind key name
     * @param originalValue What Minecraft would have resolved it to
     * @param spoofedValue What we're returning (may be same as original)
     */
    public static void logDetection(
        InterceptionType type,
        String keyName,
        String originalValue,
        String spoofedValue
    ) {
        if (!NoFingerprintConfig.getInstance().isLogDetections()) {
            return;
        }

        String packetName = PacketContext.getPacketName();

        // Clear if too large to prevent unbounded growth
        if (loggedKeys.size() >= MAX_DEDUPE_ENTRIES) {
            loggedKeys.clear();
        }

        // Dedupe by full tuple to preserve log accuracy
        if (
            !loggedKeys.add(
                new LogDedupeKey(
                    type,
                    packetName,
                    keyName,
                    originalValue,
                    spoofedValue
                )
            )
        ) {
            return;
        }

        NoFingerprint.logInfoAsync(
            "[{}:{}] '{}' '{}' -> '{}'",
            type.getDisplayName(),
            packetName,
            keyName,
            originalValue,
            spoofedValue
        );
    }

    /**
     * Truncate a key/value for display in a chat alert. Legitimate values are
     * far under {@link #MAX_ALERT_VALUE_LEN} and pass through untouched; an
     * oversized value is cut and annotated with its real length so the alert
     * stays informative without flooding chat or lagging the client.
     */
    private static String truncateForAlert(String value) {
        if (value == null || value.length() <= MAX_ALERT_VALUE_LEN) {
            return value;
        }
        return value.substring(0, MAX_ALERT_VALUE_LEN) + "…(" + value.length() + " chars)";
    }

    /**
     * Check if we should process alerts/logs.
     * When both alerts AND logging are disabled, skip everything.
     */
    private static boolean shouldProcess() {
        return (
            NoFingerprintConfig.getInstance().shouldShowAlerts() ||
            NoFingerprintConfig.getInstance().isLogDetections()
        );
    }

    /**
     * Clear key-level dedup caches. Called when entering a new exploit context
     * so each sign/anvil probe gets fresh alerts and logs.
     * Does NOT reset the header cooldown — that prevents header spam across rapid probes.
     */
    public static void clearDedup() {
        alertedKeys.clear();
        loggedKeys.clear();
        headerPending = false;
    }

    /**
     * Clear all cached state. Called on disconnect.
     */
    public static void clearCache() {
        alertedKeys.clear();
        loggedKeys.clear();
        lastHeaderTime = 0;
        headerPending = false;
    }
}
