package top.ifoxp.quickstack.config;

import top.ifoxp.quickstack.sort.SortMode;

/** Per-player preferences (config/quickstack-client.json). */
public final class ClientConfig {

    /** Modded menus with at least this many plain slots of one container count as storage (buttons shown). */
    public static final int STORAGE_MIN_SLOTS = 18;

    private static final String FILE = "quickstack-client.json";
    private static ClientConfig instance = new ClientConfig();

    /** "Restock from chest" leaves this many of each item in the chest. */
    public int keepInChest = 1;
    /** Show the flying items; without it items go straight into the containers. */
    public boolean animation = true;
    public boolean showInventoryButtons = true;
    /** Where the inventory buttons go: a small tab above the window, or inside it. */
    public ButtonPlacement buttonPlacement = ButtonPlacement.TAB;
    public boolean showContainerButtons = true;
    /** Middle click on a container or the inventory sorts it. */
    public boolean middleClickSort = true;
    public SortMode sortMode = SortMode.CATEGORY;
    /** Sparkles around the containers that received items. */
    public boolean highlightTargets = true;
    /** The recipe book counts items in nearby containers. */
    public boolean craftFromNearby = true;

    public static ClientConfig get() {
        return instance;
    }

    public static void load() {
        instance = ConfigIO.read(FILE, ClientConfig.class, ClientConfig::new);
        instance.clamp();
        ConfigIO.write(FILE, instance);
    }

    public static void save() {
        instance.clamp();
        ConfigIO.write(FILE, instance);
    }

    private void clamp() {
        keepInChest = Math.clamp(keepInChest, 0, 64);
        if (sortMode == null) {
            sortMode = SortMode.CATEGORY;
        }
        if (buttonPlacement == null) {
            buttonPlacement = ButtonPlacement.TAB;
        }
    }

    public enum ButtonPlacement {
        /** A tab sticking out of the top edge of the window, off the inventory area. */
        TAB,
        /** Inside the window: under the crafting grid, or on the "Inventory" label row. */
        INSIDE;

        public String translationKey() {
            return "quickstack.button_placement." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
