package org.cardanofoundation.tokenmetadata.registry.api.util;

/**
 * Utility for sanitizing user input before logging to prevent log injection (CWE-117).
 * Methods in this class strip all characters that could be used for log forging,
 * including newlines, carriage returns, and other control characters.
 */
public final class LogSanitizer {

    private LogSanitizer() {
    }

    /**
     * Sanitizes a string for safe inclusion in log output.
     * Removes all characters except alphanumeric, hyphens, underscores, dots, and spaces.
     *
     * @param input the untrusted input
     * @return a sanitized string safe for logging, or "null" if input is null
     */
    public static String sanitize(String input) {
        if (input == null) {
            return "null";
        }
        return input.replaceAll("[^a-zA-Z0-9._\\- ]", "");
    }

    /**
     * Sanitizes a hex string for safe inclusion in log output.
     * Only allows hexadecimal characters (0-9, a-f, A-F) and dots.
     *
     * @param input the untrusted hex input
     * @return a sanitized string safe for logging, or "null" if input is null
     */
    public static String sanitizeHex(String input) {
        if (input == null) {
            return "null";
        }
        return input.replaceAll("[^a-fA-F0-9.]", "");
    }

}
