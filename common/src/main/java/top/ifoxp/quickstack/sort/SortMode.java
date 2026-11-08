package top.ifoxp.quickstack.sort;

public enum SortMode {
    /** Registry order, which keeps related items together (stone types, woods, tools...). */
    CATEGORY,
    NAME,
    /** Item types with the biggest total count first. */
    COUNT;

    private static final SortMode[] VALUES = values();

    public static SortMode byId(int id) {
        return VALUES[Math.clamp(id, 0, VALUES.length - 1)];
    }

    public SortMode next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public String translationKey() {
        return "quickstack.sort_mode." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
