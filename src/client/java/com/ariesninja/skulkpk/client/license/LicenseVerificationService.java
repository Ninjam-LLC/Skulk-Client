package com.ariesninja.skulkpk.client.license;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class LicenseVerificationService {
    private static final String API_BASE_URL = "https://skulk-server.aries-powvalla.workers.dev/api/licenses/verify";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final Gson GSON = new Gson();

    public static CompletableFuture<LicenseVerificationResult> verifyLicense(String username, String license) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = String.format("%s/%s/%s", API_BASE_URL, username, license);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                JsonObject jsonResponse = GSON.fromJson(response.body(), JsonObject.class);

                if (response.statusCode() == 200) {
                    boolean isValid = jsonResponse.has("valid") && jsonResponse.get("valid").getAsBoolean();

                    return new LicenseVerificationResult(isValid, isValid ? "License verified successfully" : "Invalid license or license doesn't belong to this user.");
                } else {
                    if (response.statusCode() == 404) {
                        return new LicenseVerificationResult(false, "License invalid, or license doesn't belong to this user.");
                    } else if (response.statusCode() == 403) {
                        if (jsonResponse.has("reason") && Objects.equals(jsonResponse.get("reason").getAsString(), "License is revoked")) {
                            return new LicenseVerificationResult(false, "This license is banned.");
                        }
                        return new LicenseVerificationResult(false, "This license is expired.");
                    }
                    return new LicenseVerificationResult(false, "The server can't complete your verification.");
                }
            } catch (IOException | InterruptedException e) {
                return new LicenseVerificationResult(false, "Cannot connect to the license server.");
            } catch (Exception e) {
                return new LicenseVerificationResult(false, "An unexpected error occurred.");
            }
        });
    }

    public static class LicenseVerificationResult {
        private final boolean valid;
        private final String message;

        public LicenseVerificationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
