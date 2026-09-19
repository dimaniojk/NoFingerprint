package io.github.dimaniojk.nofingerprint.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatibilityPolicyTest {

    private static CompatibilityPolicy.Context base(boolean epLoaded) {
        return new CompatibilityPolicy.Context(
            epLoaded,
            false,
            false,
            true,
            true,
            true,
            true,
            true,
            true,
            true
        );
    }

    @Test
    void withoutExploitPreventer_allRequestedFeaturesStayOn() {
        CompatibilityPolicy.Decisions d = CompatibilityPolicy.decide(base(false));
        assertTrue(d.spoofBrand());
        assertTrue(d.filterChannels());
        assertTrue(d.filterKnownPacks());
        assertTrue(d.isolatePackCache());
        assertTrue(d.blockLocalPackUrls());
        assertTrue(d.translationProtection());
        assertTrue(d.stripPack());
        assertTrue(d.stripModShaders());
        assertTrue(d.applyUnsignedChat());
        assertTrue(d.blockTelemetry());
    }

    @Test
    void withExploitPreventer_identityAndNfOnlyFeaturesRemain() {
        CompatibilityPolicy.Decisions d = CompatibilityPolicy.decide(base(true));
        assertTrue(d.spoofBrand(), "EP does not spoof brand");
        assertTrue(d.filterChannels(), "EP does not filter channels");
        assertTrue(d.filterKnownPacks(), "EP does not filter known-packs");
        assertTrue(d.isolatePackCache(), "cache mixins coexist");
        assertTrue(d.stripPack(), "EP has no pack-strip");
        assertTrue(d.stripModShaders(), "EP has no shader strip");
        assertFalse(d.blockLocalPackUrls(), "same HttpUtil wrap — stand down NF");
        assertFalse(d.translationProtection(), "same component wrap — stand down NF");
    }

    @Test
    void withExploitPreventer_userCanDisableBrandSpoof() {
        CompatibilityPolicy.Context ctx = new CompatibilityPolicy.Context(
            true, false, false,
            false, true, true, true, true, false, true
        );
        CompatibilityPolicy.Decisions d = CompatibilityPolicy.decide(ctx);
        assertFalse(d.spoofBrand());
        assertTrue(d.filterChannels());
    }

    @Test
    void noChatReports_standsDownSigningAndTelemetryOnly() {
        CompatibilityPolicy.Context ctx = new CompatibilityPolicy.Context(
            false, true, false,
            true, true, true, true, true, true, true
        );
        CompatibilityPolicy.Decisions d = CompatibilityPolicy.decide(ctx);
        assertTrue(d.spoofBrand());
        assertTrue(d.filterChannels());
        assertFalse(d.applyUnsignedChat());
        assertFalse(d.blockTelemetry());
        assertTrue(d.translationProtection());
        assertTrue(d.blockLocalPackUrls());
    }

    @Test
    void hookOwnershipHelpers() {
        assertFalse(CompatibilityPolicy.localUrlHookOwnedByExploitPreventer(false));
        assertTrue(CompatibilityPolicy.localUrlHookOwnedByExploitPreventer(true));
        assertFalse(CompatibilityPolicy.translationHookOwnedByExploitPreventer(false));
        assertTrue(CompatibilityPolicy.translationHookOwnedByExploitPreventer(true));
    }

    @Test
    void withExploitPreventer_userTogglesStillApplyToNonOverlappingFeatures() {
        CompatibilityPolicy.Context ctx = new CompatibilityPolicy.Context(
            true, false, false,
            true, false, false, false, false, true, true
        );
        CompatibilityPolicy.Decisions d = CompatibilityPolicy.decide(ctx);
        assertTrue(d.spoofBrand());
        assertTrue(d.filterChannels());
        assertTrue(d.filterKnownPacks());
        assertFalse(d.isolatePackCache());
        assertFalse(d.stripModShaders());
        assertFalse(d.blockLocalPackUrls());
        assertFalse(d.translationProtection());
    }

    @Test
    void withoutExploitPreventer_userCanDisableUrlAndTranslation() {
        CompatibilityPolicy.Context ctx = new CompatibilityPolicy.Context(
            false, false, false,
            false, false, false, false, false, false, false
        );
        CompatibilityPolicy.Decisions d = CompatibilityPolicy.decide(ctx);
        assertFalse(d.spoofBrand());
        assertTrue(d.filterChannels());
        assertFalse(d.isolatePackCache());
        assertFalse(d.blockLocalPackUrls());
        assertFalse(d.translationProtection());
        assertFalse(d.stripModShaders());
    }
}
