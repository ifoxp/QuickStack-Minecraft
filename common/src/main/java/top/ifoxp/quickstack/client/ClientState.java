package top.ifoxp.quickstack.client;

import java.util.List;
import top.ifoxp.quickstack.network.Payloads;
import top.ifoxp.quickstack.server.LockManager;

/** What the client knows from the server: slot locks and the items in nearby containers. */
public final class ClientState {

    private static long lockMask = LockManager.DEFAULT_MASK;
    private static List<Payloads.NearbyCount> nearbyItems = List.of();
    private static int nearbyVersion;

    private ClientState() {
    }

    public static boolean isLocked(int slot) {
        return LockManager.isLocked(lockMask, slot);
    }

    public static void setLockMask(long mask) {
        lockMask = mask;
    }

    /** Optimistic local toggle so the overlay reacts at once; the server's sync confirms it. */
    public static void toggleLocal(int slot) {
        lockMask ^= 1L << slot;
    }

    public static List<Payloads.NearbyCount> nearbyItems() {
        return nearbyItems;
    }

    public static int nearbyVersion() {
        return nearbyVersion;
    }

    public static void setNearbyItems(List<Payloads.NearbyCount> items) {
        nearbyItems = items;
        nearbyVersion++;
    }

    public static void reset() {
        lockMask = LockManager.DEFAULT_MASK;
        nearbyItems = List.of();
        nearbyVersion++;
    }
}
