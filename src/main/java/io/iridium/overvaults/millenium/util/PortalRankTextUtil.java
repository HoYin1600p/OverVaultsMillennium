package io.iridium.overvaults.millenium.util;

import net.minecraft.ChatFormatting;

import java.util.Locale;

public class PortalRankTextUtil {
    private static final int DEFAULT_RANK_COLOR = 0x00AAAA;

    public static PortalRankInfo fromPortalOpenText(String portalOpenText) {
        if (portalOpenText == null || portalOpenText.isBlank()) {
            return PortalRankInfo.unknown();
        }

        String rank = getRankFromParenthetical(portalOpenText);
        if (rank == null) {
            rank = getRankFromTranslationKey(portalOpenText);
        }

        if (rank == null) {
            return PortalRankInfo.unknown();
        }

        return new PortalRankInfo(rank, getFormattingColor(portalOpenText));
    }

    private static String getRankFromParenthetical(String text) {
        int close = text.lastIndexOf(')');
        int open = close < 0 ? -1 : text.lastIndexOf('(', close);
        if (open < 0 || close <= open) {
            return null;
        }

        String title = stripFormatting(text.substring(open + 1, close)).trim();
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
        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) != '\u00A7') {
                continue;
            }

            ChatFormatting formatting = ChatFormatting.getByCode(text.charAt(i + 1));
            if (formatting != null && formatting.getColor() != null) {
                return formatting.getColor();
            }
        }

        return DEFAULT_RANK_COLOR;
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
