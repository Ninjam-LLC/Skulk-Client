package com.ariesninja.skulkpk.client.license;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LicenseManager {
    private static final String LICENSE_FILE_NAME = "skulk_license.json";
    private static final Gson GSON = new Gson();

    public static boolean hasStoredLicense() {
        return getStoredLicense() != null;
    }

    public static String getStoredLicense() {
        try {
            Path configDir = MinecraftClient.getInstance().runDirectory.toPath().resolve("config");
            Path licenseFile = configDir.resolve(LICENSE_FILE_NAME);

            if (Files.exists(licenseFile)) {
                String content = Files.readString(licenseFile);
                JsonObject licenseData = GSON.fromJson(content, JsonObject.class);

                if (licenseData.has("license")) {
                    return licenseData.get("license").getAsString();
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load license: " + e.getMessage());
        }
        return null;
    }

    public static void storeLicense(String username, String license) {
        try {
            Path configDir = MinecraftClient.getInstance().runDirectory.toPath().resolve("config");
            Files.createDirectories(configDir);

            Path licenseFile = configDir.resolve(LICENSE_FILE_NAME);

            JsonObject licenseData = new JsonObject();
            licenseData.addProperty("username", username);
            licenseData.addProperty("license", license);
            licenseData.addProperty("timestamp", System.currentTimeMillis());

            Files.writeString(licenseFile, GSON.toJson(licenseData));

        } catch (IOException e) {
            System.err.println("Failed to store license: " + e.getMessage());
        }
    }

    public static void clearLicense() {
        try {
            Path configDir = MinecraftClient.getInstance().runDirectory.toPath().resolve("config");
            Path licenseFile = configDir.resolve(LICENSE_FILE_NAME);
            Files.deleteIfExists(licenseFile);
        } catch (IOException e) {
            System.err.println("Failed to clear license: " + e.getMessage());
        }
    }
}
