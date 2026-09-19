package io.github.dimaniojk.nofingerprint.config;

import io.github.dimaniojk.nofingerprint.lang.NoFingerprintLang;
import io.github.dimaniojk.nofingerprint.lang.NoFingerprintStrings;
//? if <1.20.2
/*import net.minecraft.client.gui.GuiGraphics;*/
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

//? if >=1.21.11 {
/*import net.minecraft.util.Util;*/
//?} else {
import net.minecraft.Util;
//?}

/**
 * Native Minecraft-style screen that notifies the user when a new version of NoFingerprint is available.
 * Provides three options: Download (opens browser), Skip This Version (skips only this version), Cancel (session dismiss).
 *
 * All text is rendered via StringWidget (not raw drawCenteredString) for compatibility
 * with the stratum-based rendering pipeline in 1.21.9+.
 */
public class UpdateScreen extends Screen {

    private final Screen parent;

    public UpdateScreen(Screen parent) {
        super(Component.literal(NoFingerprintLang.tr(NoFingerprintStrings.UPDATE_TITLE)));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Text labels (using StringWidget for cross-version rendering compatibility)
        addCenteredStringWidget(this.title, centerX, centerY - 50);
        addCenteredStringWidget(Component.literal(NoFingerprintLang.tr(NoFingerprintStrings.UPDATE_MESSAGE)), centerX, centerY - 28);
        addCenteredStringWidget(Component.literal(NoFingerprintLang.tr(NoFingerprintStrings.UPDATE_CURRENT, UpdateChecker.getCurrentVersion())), centerX, centerY - 14);
        addCenteredStringWidget(Component.literal(NoFingerprintLang.tr(NoFingerprintStrings.UPDATE_LATEST, UpdateChecker.getLatestVersion())), centerX, centerY);

        // Buttons stacked vertically, centered horizontally
        int buttonWidth = 200;
        int buttonHeight = 20;
        int buttonSpacing = 24;
        int firstButtonY = centerY + 15;

        // Download button (green text)
        this.addRenderableWidget(Button.builder(Component.literal(NoFingerprintLang.tr(NoFingerprintStrings.UPDATE_DOWNLOAD)), button -> {
            Util.getPlatform().openUri(UpdateChecker.getReleaseUrl());
            this.onClose();
        }).bounds(centerX - buttonWidth / 2, firstButtonY, buttonWidth, buttonHeight).build());

        // Skip This Version button
        this.addRenderableWidget(Button.builder(Component.literal(NoFingerprintLang.tr(NoFingerprintStrings.UPDATE_SKIP)), button -> {
            NoFingerprintConfig.getInstance().getSettings().setSkippedUpdateVersion(UpdateChecker.getLatestVersion());
            NoFingerprintConfig.getInstance().save();
            this.onClose();
        }).bounds(centerX - buttonWidth / 2, firstButtonY + buttonSpacing, buttonWidth, buttonHeight).build());

        // Cancel button
        this.addRenderableWidget(Button.builder(Component.literal(NoFingerprintLang.tr(NoFingerprintStrings.UPDATE_CANCEL)), button -> {
            this.onClose();
        }).bounds(centerX - buttonWidth / 2, firstButtonY + buttonSpacing * 2, buttonWidth, buttonHeight).build());
    }

    private void addCenteredStringWidget(Component text, int centerX, int y) {
        int textWidth = this.font.width(text);
        this.addRenderableWidget(new StringWidget(centerX - textWidth / 2, y, textWidth, 10, text, this.font));
    }

    //? if <1.20.2 {
    /*@Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 1.20.1 Screen.render does not auto-call renderBackground.
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    *///?}

    @Override
    public void onClose() {
        //? if >=26.2 {
        /*this.minecraft.setScreenAndShow(this.parent);*/
        //?} else {
        this.minecraft.setScreen(this.parent);
        //?}
    }
}
