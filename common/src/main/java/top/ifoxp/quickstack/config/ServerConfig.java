package top.ifoxp.quickstack.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side limits (config/quickstack-server.json). In singleplayer the integrated server reads the same
 * file, so the config screen edits it directly.
 */
public final class ServerConfig {

    private static final String FILE = "quickstack-server.json";
    private static ServerConfig instance = new ServerConfig();

    /** Containers within this many blocks of the player take part in quick stack. */
    public int quickStackRadius = 8;
    /** Upper bound on items landing in containers per server tick, across all players. */
    public int maxInsertsPerTick = 16;
    /** Ticks a player has to wait between two quick stacks to nearby containers. */
    public int cooldownTicks = 20;
    /** How fast flying items travel, in blocks per tick. */
    public double flightSpeed = 0.7;
    /** Crafting tables and the inventory grid may take ingredients from nearby containers. */
    public boolean craftFromNearby = true;
    public int craftRadius = 8;
    /** Radius of "find item in nearby containers". */
    public int searchRadius = 16;
    /** Seconds after a quick stack during which it can be undone. */
    public int undoSeconds = 10;
    /** Accept any container block entity with at least this many slots (modded chests), besides vanilla ones. */
    public boolean allowModdedContainers = true;
    public int moddedContainerMinSlots = 18;
    /** Block entity type ids that never take part, for example "somemod:trash_can". */
    public List<String> blockedContainerTypes = new ArrayList<>();

    public static ServerConfig get() {
        return instance;
    }

    public static void load() {
        instance = ConfigIO.read(FILE, ServerConfig.class, ServerConfig::new);
        instance.clamp();
        ConfigIO.write(FILE, instance);
    }

    public static void save() {
        instance.clamp();
        ConfigIO.write(FILE, instance);
    }

    private void clamp() {
        quickStackRadius = Math.clamp(quickStackRadius, 1, 32);
        craftRadius = Math.clamp(craftRadius, 1, 32);
        searchRadius = Math.clamp(searchRadius, 1, 48);
        maxInsertsPerTick = Math.clamp(maxInsertsPerTick, 1, 512);
        cooldownTicks = Math.clamp(cooldownTicks, 0, 1200);
        flightSpeed = Math.clamp(flightSpeed, 0.1, 5.0);
        undoSeconds = Math.clamp(undoSeconds, 0, 300);
        moddedContainerMinSlots = Math.clamp(moddedContainerMinSlots, 1, 1000);
        if (blockedContainerTypes == null) {
            blockedContainerTypes = new ArrayList<>();
        }
    }
}
