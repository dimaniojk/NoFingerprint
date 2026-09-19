package io.github.dimaniojk.nofingerprint.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * Resource-pack HTTP redirect policy shared by {@code HttpUtilMixin} and tests.
 *
 * <p>Follows the same status set vanilla JDK {@code HttpURLConnection} uses
 * (300–303, 305, 307) plus <strong>308</strong> Permanent Redirect, which the
 * previous mixin omitted. 304 Not Modified and unused 306 are not redirects
 * and are not followed. Depth is capped ({@link #DEFAULT_MAX_REDIRECTS}); the
 * mixin also rejects repeated hop URLs to avoid simple loops.
 *
 * <p>Every hop's host is classified with {@link PrivateAddressClassifier}. The
 * initial URL and each {@code Location} are checked <em>before</em> opening the
 * next connection. Cross-protocol redirects are not followed (matches vanilla).
 *
 * <p>DNS TOCTOU status: <strong>PARTIALLY MITIGATED</strong>. All A/AAAA records
 * at check time are inspected, and every redirect hop is re-checked, but Java
 * {@code HttpURLConnection} may resolve the hostname again at connect. Pinning
 * the connected {@code InetAddress} would require replacing the HTTP stack.
 */
public final class RedirectPolicy {

    /** Same default as {@code http.maxRedirects} / vanilla JDK. */
    public static final int DEFAULT_MAX_REDIRECTS = 20;

    private RedirectPolicy() {}

    public static boolean isFollowableRedirect(int status) {
        return status == 300
            || status == 301
            || status == 302
            || status == 303
            || status == 305
            || status == 307
            || status == 308;
    }

    public static boolean isHttpOrHttps(URL url) {
        if (url == null || url.getProtocol() == null) return false;
        String protocol = url.getProtocol();
        return protocol.equalsIgnoreCase("http") || protocol.equalsIgnoreCase("https");
    }

    public static URL resolveLocation(URL current, String location) throws MalformedURLException {
        if (location == null || location.isEmpty()) {
            throw new MalformedURLException("empty Location");
        }
        try {
            return new URL(location);
        } catch (MalformedURLException e) {
            if (current == null) throw e;
            return new URL(current, location);
        }
    }

    public static int maxRedirects() {
        String property = System.getProperty("http.maxRedirects");
        if (property == null) return DEFAULT_MAX_REDIRECTS;
        try {
            return Math.max(Integer.parseInt(property), 1);
        } catch (NumberFormatException e) {
            return DEFAULT_MAX_REDIRECTS;
        }
    }

    /**
     * Whether {@code hopHost} must not be contacted.
     *
     * <p>When the connected game server itself is local/private, blocking is
     * skipped so LAN worlds can still push packs from the same machine.
     */
    public static boolean isBlockedHopHost(String hopHost, String gameServerHost) throws UnknownHostException {
        if (PrivateAddressClassifier.hostResolvesToBlocked(gameServerHost)) {
            return false;
        }
        return PrivateAddressClassifier.hostResolvesToBlocked(hopHost);
    }

    public static boolean isBlockedHop(URL url, String gameServerHost) throws UnknownHostException {
        if (url == null) return false;
        return isBlockedHopHost(url.getHost(), gameServerHost);
    }
}
