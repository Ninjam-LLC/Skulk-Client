package com.ariesninja.skulkpk.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ChatMessageUtil {

    public enum MessageType {
        INFO(Formatting.WHITE),
        WARN(Formatting.YELLOW),
        ERROR(Formatting.RED),
        SUCCESS(Formatting.GREEN);

        private final Formatting color;

        MessageType(Formatting color) {
            this.color = color;
        }

        public Formatting getColor() {
            return this.color;
        }
    }

    private static final String MOD_PREFIX = "Skulk";
    private static final String ARROW = " > ";

    /**
     * Sends a formatted message to the player's chat
     * @param client The Minecraft client instance
     * @param message The message content
     * @param type The message type (INFO, WARN, ERROR, SUCCESS)
     */
    public static void sendMessage(MinecraftClient client, String message, MessageType type) {
        if (client.player == null) return;

        Text prefixText = Text.literal(MOD_PREFIX).formatted(Formatting.AQUA, Formatting.BOLD);
        Text arrowText = Text.literal(ARROW).formatted(Formatting.GRAY);
        Text messageText = Text.literal(message).formatted(type.getColor());

        Text fullMessage = Text.empty().append(prefixText).append(arrowText).append(messageText);
        client.player.sendMessage(fullMessage, false);
    }

    /**
     * Sends an info message (white text)
     */
    public static void sendInfo(MinecraftClient client, String message) {
        sendMessage(client, message, MessageType.INFO);
    }

    /**
     * Sends a warning message (yellow text)
     */
    public static void sendWarn(MinecraftClient client, String message) {
        sendMessage(client, message, MessageType.WARN);
    }

    /**
     * Sends an error message (red text)
     */
    public static void sendError(MinecraftClient client, String message) {
        sendMessage(client, message, MessageType.ERROR);
    }

    /**
     * Sends a success message (green text)
     */
    public static void sendSuccess(MinecraftClient client, String message) {
        sendMessage(client, message, MessageType.SUCCESS);
    }
}
