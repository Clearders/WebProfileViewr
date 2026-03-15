package org.exmple.webprofileviewer.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Formats and colorizes Bedwars statistics for display.
 *
 * This class handles the parsing and formatting of raw stats strings from the Hypixel API,
 * converting them into colored text components suitable for chat display.
 *
 * Design Philosophy:
 * - Separates formatting logic from command implementation
 * - Provides reusable methods for consistent stat display across commands
 * - Supports flexible color schemes for different display contexts
 */
public class StatsFormatter {

    /**
     * Parses a raw stats string and returns formatted Components for display.
     * Each line is split and formatted with label in AQUA and value in WHITE.
     *
     * Example input:
     *   "Final K/D: 1.5\nWins: 100\nLosses: 50"
     *
     * Example output:
     *   [AQUA]Final K/D:[WHITE] 1.5
     *   [AQUA]Wins:[WHITE] 100
     *   [AQUA]Losses:[WHITE] 50
     *
     * @param statsString the raw stats string from API (may contain multiple lines)
     * @return array of formatted Components, each representing one stat line
     */
    public static Component[] formatStatsLines(String statsString) {
        String[] lines = statsString.split("\\r?\\n");
        Component[] components = new Component[lines.length];

        for (int i = 0; i < lines.length; i++) {
            components[i] = formatSingleLine(lines[i]);
        }

        return components;
    }

    /**
     * Formats a single stats line with color coding.
     *
     * Format rules:
     * - If line contains ":", splits into label and value
     *   - Label (before ":") is colored AQUA
     *   - Value (after ":") is colored WHITE
     * - If line doesn't contain ":", entire line is colored WHITE
     *
     * @param line a single line of stats text
     * @return formatted Component with appropriate colors
     */
    public static Component formatSingleLine(String line) {
        if (line.contains(":")) {
            int colonIdx = line.indexOf(":");
            String label = line.substring(0, colonIdx).trim();
            String value = line.substring(colonIdx + 1).trim();

            // Build component: AQUA label + ": " + WHITE value
            MutableComponent component = Component.literal(label + ": ")
                    .withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(value).withStyle(ChatFormatting.WHITE));

            return component;
        } else {
            // Line without colon, display in WHITE
            return Component.literal(line).withStyle(ChatFormatting.WHITE);
        }
    }

    /**
     * Extracts a specific stat value from a raw stats string.
     *
     * Example:
     *   extractStatValue("Final K/D: 1.5\nWins: 100", "Final K/D:")
     *   returns "1.5"
     *
     * @param statsString the raw stats string from API
     * @param statLabel the label to search for (e.g., "Final K/D:")
     * @return the value string if found, or null if not found
     */
    public static String extractStatValue(String statsString, String statLabel) {
        int index = statsString.indexOf(statLabel);
        if (index < 0) {
            return null;
        }

        // Skip past the label to get to the value
        String rest = statsString.substring(index + statLabel.length());
        String firstLine = rest.split("\\r?\\n")[0].trim();

        return firstLine;
    }

    /**
     * Tries to parse a stat value as a double.
     * Used for numeric comparisons (e.g., K/D > 1.0).
     *
     * @param statValue the stat value string
     * @return the parsed double value, or Double.NaN if parsing fails
     */
    public static double parseStatAsDouble(String statValue) {
        try {
            return Double.parseDouble(statValue);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
