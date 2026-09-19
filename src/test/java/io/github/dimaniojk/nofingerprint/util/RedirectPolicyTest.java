package io.github.dimaniojk.nofingerprint.util;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedirectPolicyTest {

    @Test
    void followableStatusesInclude308() {
        assertTrue(RedirectPolicy.isFollowableRedirect(300));
        assertTrue(RedirectPolicy.isFollowableRedirect(301));
        assertTrue(RedirectPolicy.isFollowableRedirect(302));
        assertTrue(RedirectPolicy.isFollowableRedirect(303));
        assertTrue(RedirectPolicy.isFollowableRedirect(305));
        assertTrue(RedirectPolicy.isFollowableRedirect(307));
        assertTrue(RedirectPolicy.isFollowableRedirect(308));
        assertFalse(RedirectPolicy.isFollowableRedirect(200));
        assertFalse(RedirectPolicy.isFollowableRedirect(304));
        assertFalse(RedirectPolicy.isFollowableRedirect(404));
    }

    @Test
    void resolveLocationHandlesAbsoluteAndRelative() throws Exception {
        URL current = new URL("https://packs.example/a/b");
        assertEquals("http://127.0.0.1:8080/private",
            RedirectPolicy.resolveLocation(current, "http://127.0.0.1:8080/private").toString());
        assertEquals("https://packs.example/a/c",
            RedirectPolicy.resolveLocation(current, "c").toString());
    }

    @Test
    void loopbackRedirectTargetIsBlockedWhenGameServerIsRemote() throws Exception {
        URL local = new URL("http://127.0.0.1:8080/test.zip");
        assertTrue(RedirectPolicy.isBlockedHop(local, "1.1.1.1"));
        assertFalse(RedirectPolicy.isBlockedHop(local, "127.0.0.1"));
    }

    @Test
    void publicRedirectTargetIsAllowed() throws Exception {
        URL publicHost = new URL("https://8.8.8.8/pack.zip");
        assertFalse(RedirectPolicy.isBlockedHop(publicHost, "1.1.1.1"));
    }

    @Test
    void localHttpServer_302And308LocationsAreBlocked() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/to-loopback-302", exchange -> {
            exchange.getResponseHeaders().add("Location", "http://127.0.0.1:9/private");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/to-loopback-308", exchange -> {
            exchange.getResponseHeaders().add("Location", "http://127.0.0.1:9/private");
            exchange.sendResponseHeaders(308, -1);
            exchange.close();
        });
        server.start();
        int port = server.getAddress().getPort();
        try {
            assertRedirectBlocked(new URL("http://127.0.0.1:" + port + "/to-loopback-302"), 302);
            assertRedirectBlocked(new URL("http://127.0.0.1:" + port + "/to-loopback-308"), 308);
        } finally {
            server.stop(0);
        }
    }

    private static void assertRedirectBlocked(URL start, int expectedStatus) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) start.openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(2000);
        connection.setReadTimeout(2000);
        try {
            assertEquals(expectedStatus, connection.getResponseCode());
            assertTrue(RedirectPolicy.isFollowableRedirect(expectedStatus));
            URL next = RedirectPolicy.resolveLocation(start, connection.getHeaderField("Location"));
            // game server treated as remote so the Location hop is classified
            assertTrue(RedirectPolicy.isBlockedHop(next, "8.8.8.8"),
                () -> "Location should be blocked: " + next);
        } finally {
            connection.disconnect();
        }
    }

    @Test
    void crossProtocolIsDetected() throws Exception {
        assertTrue(RedirectPolicy.isHttpOrHttps(new URL("http://example/")));
        assertTrue(RedirectPolicy.isHttpOrHttps(new URL("https://example/")));
        assertFalse(RedirectPolicy.isHttpOrHttps(new URL("file:/tmp/x")));
        assertFalse(RedirectPolicy.isHttpOrHttps(URI.create("ftp://127.0.0.1/x").toURL()));
    }

    @Test
    void maxRedirectsDefaultsToTwenty() {
        assertEquals(20, RedirectPolicy.DEFAULT_MAX_REDIRECTS);
        assertTrue(RedirectPolicy.maxRedirects() >= 1);
    }
}
