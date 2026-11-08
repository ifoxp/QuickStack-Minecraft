package top.ifoxp.quickstack.server;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import top.ifoxp.quickstack.network.Payloads;
import top.ifoxp.quickstack.platform.Services;

/**
 * Locked ("favourite") slots of the main inventory, stored on the player as a 36-bit mask. Locked slots never
 * leave through quick stack, deposit or sorting. The hotbar starts locked; armour and offhand are never touched.
 */
public final class LockManager {

    public static final long HOTBAR_MASK = (1L << Inventory.getSelectionSize()) - 1;
    public static final long ALL_MASK = (1L << Inventory.INVENTORY_SIZE) - 1;
    public static final long DEFAULT_MASK = HOTBAR_MASK;

    private LockManager() {
    }

    public static boolean isLocked(long mask, int slot) {
        return slot >= 0 && slot < Inventory.INVENTORY_SIZE && (mask >>> slot & 1L) != 0;
    }

    public static long mask(Player player) {
        return Services.PLATFORM.getLockMask(player) & ALL_MASK;
    }

    public static void toggle(ServerPlayer player, int slot) {
        long mask = mask(player);
        if (slot == Payloads.ToggleLock.RESET_DEFAULT) {
            mask = DEFAULT_MASK;
        } else if (slot == Payloads.ToggleLock.UNLOCK_ALL) {
            mask = 0;
        } else if (slot >= 0 && slot < Inventory.INVENTORY_SIZE) {
            mask ^= 1L << slot;
        } else {
            return;
        }
        Services.PLATFORM.setLockMask(player, mask);
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        Services.PLATFORM.sendToPlayer(player, new Payloads.LockSync(mask(player)));
    }
}
