package io.github.dimaniojk.nofingerprint.util;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrivateAddressClassifierTest {

    private static void assertBlockedHost(String host) throws UnknownHostException {
        assertTrue(PrivateAddressClassifier.hostResolvesToBlocked(host), () -> "expected blocked: " + host);
    }

    private static void assertAllowedHost(String host) throws UnknownHostException {
        assertFalse(PrivateAddressClassifier.hostResolvesToBlocked(host), () -> "expected allowed: " + host);
    }

    private static void assertBlockedAddress(String literal) throws UnknownHostException {
        assertTrue(PrivateAddressClassifier.isBlocked(InetAddress.getByName(literal)), () -> "expected blocked: " + literal);
    }

    private static void assertAllowedAddress(String literal) throws UnknownHostException {
        assertFalse(PrivateAddressClassifier.isBlocked(InetAddress.getByName(literal)), () -> "expected allowed: " + literal);
    }

    @Test
    void publicIpv4IsAllowed() throws Exception {
        assertAllowedHost("1.1.1.1");
        assertAllowedHost("8.8.8.8");
        assertAllowedAddress("1.1.1.1");
        assertAllowedAddress("8.8.8.8");
    }

    @Test
    void publicIpv6IsAllowed() throws Exception {
        assertAllowedAddress("2001:4860:4860::8888");
        assertAllowedHost("2001:4860:4860::8888");
    }

    @Test
    void loopbackIpv4IsBlocked() throws Exception {
        assertBlockedHost("127.0.0.1");
        assertBlockedHost("127.1.2.3");
        assertBlockedHost("127.255.255.255");
    }

    @Test
    void rfc1918_10() throws Exception {
        assertBlockedHost("10.0.0.1");
        assertBlockedHost("10.255.255.255");
    }

    @Test
    void rfc1918_172() throws Exception {
        assertBlockedHost("172.16.0.1");
        assertBlockedHost("172.31.255.255");
        assertAllowedHost("172.15.255.255");
        assertAllowedHost("172.32.0.1");
    }

    @Test
    void rfc1918_192() throws Exception {
        assertBlockedHost("192.168.0.1");
        assertBlockedHost("192.168.255.255");
    }

    @Test
    void linkLocalIpv4() throws Exception {
        assertBlockedHost("169.254.1.1");
        assertAllowedHost("169.253.255.255");
        assertAllowedHost("169.255.0.1");
    }

    @Test
    void cgnatBoundaries() throws Exception {
        assertAllowedHost("100.63.255.255");
        assertBlockedHost("100.64.0.0");
        assertBlockedHost("100.64.0.1");
        assertBlockedHost("100.127.255.254");
        assertBlockedHost("100.127.255.255");
        assertAllowedHost("100.128.0.0");
    }

    @Test
    void thisNetworkAndUnspecifiedIpv4() throws Exception {
        assertBlockedHost("0.0.0.0");
        assertBlockedHost("0.0.0.1");
        assertAllowedHost("1.0.0.1");
    }

    @Test
    void multicastAndReservedIpv4() throws Exception {
        assertBlockedHost("224.0.0.1");
        assertBlockedHost("239.255.255.255");
        assertBlockedHost("240.0.0.1");
        assertBlockedHost("255.255.255.255");
        assertAllowedHost("223.255.255.255");
    }

    @Test
    void ipv6LoopbackAndUnspecified() throws Exception {
        assertBlockedHost("::1");
        assertBlockedHost("0:0:0:0:0:0:0:1");
        assertBlockedHost("::");
    }

    @Test
    void ipv6UniqueLocal() throws Exception {
        assertBlockedHost("fc00::1");
        assertBlockedHost("fc00::");
        assertBlockedHost("fdff:ffff::1");
        assertAllowedHost("fbff::1");
        assertAllowedHost("fe00::1");
    }

    @Test
    void ipv6LinkLocal() throws Exception {
        assertBlockedHost("fe80::1");
        assertBlockedHost("febf::1");
        assertAllowedHost("fe7f::1");
    }

    @Test
    void ipv6Multicast() throws Exception {
        assertBlockedHost("ff00::1");
        assertBlockedHost("ff02::1");
    }

    @Test
    void ipv4MappedIpv6() throws Exception {
        assertBlockedHost("::ffff:127.0.0.1");
        assertBlockedHost("::ffff:10.0.0.1");
        assertBlockedHost("::ffff:192.168.1.1");
        assertBlockedHost("::ffff:100.64.0.1");
        assertAllowedHost("::ffff:1.1.1.1");
        assertAllowedHost("::ffff:8.8.8.8");
    }

    @Test
    void ipv6BracketFormFromUrl() throws Exception {
        URL url = new URL("http://[::1]/");
        assertTrue(PrivateAddressClassifier.hostResolvesToBlocked(url.getHost()));
        URL mapped = new URL("http://[::ffff:127.0.0.1]/");
        assertTrue(PrivateAddressClassifier.hostResolvesToBlocked(mapped.getHost()));
    }

    @Test
    void nullAndEmptyAreNotBlocked() throws Exception {
        assertFalse(PrivateAddressClassifier.hostResolvesToBlocked(null));
        assertFalse(PrivateAddressClassifier.hostResolvesToBlocked(""));
        assertFalse(PrivateAddressClassifier.isBlocked(null));
    }

    @Test
    void stripIpv6Brackets() {
        assertTrue("::1".equals(PrivateAddressClassifier.stripIpv6Brackets("[::1]")));
        assertTrue("example".equals(PrivateAddressClassifier.stripIpv6Brackets("example")));
    }

    @Test
    void anyResolvedAddressUnsafeMeansBlocked() throws Exception {
        InetAddress pub = InetAddress.getByName("8.8.8.8");
        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        InetAddress ula = InetAddress.getByName("fc00::1");
        assertTrue(PrivateAddressClassifier.anyBlocked(new InetAddress[] { pub, loopback }));
        assertTrue(PrivateAddressClassifier.anyBlocked(new InetAddress[] { pub, ula }));
        assertFalse(PrivateAddressClassifier.anyBlocked(new InetAddress[] { pub }));
        assertFalse(PrivateAddressClassifier.anyBlocked(new InetAddress[0]));
        assertFalse(PrivateAddressClassifier.anyBlocked(null));
    }

    @Test
    void ipv4CompatibleIpv6IsUnwrapped() throws Exception {
        assertBlockedHost("::10.0.0.1");
        assertBlockedHost("::192.168.1.1");
        assertAllowedHost("::8.8.8.8");
    }
}
