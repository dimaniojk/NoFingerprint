package io.github.dimaniojk.nofingerprint.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.github.dimaniojk.nofingerprint.NoFingerprint;
import io.github.dimaniojk.nofingerprint.PrivacyLogger;
import io.github.dimaniojk.nofingerprint.config.NoFingerprintConfig;
import io.github.dimaniojk.nofingerprint.debug.ProbeDiagnostics;
import io.github.dimaniojk.nofingerprint.util.LocalAddressUtil;
import io.github.dimaniojk.nofingerprint.util.RedirectPolicy;
import net.minecraft.util.HttpUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Vanilla-aligned redirect handling with per-hop local-address rejection.
 *
 * <p>Follows 300/301/302/303/305/307/308. 305 is re-issued through the named proxy via
 * {@link Proxy.Type#HTTP}. Cross-protocol redirects are rejected.
 *
 * <p>1.20.1 and 1.20.2 hook {@code HttpUtil.method_15303} (the {@code downloadTo}
 * lambda). 1.20.3+ hooks {@code downloadFile}. Both call
 * {@code HttpURLConnection.getInputStream()} — verified on the 1.20.1 mapped jar.
 *
 * <p>Each new redirect connection sets a unique {@link Authenticator}. JDK's
 * {@code HttpClient.New} compatibility check rejects any cached
 * {@code HttpClient} whose {@code AuthCache} does not match the new
 * connection's, closing the cached socket and creating a fresh one — so each
 * hop uses a fresh TCP socket, matching vanilla MC's
 * {@code setInstanceFollowRedirects(true)} behavior. The {@code Authenticator}
 * is never invoked unless the server responds with 401/407.
 *
 * <p>DNS TOCTOU: PARTIALLY MITIGATED — every hop's host is resolved and classified
 * before {@code openConnection}, but {@code HttpURLConnection} may resolve again
 * at connect. See {@link RedirectPolicy}.
 */
@Mixin(HttpUtil.class)
public class HttpUtilMixin {

    @SuppressWarnings("deprecation")
    @WrapOperation(
        //? if >=1.20.3 {
        method = "downloadFile",
        //?} else {
        /*// 1.20.1 / 1.20.2: HttpUtil.downloadFile doesn't exist. HTTP work lives in
        // private static method_15303 (Yarn intermediary for the downloadTo lambda).
        method = {"method_15303", "lambda$downloadTo$0"},
        *///?}
        at = @At(value = "INVOKE", target = "Ljava/net/HttpURLConnection;getInputStream()Ljava/io/InputStream;"),
        require = 1
    )
    private static InputStream nofingerprint$downloadFile(
            HttpURLConnection instance,
            Operation<InputStream> original,
            @Local(argsOnly = true) Proxy proxy,
            @Local(argsOnly = true) Map<String, String> requestProperties,
            @Local LocalRef<HttpURLConnection> httpURLConnection) throws IOException {

        if (!NoFingerprintConfig.getInstance().shouldBlockLocalPackUrls()) return original.call(instance);

        nofingerprint$rejectIfBlocked(instance.getURL());

        instance.setInstanceFollowRedirects(false);

        int maxRedirects = RedirectPolicy.maxRedirects();
        int redirects = 0;
        Set<String> seenHops = new HashSet<>();
        seenHops.add(instance.getURL().toString());
        int status = instance.getResponseCode();

        while (instance.getHeaderField("Location") != null
                && RedirectPolicy.isFollowableRedirect(status)) {
            if (redirects >= maxRedirects - 1) {
                // Mirror vanilla JDK's setProxiedClient leak so cap-boundary TCP-count fingerprinting fails.
                try {
                    URL leakUrl = RedirectPolicy.resolveLocation(instance.getURL(), instance.getHeaderField("Location"));
                    int leakPort = leakUrl.getPort() == -1 ? leakUrl.getDefaultPort() : leakUrl.getPort();
                    //noinspection resource
                    new Socket(leakUrl.getHost(), leakPort);
                } catch (Exception ignored) {}
                throw new ProtocolException("Server redirected too many times (" + maxRedirects + ")");
            }

            if (status == 305) {
                URL proxyUrl;
                try {
                    proxyUrl = RedirectPolicy.resolveLocation(instance.getURL(), instance.getHeaderField("Location"));
                } catch (MalformedURLException ignored) {
                    break;
                }
                if (!RedirectPolicy.isHttpOrHttps(proxyUrl)) break;

                nofingerprint$rejectIfBlocked(proxyUrl);
                if (!seenHops.add(proxyUrl.toString())) {
                    throw new ProtocolException("Redirect loop detected");
                }

                int proxyPort = proxyUrl.getPort() == -1 ? proxyUrl.getDefaultPort() : proxyUrl.getPort();
                Proxy hopProxy = new Proxy(Proxy.Type.HTTP,
                    InetSocketAddress.createUnresolved(proxyUrl.getHost(), proxyPort));
                URL originalUrl = instance.getURL();
                instance = (HttpURLConnection) originalUrl.openConnection(hopProxy);
                instance.setAuthenticator(new Authenticator() {});
            } else {
                URL url;
                try {
                    url = RedirectPolicy.resolveLocation(instance.getURL(), instance.getHeaderField("Location"));
                    if (!instance.getURL().getProtocol().equalsIgnoreCase(url.getProtocol())) break;
                } catch (MalformedURLException exception) {
                    break;
                }

                nofingerprint$rejectIfBlocked(url);
                if (!seenHops.add(url.toString())) {
                    throw new ProtocolException("Redirect loop detected");
                }

                instance = (HttpURLConnection) url.openConnection(proxy);
                instance.setAuthenticator(new Authenticator() {});
            }

            instance.setInstanceFollowRedirects(false);
            requestProperties.forEach(instance::setRequestProperty);

            status = instance.getResponseCode();
            redirects++;
        }

        httpURLConnection.set(instance);
        return original.call(instance);
    }

    @Unique
    private static void nofingerprint$rejectIfBlocked(URL url) throws IOException {
        if (url == null) return;
        if (RedirectPolicy.isBlockedHop(url, LocalAddressUtil.serverAddress)) {
            NoFingerprint.LOGGER.warn("[NoFingerprint] Blocked connection to local address: {}", url);
            ProbeDiagnostics.log("blocked pack url={} serverAddress={}", url, LocalAddressUtil.serverAddress);
            PrivacyLogger.alertLocalPortScanDetected(url.toString(), true);
            throw new IllegalStateException("Tried to connect to local address!");
        }
    }
}
