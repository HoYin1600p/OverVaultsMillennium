package io.iridium.overvaults.millenium.util;

import net.minecraft.ChatFormatting;

import java.util.Locale;

public class PortalRankTextUtil {
    private static final int DEFAULT_RANK_COLOR = 0x00AAAA;

    public static PortalRankInfo fromPortalOpenText(String portalOpenText) {
        if (portalOpenText == null || portalOpenText.isBlank()) {
            return PortalRankInfo.unknown();
        }

        String rankSection = getParentheticalRankSection(portalOpenText);
        String rank = getRankFromParentheticalSection(rankSection);
        if (rank == null) {
            rank = getRankFromTranslationKey(portalOpenText);
        }

        if (rank == null) {
            return PortalRankInfo.unknown();
        }

        return new PortalRankInfo(rank, getFormattingColor(rankSection == null ? portalOpenText : rankSection));
    }

    private static String getParentheticalRankSection(String text) {
        int close = text.lastIndexOf(')');
        int open = close < 0 ? -1 : text.lastIndexOf('(', close);
        if (open < 0 || close <= open) {
            return null;
        }

        return text.substring(open + 1, close);
    }

    private static String getRankFromParentheticalSection(String section) {
        if (section == null) {
            return null;
        }

        String title = stripFormatting(section).trim();
        String lowerTitle = title.toLowerCase(Locale.ROOT);
        int tierIndex = lowerTitle.indexOf("-tier");
        if (tierIndex < 0) {
            tierIndex = lowerTitle.indexOf(" tier");
        }

        if (tierIndex > 0) {
            return title.substring(0, tierIndex).trim();
        }

        int rankIndex = lowerTitle.indexOf("-rank");
        if (rankIndex < 0) {
            rankIndex = lowerTitle.indexOf(" rank");
        }

        if (rankIndex > 0) {
            return title.substring(0, rankIndex).trim();
        }

        return null;
    }

    private static String getRankFromTranslationKey(String key) {
        String suffix = key.substring(key.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return switch (suffix) {
            case "splusplus" -> "S++";
            case "splus" -> "S+";
            case "s" -> "S";
            case "a" -> "A";
            case "b" -> "B";
            case "c" -> "C";
            case "d" -> "D";
            case "e" -> "E";
            default -> null;
        };
    }

    private static int getFormattingColor(String text) {
        if (text == null || text.isBlank()) {
            return DEFAULT_RANK_COLOR;
        }

        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) != '\u00A7') {
                continue;
            }

            if (Character.toLowerCase(text.charAt(i + 1)) == 'x') {
                Integer hexColor = readHexFormattingColor(text, i);
                if (hexColor != null) {
                    return hexColor;
                }
            }

            ChatFormatting formatting = ChatFormatting.getByCode(text.charAt(i + 1));
            if (formatting != null && formatting.getColor() != null) {
                return formatting.getColor();
            }
        }

        return DEFAULT_RANK_COLOR;
    }

    private static Integer readHexFormattingColor(String text, int sectionSignIndex) {
        if (sectionSignIndex + 13 >= text.length()) {
            return null;
        }

        StringBuilder hex = new StringBuilder(6);
        for (int offset = 2; offset <= 12; offset += 2) {
            if (text.charAt(sectionSignIndex + offset) != '\u00A7') {
                return null;
            }

            char digit = text.charAt(sectionSignIndex + offset + 1);
            if (Character.digit(digit, 16) < 0) {
                return null;
            }

            hex.append(digit);
        }

        return Integer.parseInt(hex.toString(), 16);
    }

    private static String stripFormatting(String text) {
        StringBuilder stripped = new StringBuilder();
        boolean skipNext = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (skipNext) {
                skipNext = false;
                continue;
            }

            if (c == '\u00A7') {
                skipNext = true;
                continue;
            }

            stripped.append(c);
        }

        return stripped.toString();
    }

    public record PortalRankInfo(String rank, int color) {
        public static PortalRankInfo unknown() {
            return new PortalRankInfo("Unknown", DEFAULT_RANK_COLOR);
        }
    }
}
