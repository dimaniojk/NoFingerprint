package io.github.dimaniojk.nofingerprint.lang;

/**
 * String keys for NoFingerprint UI text. Values are intentionally namespaced like vanilla
 * translation keys but live in a private lookup table (see {@link NoFingerprintLang}) that
 * bypasses the vanilla {@code Language} map. Server resource packs cannot override
 * these strings because they are never exposed to the vanilla resource manager.
 */
public final class NoFingerprintStrings {
    private NoFingerprintStrings() {}

    public static final String CONFIG_TITLE = "nofingerprint.config.title";

    public static final String TAB_PROTECTION = "nofingerprint.tab.protection";
    public static final String TAB_ACCOUNTS = "nofingerprint.tab.accounts";
    public static final String TAB_WHITELIST = "nofingerprint.tab.whitelist";
    public static final String TAB_MISC = "nofingerprint.tab.misc";

    // Settings-menu section headers (values carry their own §-formatting).
    public static final String SECTION_CLIENT_BRAND = "nofingerprint.section.clientBrand";
    public static final String SECTION_RESOURCE_PACK = "nofingerprint.section.resourcePack";
    public static final String SECTION_KEY_RESOLUTION = "nofingerprint.section.keyResolution";
    public static final String SECTION_PRIVACY = "nofingerprint.section.privacy";
    public static final String SECTION_ALERTS = "nofingerprint.section.alerts";
    public static final String SECTION_ACCOUNTS = "nofingerprint.section.accounts";
    public static final String SECTION_SAVED_ACCOUNTS = "nofingerprint.section.savedAccounts";
    public static final String SECTION_ADD_ACCOUNT = "nofingerprint.section.addAccount";
    public static final String SECTION_MOD_WHITELIST = "nofingerprint.section.modWhitelist";
    public static final String SECTION_INSTALLED_MODS = "nofingerprint.section.installedMods";
    public static final String SECTION_RESTART_WARNING = "nofingerprint.section.restartWarning";
    public static final String SECTION_DEBUG = "nofingerprint.section.debug";
    public static final String ACCOUNT_CURRENT = "nofingerprint.account.current";
    public static final String ACCOUNT_STORAGE_WARNING = "nofingerprint.account.storageWarning";
    public static final String WHITELIST_SUFFIX_CHANNELS = "nofingerprint.whitelist.suffix.channels";

    public static final String OPTION_SPOOF_AS_VANILLA = "nofingerprint.option.spoofAsVanilla";
    public static final String OPTION_SPOOF_AS_VANILLA_TOOLTIP = "nofingerprint.option.spoofAsVanilla.tooltip";

    public static final String OPTION_WHITELIST_MODE = "nofingerprint.option.whitelistMode";
    public static final String OPTION_WHITELIST_MODE_LOCKED_TOOLTIP = "nofingerprint.option.whitelistMode.locked.tooltip";
    public static final String WHITELIST_SEARCH = "nofingerprint.whitelist.search";

    public static final String WHITELIST_MODE_BLOCK_ALL = "nofingerprint.whitelist.mode.blockAll";
    public static final String WHITELIST_MODE_AUTO = "nofingerprint.whitelist.mode.auto";
    public static final String WHITELIST_MODE_CUSTOM = "nofingerprint.whitelist.mode.custom";
    public static final String WHITELIST_MODE_BLOCK_ALL_TOOLTIP = "nofingerprint.whitelist.mode.blockAll.tooltip";
    public static final String WHITELIST_MODE_AUTO_TOOLTIP = "nofingerprint.whitelist.mode.auto.tooltip";
    public static final String WHITELIST_MODE_CUSTOM_TOOLTIP = "nofingerprint.whitelist.mode.custom.tooltip";

    public static final String EP_MANAGED_HEADER = "nofingerprint.ep.managed.header";
    public static final String EP_MANAGED_TOOLTIP = "nofingerprint.ep.managed.tooltip";
    public static final String EP_URL_OVERLAP_TOOLTIP = "nofingerprint.ep.urlOverlap.tooltip";
    public static final String EP_TRANSLATION_OVERLAP_TOOLTIP = "nofingerprint.ep.translationOverlap.tooltip";

    public static final String DIAG_BRAND_CHANNELS_VISIBLE = "nofingerprint.diag.brandChannelsVisible";
    public static final String DIAG_AUTO_WHITELIST = "nofingerprint.diag.autoWhitelist";

    // Generic "managed by another mod" strings; %s is the managing mod's display name.
    public static final String COMPAT_MANAGED_HEADER = "nofingerprint.compat.managed.header";
    public static final String COMPAT_MANAGED_TOOLTIP = "nofingerprint.compat.managed.tooltip";

    public static final String OPTION_ISOLATE_PACK_CACHE = "nofingerprint.option.isolatePackCache";
    public static final String OPTION_BLOCK_LOCAL_PACK_URLS = "nofingerprint.option.blockLocalPackUrls";
    public static final String OPTION_STRIP_MOD_SHADERS = "nofingerprint.option.stripModShaders";
    public static final String OPTION_CLEAR_CACHE = "nofingerprint.option.clearCache";
    public static final String OPTION_KEY_RESOLUTION_SPOOFING = "nofingerprint.option.keyResolutionSpoofing";
    public static final String OPTION_FAKE_DEFAULT_KEYBINDS = "nofingerprint.option.fakeDefaultKeybinds";
    public static final String OPTION_METEOR_FIX = "nofingerprint.option.meteorFix";
    public static final String OPTION_PACK_STRIP_MODE = "nofingerprint.option.packStripMode";

    public static final String PACKSTRIP_MODE_MANUAL = "nofingerprint.packstrip.mode.manual";
    public static final String PACKSTRIP_MODE_ASK = "nofingerprint.packstrip.mode.ask";
    public static final String PACKSTRIP_MODE_ALWAYS_ON = "nofingerprint.packstrip.mode.alwaysOn";
    public static final String PACKSTRIP_MODE_MANUAL_TOOLTIP = "nofingerprint.packstrip.mode.manual.tooltip";
    public static final String PACKSTRIP_MODE_ASK_TOOLTIP = "nofingerprint.packstrip.mode.ask.tooltip";
    public static final String PACKSTRIP_MODE_ALWAYS_ON_TOOLTIP = "nofingerprint.packstrip.mode.alwaysOn.tooltip";

    public static final String PACKSTRIP_TITLE = "nofingerprint.packstrip.title";
    public static final String PACKSTRIP_REQUIRED = "nofingerprint.packstrip.required";
    public static final String PACKSTRIP_OPTIONAL = "nofingerprint.packstrip.optional";
    public static final String PACKSTRIP_STRIPPED = "nofingerprint.packstrip.stripped";
    public static final String PACKSTRIP_CONTINUE = "nofingerprint.packstrip.continue";
    public static final String PACKSTRIP_LOAD_REAL = "nofingerprint.packstrip.loadReal";

    public static final String OPTION_CHAT_SIGNING = "nofingerprint.option.chatSigning";
    public static final String OPTION_DISABLE_TELEMETRY = "nofingerprint.option.disableTelemetry";

    public static final String OPTION_SHOW_ALERTS = "nofingerprint.option.showAlerts";
    public static final String OPTION_SHOW_TOASTS = "nofingerprint.option.showToasts";
    public static final String OPTION_LOG_DETECTIONS = "nofingerprint.option.logDetections";
    public static final String OPTION_DEBUG_ALERTS = "nofingerprint.option.debugAlerts";
    public static final String OPTION_DEBUG_COMMAND = "nofingerprint.option.debugCommand";
    public static final String OPTION_HIDE_INSECURE_INDICATORS = "nofingerprint.option.hideInsecureIndicators";
    public static final String OPTION_HIDE_MODIFIED_INDICATORS = "nofingerprint.option.hideModifiedIndicators";
    public static final String OPTION_HIDE_SYSTEM_MSG_INDICATORS = "nofingerprint.option.hideSystemMsgIndicators";
    public static final String OPTION_HIDE_WARNING_TOAST = "nofingerprint.option.hideWarningToast";

    public static final String BRAND_VANILLA = "nofingerprint.brand.vanilla";
    public static final String BRAND_FABRIC = "nofingerprint.brand.fabric";

    public static final String TRANSLATION_MODE_SPOOF = "nofingerprint.translationMode.spoof";
    public static final String TRANSLATION_MODE_BLOCK = "nofingerprint.translationMode.block";

    public static final String ACCOUNT_ADD_SESSION = "nofingerprint.account.addSession";
    public static final String ACCOUNT_ADD_SESSION_TOOLTIP = "nofingerprint.account.addSession.tooltip";
    public static final String ACCOUNT_ADD_OFFLINE = "nofingerprint.account.addOffline";
    public static final String ACCOUNT_ADD_OFFLINE_TOOLTIP = "nofingerprint.account.addOffline.tooltip";

    public static final String UPDATE_TITLE = "nofingerprint.update.title";
    public static final String UPDATE_MESSAGE = "nofingerprint.update.message";
    public static final String UPDATE_CURRENT = "nofingerprint.update.current";
    public static final String UPDATE_LATEST = "nofingerprint.update.latest";
    public static final String UPDATE_DOWNLOAD = "nofingerprint.update.download";
    public static final String UPDATE_SKIP = "nofingerprint.update.skip";
    public static final String UPDATE_CANCEL = "nofingerprint.update.cancel";

    public static final String TAMPER_TITLE = "nofingerprint.tamper.title";
    public static final String TAMPER_WARNING = "nofingerprint.tamper.warning";
    public static final String TAMPER_MISMATCH = "nofingerprint.tamper.mismatch";
    public static final String TAMPER_MALICIOUS = "nofingerprint.tamper.malicious";
    public static final String TAMPER_COMPROMISED = "nofingerprint.tamper.compromised";
    public static final String TAMPER_ACTION = "nofingerprint.tamper.action";
    public static final String TAMPER_EXPECTED = "nofingerprint.tamper.expected";
    public static final String TAMPER_ACTUAL = "nofingerprint.tamper.actual";
    public static final String TAMPER_DOWNLOAD = "nofingerprint.tamper.download";
    public static final String TAMPER_DISMISS_PERMANENT = "nofingerprint.tamper.dismiss_permanent";

    // Chat alerts + toast titles (user-visible runtime messages)
    public static final String ALERT_TRACKPACK_PATTERN = "nofingerprint.alert.trackpack.pattern";
    public static final String TOAST_TRACKPACK = "nofingerprint.toast.trackpack";
    public static final String ALERT_SHADER_STRIP = "nofingerprint.alert.shaderStrip";
    public static final String ALERT_SHADER_STRIP_MULTI = "nofingerprint.alert.shaderStrip.multi";
    public static final String TOAST_SHADER_STRIP = "nofingerprint.toast.shaderStrip";
    public static final String ALERT_PORTSCAN_BLOCKED = "nofingerprint.alert.portscan.blocked";
    public static final String ALERT_PORTSCAN_DETECTED = "nofingerprint.alert.portscan.detected";
    public static final String TOAST_PORTSCAN = "nofingerprint.toast.portscan";
    public static final String ALERT_PORTSCAN_SUMMARY_SINGLE = "nofingerprint.alert.portscan.summary.single";
    public static final String ALERT_PORTSCAN_SUMMARY_MULTI = "nofingerprint.alert.portscan.summary.multi";
    public static final String ALERT_PORTSCAN_SUMMARY_MORE = "nofingerprint.alert.portscan.summary.more";

    // Key-resolution probe alert + toast + one-time hint
    public static final String ALERT_KEYRESOLUTION = "nofingerprint.alert.keyresolution";
    public static final String TOAST_KEYRESOLUTION = "nofingerprint.toast.keyresolution";
    public static final String HINT_ALERTS_CAN_BE_DISABLED = "nofingerprint.hint.alertsCanBeDisabled";

    // /nofingerprint command output
    public static final String COMMAND_HELP_HEADER = "nofingerprint.command.help.header";
    public static final String COMMAND_HELP_INFO = "nofingerprint.command.help.info";
    public static final String COMMAND_HELP_CHANNELS = "nofingerprint.command.help.channels";

    public static final String COMMAND_OVERVIEW_HEADER = "nofingerprint.command.overview.header";
    public static final String COMMAND_OVERVIEW_TOTAL_MODS = "nofingerprint.command.overview.totalMods";
    public static final String COMMAND_OVERVIEW_VANILLA_KEYS = "nofingerprint.command.overview.vanillaKeys";
    public static final String COMMAND_OVERVIEW_SERVER_KEYS = "nofingerprint.command.overview.serverKeys";
    public static final String COMMAND_OVERVIEW_TOTAL_KEYS = "nofingerprint.command.overview.totalKeys";
    public static final String COMMAND_OVERVIEW_TOTAL_KEYBINDS = "nofingerprint.command.overview.totalKeybinds";
    public static final String COMMAND_OVERVIEW_TOTAL_SHADERS = "nofingerprint.command.overview.totalShaders";
    public static final String COMMAND_OVERVIEW_TOTAL_KNOWN_PACKS = "nofingerprint.command.overview.totalKnownPacks";
    public static final String COMMAND_OVERVIEW_MODS_HEADER = "nofingerprint.command.overview.modsHeader";
    public static final String COMMAND_OVERVIEW_NO_MODS = "nofingerprint.command.overview.noMods";
    public static final String COMMAND_OVERVIEW_USE_INFO = "nofingerprint.command.overview.useInfo";

    public static final String COMMAND_INFO_NOT_FOUND = "nofingerprint.command.info.notFound";
    public static final String COMMAND_INFO_USE_LIST = "nofingerprint.command.info.useList";
    public static final String COMMAND_INFO_HEADER = "nofingerprint.command.info.header";
    public static final String COMMAND_INFO_ID = "nofingerprint.command.info.id";
    public static final String COMMAND_INFO_TRANSLATION_KEYS = "nofingerprint.command.info.translationKeys";
    public static final String COMMAND_INFO_NONE = "nofingerprint.command.info.none";
    public static final String COMMAND_INFO_MORE = "nofingerprint.command.info.more";
    public static final String COMMAND_INFO_KEYBINDS = "nofingerprint.command.info.keybinds";
    public static final String COMMAND_INFO_CHANNELS = "nofingerprint.command.info.channels";
    public static final String COMMAND_INFO_SHADERS = "nofingerprint.command.info.shaders";
    public static final String COMMAND_INFO_KNOWN_PACKS = "nofingerprint.command.info.knownPacks";
    public static final String COMMAND_INFO_JIJ = "nofingerprint.command.info.jij";
    public static final String WHITELIST_REQUIRING_FALLBACK = "nofingerprint.whitelist.requiringFallback";
    public static final String WHITELIST_SUFFIX_REQUIRED = "nofingerprint.whitelist.suffix.required";
    public static final String WHITELIST_TOOLTIP_REQUIRED_BY = "nofingerprint.whitelist.tooltip.requiredBy";
    public static final String COMMAND_INFO_STATUS_ALLOWED_AUTO = "nofingerprint.command.info.statusAllowedAuto";
    public static final String COMMAND_INFO_STATUS_BLOCKED_AUTO = "nofingerprint.command.info.statusBlockedAuto";
    public static final String COMMAND_INFO_STATUS_ALLOWED_CUSTOM = "nofingerprint.command.info.statusAllowedCustom";
    public static final String COMMAND_INFO_STATUS_ALLOWED_DEP = "nofingerprint.command.info.statusAllowedDep";
    public static final String COMMAND_INFO_STATUS_BLOCKED_CUSTOM = "nofingerprint.command.info.statusBlockedCustom";
    public static final String COMMAND_INFO_STATUS_BLOCKED_OFF = "nofingerprint.command.info.statusBlockedOff";

    public static final String COMMAND_CHANNELS_HEADER = "nofingerprint.command.channels.header";
    public static final String COMMAND_CHANNELS_NONE = "nofingerprint.command.channels.none";
    public static final String COMMAND_CHANNELS_TOTAL = "nofingerprint.command.channels.total";
    public static final String COMMAND_CHANNELS_LEGEND = "nofingerprint.command.channels.legend";

    public static final String COMMAND_MODENTRY_KEYS = "nofingerprint.command.modEntry.keys";
    public static final String COMMAND_MODENTRY_KEYBINDS = "nofingerprint.command.modEntry.keybinds";
    public static final String COMMAND_MODENTRY_CHANNELS = "nofingerprint.command.modEntry.channels";
    public static final String COMMAND_MODENTRY_SHADERS = "nofingerprint.command.modEntry.shaders";
    public static final String COMMAND_MODENTRY_KNOWN_PACKS = "nofingerprint.command.modEntry.knownPacks";

    // AddAccountScreen (session token)
    public static final String ACCOUNT_SCREEN_SESSION_TITLE = "nofingerprint.account.screen.session.title";
    public static final String ACCOUNT_SCREEN_SESSION_LABEL = "nofingerprint.account.screen.sessionLabel";
    public static final String ACCOUNT_SCREEN_SESSION_HINT = "nofingerprint.account.screen.sessionHint";
    public static final String ACCOUNT_SCREEN_REFRESH_LABEL = "nofingerprint.account.screen.refreshLabel";
    public static final String ACCOUNT_SCREEN_REFRESH_HINT = "nofingerprint.account.screen.refreshHint";
    public static final String ACCOUNT_SCREEN_ADD_BUTTON = "nofingerprint.account.screen.addButton";
    public static final String ACCOUNT_SCREEN_CANCEL_BUTTON = "nofingerprint.account.screen.cancelButton";
    public static final String ACCOUNT_ERROR_EMPTY_TOKEN = "nofingerprint.account.error.emptyToken";
    public static final String ACCOUNT_STATUS_VALIDATING = "nofingerprint.account.status.validating";
    public static final String ACCOUNT_SUCCESS_ADDED = "nofingerprint.account.success.added";
    public static final String ACCOUNT_SUCCESS_REFRESH_SUFFIX = "nofingerprint.account.success.refreshSuffix";
    public static final String ACCOUNT_ERROR_INVALID_TOKEN = "nofingerprint.account.error.invalidToken";

    // AddCrackedAccountScreen (offline)
    public static final String ACCOUNT_SCREEN_OFFLINE_TITLE = "nofingerprint.account.screen.offline.title";
    public static final String ACCOUNT_SCREEN_USERNAME_LABEL = "nofingerprint.account.screen.usernameLabel";
    public static final String ACCOUNT_SCREEN_USERNAME_HINT = "nofingerprint.account.screen.usernameHint";
    public static final String ACCOUNT_ERROR_EMPTY_USERNAME = "nofingerprint.account.error.emptyUsername";
    public static final String ACCOUNT_STATUS_ADDING = "nofingerprint.account.status.adding";
    public static final String ACCOUNT_SUCCESS_ADDED_OFFLINE = "nofingerprint.account.success.addedOffline";
    public static final String ACCOUNT_ERROR_FAILED_ADD = "nofingerprint.account.error.failedToAdd";

    // Multiplayer-screen NoFingerprint button
    public static final String BUTTON_SETTINGS_TOOLTIP = "nofingerprint.button.settings.tooltip";

    // Config screen tooltips and section buttons
    public static final String BUTTON_RESET_TOOLTIP = "nofingerprint.button.reset.tooltip";
    public static final String BUTTON_RESET_DISABLED_TOOLTIP = "nofingerprint.button.reset.disabled.tooltip";
    public static final String VERSION_TOOLTIP_OUTDATED = "nofingerprint.version.tooltip.outdated";
    public static final String VERSION_TOOLTIP_UPTODATE = "nofingerprint.version.tooltip.upToDate";

    public static final String TOOLTIP_ISOLATE_PACK_CACHE = "nofingerprint.option.isolatePackCache.tooltip";
    public static final String TOOLTIP_BLOCK_LOCAL_PACK_URLS = "nofingerprint.option.blockLocalPackUrls.tooltip";
    public static final String TOOLTIP_STRIP_MOD_SHADERS = "nofingerprint.option.stripModShaders.tooltip";
    public static final String TOOLTIP_CLEAR_CACHE = "nofingerprint.option.clearCache.tooltip";
    public static final String TOOLTIP_KEY_RESOLUTION_SPOOFING = "nofingerprint.option.keyResolutionSpoofing.tooltip";
    public static final String TOOLTIP_FAKE_DEFAULT_KEYBINDS = "nofingerprint.option.fakeDefaultKeybinds.tooltip";
    public static final String TOOLTIP_METEOR_FIX = "nofingerprint.option.meteorFix.tooltip";
    public static final String TOOLTIP_DISABLE_TELEMETRY = "nofingerprint.option.disableTelemetry.tooltip";
    public static final String TOOLTIP_SHOW_ALERTS = "nofingerprint.option.showAlerts.tooltip";
    public static final String TOOLTIP_SHOW_TOASTS = "nofingerprint.option.showToasts.tooltip";
    public static final String TOOLTIP_LOG_DETECTIONS = "nofingerprint.option.logDetections.tooltip";
    public static final String TOOLTIP_DEBUG_ALERTS = "nofingerprint.option.debugAlerts.tooltip";
    public static final String TOOLTIP_DEBUG_COMMAND = "nofingerprint.option.debugCommand.tooltip";

    // Account-management buttons
    public static final String BUTTON_REFRESH_ALL = "nofingerprint.button.refreshAll";
    public static final String BUTTON_REFRESHING = "nofingerprint.button.refreshing";
    public static final String BUTTON_REFRESH_TOOLTIP = "nofingerprint.button.refresh.tooltip";
    public static final String BUTTON_REMOVE = "nofingerprint.button.remove";
    public static final String BUTTON_REMOVE_TOOLTIP = "nofingerprint.button.remove.tooltip";
    public static final String BUTTON_IMPORT = "nofingerprint.button.import";
    public static final String BUTTON_IMPORT_TOOLTIP = "nofingerprint.button.import.tooltip";
    public static final String BUTTON_EXPORT = "nofingerprint.button.export";
    public static final String BUTTON_EXPORT_TOOLTIP = "nofingerprint.button.export.tooltip";

    // Whitelist tab bulk-action buttons
    public static final String BUTTON_ENABLE_ALL = "nofingerprint.button.enableAll";
    public static final String BUTTON_ENABLE_ALL_TOOLTIP = "nofingerprint.button.enableAll.tooltip";
    public static final String BUTTON_DISABLE_ALL = "nofingerprint.button.disableAll";
    public static final String BUTTON_DISABLE_ALL_TOOLTIP = "nofingerprint.button.disableAll.tooltip";

    // Chat-signing mode tooltips
    public static final String CHATSIGNING_SIGN_TOOLTIP = "nofingerprint.chatSigning.sign.tooltip";
    public static final String CHATSIGNING_OFF_TOOLTIP = "nofingerprint.chatSigning.off.tooltip";
}
