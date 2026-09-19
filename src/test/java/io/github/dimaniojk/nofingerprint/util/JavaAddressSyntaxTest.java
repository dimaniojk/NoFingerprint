package io.github.dimaniojk.nofingerprint.util;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Documents how this JDK treats unusual IPv4 spellings. NoFingerprint does not
 * add a custom parser: if Java stores the token as a hostname rather than a
 * literal, the case is N/A (a DNS lookup would be non-deterministic).
 */
class JavaAddressSyntaxTest {

    @Test
    void javaUrlKeepsDecimalIpv4AsHostname() throws Exception {
        URL url = new URL("http://2130706433/");
        assertEquals("2130706433", url.getHost());
        // getAllByName would perform DNS — not invoked. N/A for classifier.
    }

    @Test
    void javaUrlKeepsHexIpv4AsHostname() throws Exception {
        URL url = new URL("http://0x7f000001/");
        assertEquals("0x7f000001", url.getHost());
    }

    @Test
    void javaUrlKeepsShortIpv4AsHostname() throws Exception {
        URL url = new URL("http://127.1/");
        assertEquals("127.1", url.getHost());
    }

    @Test
    void ipv6BracketFormIsClassifiedRegardlessOfJavaHostSpelling() throws Exception {
        URL url = new URL("http://[::1]/pack.zip");
        String host = url.getHost();
        // Observed on this JDK: URL.getHost() may keep brackets. Classifier strips them.
        assertTrue(host != null && !host.isEmpty(), () -> "empty host from " + url);
        assertTrue(PrivateAddressClassifier.hostResolvesToBlocked(host), () -> "host was: " + host);
        assertTrue(PrivateAddressClassifier.hostResolvesToBlocked("::1"));
        assertTrue(PrivateAddressClassifier.hostResolvesToBlocked("[::1]"));
    }

    @Test
    void uriRejectsInvalidIpv6Literal() {
        try {
            URI.create("http://[not-an-ip]/");
            throw new AssertionError("expected URI to reject [not-an-ip]");
        } catch (IllegalArgumentException ignored) {
            // N/A — Java rejected the syntax; no custom parser.
        }
    }

    @Test
    void dottedIpv4LiteralsDoNotNeedDns() throws UnknownHostException {
        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        assertTrue(loopback.isLoopbackAddress());
        assertTrue(PrivateAddressClassifier.isBlocked(loopback));
    }

    @Test
    void malformedUrlForEmptyHost() {
        try {
            new URL("http:///pack.zip");
        } catch (MalformedURLException ignored) {
            return;
        }
        // Some JDKs accept this; the empty host is not blocked.
        try {
            assertFalse(PrivateAddressClassifier.hostResolvesToBlocked(new URL("http:///pack.zip").getHost()));
        } catch (Exception ignored) {
            // N/A
        }
    }
}
