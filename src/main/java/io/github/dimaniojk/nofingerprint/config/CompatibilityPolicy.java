package io.github.dimaniojk.nofingerprint.config;

/**
 * Feature-level coexistence with ExploitPreventer, No Chat Reports, and No Prying Eyes.
 *
 * <p>ExploitPreventer matrix (code-verified against NikOverflow/ExploitPreventer):
 *
 * <table>
 *   <tr><th>NoFingerprint feature</th><th>EP</th><th>Decision</th></tr>
 *   <tr><td>Brand spoofing</td><td>C — not implemented</td>
 *       <td>KEEP. Dual-install previously disabled this even though EP has no
 *       {@code ClientBrandRetriever} mixin.</td></tr>
 *   <tr><td>Channel / payload filtering</td><td>C — EP only records payload
 *       registrations for translation allowlists</td>
 *       <td>KEEP. Dual {@code Connection.send} filters do not exist in EP.</td></tr>
 *   <tr><td>Known-pack filtering</td><td>C — not implemented</td><td>KEEP.</td></tr>
 *   <tr><td>Pack-strip / required-pack bypass</td><td>C — not implemented</td>
 *       <td>KEEP.</td></tr>
 *   <tr><td>Shader override strip</td><td>C — not implemented</td><td>KEEP.</td></tr>
 *   <tr><td>Pack cache isolation</td><td>B — same {@code DownloadQueue} path,
 *       same {@code cacheDir/uuid/packId} layout</td>
 *       <td>KEEP. NF skips if another mod already reparented off {@code cacheDir};
 *       EP ignores the previous path and writes the same UUID layout. Dual
 *       {@code ModifyExpressionValue} is safe.</td></tr>
 *   <tr><td>Local/private pack URL blocking</td><td>A — same
 *       {@code HttpUtil}/{@code getInputStream} WrapOperation and redirect loop</td>
 *       <td>STAND DOWN NF. Two redirect-following wraps on one call would
 *       double-connect and fight over {@code setInstanceFollowRedirects}. EP's
 *       classifier is weaker (no ULA/CGNAT/308); that remaining gap is documented,
 *       not papered over by running both hooks.</td></tr>
 *   <tr><td>Translation / keybind protection</td><td>A — {@code ComponentCodec}
 *       + {@code TranslatableContents}/{@code KeybindContents} wraps</td>
 *       <td>STAND DOWN NF. Two {@code Language.getOrDefault} wraps on
 *       {@code decompose} would conflict on fallback/pack-echo behavior. EP's
 *       packet-origin marking is a complete replacement for this surface.</td></tr>
 * </table>
 *
 * <p>No Chat Reports / No Prying Eyes only overlap chat signing and telemetry.
 *
 * <p>This class is Minecraft-free so the matrix can be unit-tested without
 * FabricLoader.
 */
public final class CompatibilityPolicy {

    public record Context(
        boolean exploitPreventerLoaded,
        boolean noChatReportsLoaded,
        boolean noPryingEyesLoaded,
        boolean spoofAsVanilla,
        boolean isolatePackCache,
        boolean blockLocalPackUrls,
        boolean translationProtection,
        boolean stripModShaders,
        boolean shouldNotSign,
        boolean disableTelemetry
    ) {
        public boolean chatSigningManagedExternally() {
            return noChatReportsLoaded || noPryingEyesLoaded;
        }

        public boolean telemetryManagedExternally() {
            return noChatReportsLoaded || noPryingEyesLoaded;
        }
    }

    public record Decisions(
        boolean spoofBrand,
        boolean filterChannels,
        boolean filterKnownPacks,
        boolean isolatePackCache,
        boolean blockLocalPackUrls,
        boolean translationProtection,
        boolean stripPack,
        boolean stripModShaders,
        boolean applyUnsignedChat,
        boolean blockTelemetry
    ) {}

    private CompatibilityPolicy() {}

    public static Decisions decide(Context ctx) {
        boolean ep = ctx.exploitPreventerLoaded();
        return new Decisions(
            ctx.spoofAsVanilla(),
            true,
            true,
            ctx.isolatePackCache(),
            !ep && ctx.blockLocalPackUrls(),
            !ep && ctx.translationProtection(),
            true,
            ctx.stripModShaders(),
            !ctx.chatSigningManagedExternally() && ctx.shouldNotSign(),
            !ctx.telemetryManagedExternally() && ctx.disableTelemetry()
        );
    }

    /** True when NF must not wrap HttpUtil because EP already does. */
    public static boolean localUrlHookOwnedByExploitPreventer(boolean exploitPreventerLoaded) {
        return exploitPreventerLoaded;
    }

    /** True when NF must not wrap component/translation resolution because EP already does. */
    public static boolean translationHookOwnedByExploitPreventer(boolean exploitPreventerLoaded) {
        return exploitPreventerLoaded;
    }
}
