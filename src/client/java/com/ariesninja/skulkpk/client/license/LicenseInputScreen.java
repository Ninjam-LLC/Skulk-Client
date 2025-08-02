package com.ariesninja.skulkpk.client.license;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class LicenseInputScreen extends Screen {
    private static final int WINDOW_WIDTH = 350;
    private static final int WINDOW_HEIGHT = 200;

    private TextFieldWidget licenseField;
    private ButtonWidget submitButton;
    private ButtonWidget exitButton;
    private String statusMessage = "";
    private Formatting statusColor = Formatting.WHITE;
    private boolean isVerifying = false;
    private String initialErrorMessage;
    private String prefilledLicense;

    public LicenseInputScreen() {
        this(null, null);
    }

    public LicenseInputScreen(String errorMessage, String prefilledLicense) {
        super(Text.literal("Skulk License Verification"));
        this.initialErrorMessage = errorMessage;
        this.prefilledLicense = prefilledLicense;

        // Set initial error message if provided
        if (errorMessage != null) {
            this.setStatus(errorMessage, Formatting.RED);
        }
    }

    // Override to prevent default background rendering
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Don't call super.renderBackground() to prevent the blur effect
        // Instead, render our custom background here
        context.fill(0, 0, this.width, this.height, 0xCC000000);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // License input field
        this.licenseField = new TextFieldWidget(
            this.textRenderer,
            centerX - 150,
            centerY - 20,
            300,
            20,
            Text.literal("License Key")
        );
        this.licenseField.setMaxLength(64);
        this.licenseField.setPlaceholder(Text.literal("Enter your license key..."));
        this.addSelectableChild(this.licenseField);
        this.setInitialFocus(this.licenseField);

        // Pre-fill license field if data is provided
        if (this.prefilledLicense != null) {
            this.licenseField.setText(this.prefilledLicense);
        }

        // Submit button
        this.submitButton = ButtonWidget.builder(
            Text.literal("Verify License"),
            button -> this.verifyLicense()
        )
        .dimensions(centerX - 100, centerY + 20, 90, 20)
        .build();
        this.addDrawableChild(this.submitButton);

        // Exit button
        this.exitButton = ButtonWidget.builder(
            Text.literal("Exit Game"),
            button -> this.client.scheduleStop()
        )
        .dimensions(centerX + 10, centerY + 20, 90, 20)
        .build();
        this.addDrawableChild(this.exitButton);
    }

    private void verifyLicense() {
        String license = this.licenseField.getText().trim();
        if (license.isEmpty()) {
            this.setStatus("Please enter a license key", Formatting.RED);
            return;
        }

        String username = this.client.getSession().getUsername();
        this.isVerifying = true;
        this.submitButton.active = false;
        this.setStatus("Verifying license...", Formatting.YELLOW);

        LicenseVerificationService.verifyLicense(username, license)
            .thenAccept(result -> {
                this.client.execute(() -> {
                    this.isVerifying = false;
                    this.submitButton.active = true;

                    if (result.isValid()) {
                        LicenseManager.storeLicense(username, license);
                        this.setStatus("License verified successfully!", Formatting.GREEN);

                        // Close the screen after a short delay
                        new Thread(() -> {
                            try {
                                Thread.sleep(1500);
                                this.client.execute(() -> this.close());
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                    } else {
                        this.setStatus(result.getMessage(), Formatting.RED);
                    }
                });
            })
            .exceptionally(throwable -> {
                this.client.execute(() -> {
                    this.isVerifying = false;
                    this.submitButton.active = true;
                    this.setStatus("Verification failed: " + throwable.getMessage(), Formatting.RED);
                });
                return null;
            });
    }

    private void setStatus(String message, Formatting color) {
        this.statusMessage = message;
        this.statusColor = color;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // First render the background (this calls our overridden renderBackground method)
        this.renderBackground(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Draw window background
        context.fill(centerX - WINDOW_WIDTH/2, centerY - WINDOW_HEIGHT/2,
                    centerX + WINDOW_WIDTH/2, centerY + WINDOW_HEIGHT/2, 0xFF2C2C2C);

        // Draw window border
        context.drawBorder(centerX - WINDOW_WIDTH/2, centerY - WINDOW_HEIGHT/2,
                          WINDOW_WIDTH, WINDOW_HEIGHT, 0xFF4A90E2);

        // Draw title
        Text title = Text.literal("Skulk License Verification").formatted(Formatting.BOLD, Formatting.AQUA);
        context.drawCenteredTextWithShadow(this.textRenderer, title, centerX, centerY - 80, 0xFFFFFF);

        // Draw description
        Text description = Text.literal("This mod requires a valid license to continue.");
        context.drawCenteredTextWithShadow(this.textRenderer, description, centerX, centerY - 60, 0xAAAAAA);

        // Draw username with different colors
        String username = this.client.getSession().getUsername();
        String usernameLabel = "Username: ";

        // Calculate positions to keep them on the same line
        int usernameTextWidth = this.textRenderer.getWidth(usernameLabel);
        int totalWidth = usernameTextWidth + this.textRenderer.getWidth(username);
        int startX = centerX - totalWidth / 2;

        // Draw "Username: " in gray
        context.drawTextWithShadow(this.textRenderer, usernameLabel, startX, centerY - 45, 0xFFAAAAAA);

        // Draw the actual username in aqua
        context.drawTextWithShadow(this.textRenderer, username, startX + usernameTextWidth, centerY - 45, 0xFF55FFFF);

        // Draw text field background manually
        context.fill(centerX - 150, centerY - 20, centerX + 150, centerY, 0xFF1E1E1E);
        context.drawBorder(centerX - 150, centerY - 20, 300, 20, this.licenseField.isFocused() ? 0xFF4A90E2 : 0xFF666666);

        // Draw text field content manually
        String text = this.licenseField.getText();
        if (text.isEmpty() && !this.licenseField.isFocused()) {
            context.drawTextWithShadow(this.textRenderer, "Enter your license key...", centerX - 145, centerY - 15, 0xFF666666);
        } else {
            context.drawTextWithShadow(this.textRenderer, text, centerX - 145, centerY - 15, 0xFFFFFFFF);
        }

        // Draw cursor if focused
        if (this.licenseField.isFocused()) {
            int cursorX = centerX - 145 + this.textRenderer.getWidth(text);
            if ((System.currentTimeMillis() / 500) % 2 == 0) { // Blinking cursor
                context.drawVerticalLine(cursorX, centerY - 18, centerY - 2, 0xFFFFFFFF);
            }
        }

        // Render buttons only (not the text field since we draw it manually)
        this.submitButton.render(context, mouseX, mouseY, delta);
        this.exitButton.render(context, mouseX, mouseY, delta);

        // Draw status message last so it appears on top of everything
        if (!this.statusMessage.isEmpty()) {
            Text statusText = Text.literal(this.statusMessage).formatted(this.statusColor);
            context.drawCenteredTextWithShadow(this.textRenderer, statusText, centerX, centerY + 50, 0xFFFFFF);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // Prevent closing with ESC until license is verified
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { // Enter or Numpad Enter
            if (!this.isVerifying) {
                this.verifyLicense();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
