package io.github.dimaniojk.nofingerprint.debug;

import io.github.dimaniojk.nofingerprint.NoFingerprint;

/**
 * Development-only structured probe logs. Off unless the JVM is started with
 * {@code -Dnofingerprint.debug.probes=true} or {@code NOFINGERPRINT_DEBUG_PROBES=true}.
 *
 * <p>Must never log access tokens, refresh tokens, session secrets, or Microsoft
 * credentials. Channel names, pack URLs, and feature decisions are the intended
 * surface for the Phase 2 probe harness.
 */
public final class ProbeDiagnostics {

    private ProbeDiagnostics() {}

    public static boolean enabled() {
        if (Boolean.getBoolean("nofingerprint.debug.probes")) {
            return true;
        }
        String env = System.getenv("NOFINGERPRINT_DEBUG_PROBES");
        return env != null && env.equalsIgnoreCase("true");
    }

    public static void log(String format, Object... args) {
        if (!enabled()) {
            return;
        }
        NoFingerprint.LOGGER.info("[NoFingerprint][probe] " + format, args);
    }
}
