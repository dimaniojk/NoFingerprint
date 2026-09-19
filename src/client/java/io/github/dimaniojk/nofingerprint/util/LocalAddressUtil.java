package io.github.dimaniojk.nofingerprint.util;

import java.net.UnknownHostException;

/**
 * Local address detection and connected-server state for resource-pack URL blocking.
 *
 * <p>Classification lives in {@link PrivateAddressClassifier} (explicit CIDR, ULA, CGNAT,
 * mapped IPv6). This class only holds the connected game-server address and delegates.
 */
public class LocalAddressUtil {

    /**
     * Current server address, set on connect, cleared on disconnect.
     * Used to skip local address blocking when already connected to a local server.
     */
    public static volatile String serverAddress = null;

    /**
     * True if {@code host} resolves to any blocked (loopback / private / ULA / CGNAT /
     * link-local / multicast / reserved) address.
     *
     * @throws UnknownHostException if the hostname cannot be resolved
     */
    public static boolean isLocalAddress(String host) throws UnknownHostException {
        return PrivateAddressClassifier.hostResolvesToBlocked(host);
    }
}
