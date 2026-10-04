package com.dafacha.inventory.application.support;

/** Helpers shared by the paginated list use cases. */
public final class SearchSupport {

    public static final int MAX_PAGE_SIZE = 100;

    private SearchSupport() {
    }

    public static int safePage(int page) {
        return Math.max(page, 0);
    }

    public static int safeSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    /**
     * Turns free text into a contains-style ILIKE pattern, escaping the LIKE wildcards so a user typing
     * "50%" searches for the literal text. Returns null for blank input (meaning "no filter").
     */
    public static String likePattern(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String escaped = search.trim()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    /** Trims; blank becomes null. */
    public static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
