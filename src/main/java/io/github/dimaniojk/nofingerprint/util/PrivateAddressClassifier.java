package io.github.dimaniojk.nofingerprint.util;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Explicit local/private/non-unicast address classification for resource-pack URL
 * blocking. Does <em>not</em> use {@link InetAddress#isSiteLocalAddress()}, which
 * misses IPv6 ULA ({@code fc00::/7}) and CGNAT ({@code 100.64.0.0/10}).
 *
 * <p>A hostname is unsafe if <strong>any</strong> address returned by
 * {@link InetAddress#getAllByName(String)} is blocked. IPv6 answers are not
 * ignored. IPv4-mapped IPv6 ({@code ::ffff:x.x.x.x}) is unwrapped and classified
 * as IPv4.
 *
 * <p>DNS TOCTOU (the set of records changing between this check and the later
 * TCP connect) is <strong>not</strong> solved here — see {@code RedirectPolicy}
 * and {@code SECURITY_AUDIT.md}.
 */
public final class PrivateAddressClassifier {

    private PrivateAddressClassifier() {}

    public static boolean hostResolvesToBlocked(String host) throws UnknownHostException {
        if (host == null || host.isEmpty()) {
            return false;
        }
        String normalized = stripIpv6Brackets(host);
        // Fail closed on mixed records: a public A plus a loopback AAAA is still unsafe.
        // getAllByName is used so IPv6 answers are not ignored. This is check-time only;
        // HttpURLConnection may resolve again at connect (DNS TOCTOU).
        return anyBlocked(InetAddress.getAllByName(normalized));
    }

    /** True if any candidate address is blocked. Empty/null arrays are not blocked. */
    static boolean anyBlocked(InetAddress[] addresses) {
        if (addresses == null) {
            return false;
        }
        for (InetAddress address : addresses) {
            if (isBlocked(address)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBlocked(InetAddress address) {
        if (address == null) {
            return false;
        }
        if (address instanceof Inet6Address inet6) {
            InetAddress unwrapped = unwrapIpv4MappedOrCompatible(inet6);
            if (unwrapped instanceof Inet4Address) {
                return isBlockedIpv4((Inet4Address) unwrapped);
            }
            return isBlockedIpv6(inet6);
        }
        if (address instanceof Inet4Address inet4) {
            return isBlockedIpv4(inet4);
        }
        return false;
    }

    static String stripIpv6Brackets(String host) {
        if (host.length() >= 2 && host.charAt(0) == '[' && host.charAt(host.length() - 1) == ']') {
            return host.substring(1, host.length() - 1);
        }
        return host;
    }

    private static boolean isBlockedIpv4(Inet4Address address) {
        int ip = ipv4ToInt(address);
        // 0.0.0.0/8  this-network (includes 0.0.0.0 any-local)
        if (inCidr(ip, 0x00000000, 8)) return true;
        // 10.0.0.0/8
        if (inCidr(ip, 0x0A000000, 8)) return true;
        // 100.64.0.0/10 CGNAT
        if (inCidr(ip, 0x64400000, 10)) return true;
        // 127.0.0.0/8 loopback
        if (inCidr(ip, 0x7F000000, 8)) return true;
        // 169.254.0.0/16 link-local
        if (inCidr(ip, 0xA9FE0000, 16)) return true;
        // 172.16.0.0/12
        if (inCidr(ip, 0xAC100000, 12)) return true;
        // 192.168.0.0/16
        if (inCidr(ip, 0xC0A80000, 16)) return true;
        // 224.0.0.0/4 multicast — not a unicast HTTP origin
        if (inCidr(ip, 0xE0000000, 4)) return true;
        // 240.0.0.0/4 reserved (includes broadcast)
        if (inCidr(ip, 0xF0000000, 4)) return true;
        return false;
    }

    private static boolean isBlockedIpv6(Inet6Address address) {
        byte[] a = address.getAddress();
        if (a.length != 16) {
            return false;
        }
        if (isUnspecifiedIpv6(a) || isLoopbackIpv6(a)) {
            return true;
        }
        // fc00::/7 unique-local
        if ((a[0] & 0xFE) == 0xFC) {
            return true;
        }
        // fe80::/10 link-local
        if (a[0] == (byte) 0xFE && (a[1] & 0xC0) == 0x80) {
            return true;
        }
        // ff00::/8 multicast
        if (a[0] == (byte) 0xFF) {
            return true;
        }
        return false;
    }

    /**
     * Unwrap {@code ::ffff:x.x.x.x} (mapped) and deprecated {@code ::x.x.x.x}
     * (IPv4-compatible, excluding {@code ::} and {@code ::1}).
     */
    static InetAddress unwrapIpv4MappedOrCompatible(Inet6Address inet6) {
        byte[] a = inet6.getAddress();
        if (a.length != 16) {
            return inet6;
        }
        if (isIpv4Mapped(a) || isIpv4CompatibleUnicast(a)) {
            byte[] v4 = Arrays.copyOfRange(a, 12, 16);
            try {
                return InetAddress.getByAddress(v4);
            } catch (UnknownHostException e) {
                return inet6;
            }
        }
        return inet6;
    }

    private static boolean isIpv4Mapped(byte[] a) {
        for (int i = 0; i < 10; i++) {
            if (a[i] != 0) return false;
        }
        return a[10] == (byte) 0xFF && a[11] == (byte) 0xFF;
    }

    /** ::/96 with a non-loopback, non-unspecified IPv4 tail. */
    private static boolean isIpv4CompatibleUnicast(byte[] a) {
        for (int i = 0; i < 12; i++) {
            if (a[i] != 0) return false;
        }
        if (isUnspecifiedIpv6(a) || isLoopbackIpv6(a)) {
            return false;
        }
        return true;
    }

    private static boolean isUnspecifiedIpv6(byte[] a) {
        for (byte b : a) {
            if (b != 0) return false;
        }
        return true;
    }

    private static boolean isLoopbackIpv6(byte[] a) {
        for (int i = 0; i < 15; i++) {
            if (a[i] != 0) return false;
        }
        return a[15] == 1;
    }

    private static int ipv4ToInt(Inet4Address address) {
        byte[] b = address.getAddress();
        return ((b[0] & 0xFF) << 24)
            | ((b[1] & 0xFF) << 16)
            | ((b[2] & 0xFF) << 8)
            | (b[3] & 0xFF);
    }

    private static boolean inCidr(int ip, int prefix, int prefixBits) {
        if (prefixBits <= 0) return true;
        if (prefixBits >= 32) return ip == prefix;
        int mask = -1 << (32 - prefixBits);
        return (ip & mask) == (prefix & mask);
    }
}
