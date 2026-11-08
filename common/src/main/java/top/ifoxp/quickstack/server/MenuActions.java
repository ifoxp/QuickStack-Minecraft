package top.ifoxp.quickstack.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import top.ifoxp.quickstack.config.ServerConfig;
import top.ifoxp.quickstack.mixin.CompoundContainerAccessor;
import top.ifoxp.quickstack.network.MenuAction;
import top.ifoxp.quickstack.network.Payloads;
import top.ifoxp.quickstack.platform.Services;
import top.ifoxp.quickstack.sort.SortMode;
import top.ifoxp.quickstack.sort.Sorter;

/** The buttons on container screens and the inventory. Everything goes through menu slots so vanilla sync just works. */
public final class MenuActions {

    private MenuActions() {
    }

    public static void handle(Payloads.MenuActionRequest request, ServerPlayer player) {
        AbstractContainerMenu menu = player.containerMenu;
        if (player.isSpectator() || !menu.stillValid(player) || !menu.getCarried().isEmpty()) {
            return;
        }
        SortMode sortMode = SortMode.byId(request.sortMode());
        if (request.action() == MenuAction.SORT_INVENTORY) {
            sortInventory(player, sortMode);
            menu.broadcastChanges();
            return;
        }
        List<Slot> containerSlots = storageSlots(menu, player);
        if (containerSlots.isEmpty()) {
            return;
        }
        switch (request.action()) {
            case DEPOSIT_MATCHING -> deposit(player, containerSlots, true);
            case DEPOSIT_ALL -> deposit(player, containerSlots, false);
            case RESTOCK -> restock(player, containerSlots, Math.max(0, request.keepInContainer()));
            case LOOT_ALL -> lootAll(player, containerSlots);
            case SORT_CONTAINER -> sortSlots(containerSlots, sortMode);
            case TOGGLE_EXCLUDED -> toggleExcluded(player, containerSlots.getFirst().container);
            default -> {
            }
        }
        menu.broadcastChanges();
    }

    /**
     * Mouse wheel up over a slot: moves items from the slot onto the cursor, which is empty or already holds the same
     * item. The opposite (one item from the cursor into a slot) is a vanilla right click, sent by the client.
     */
    public static void takeFromSlot(Payloads.TakeFromSlot request, ServerPlayer player) {
        AbstractContainerMenu menu = player.containerMenu;
        if (player.isSpectator() || menu.containerId != request.containerId() || !menu.stillValid(player)
                || request.amount() <= 0 || !menu.isValidSlotIndex(request.slot())) {
            return;
        }
        Slot slot = menu.getSlot(request.slot());
        ItemStack carried = menu.getCarried();
        if (!slot.hasItem() || !slot.mayPickup(player)
                || (!carried.isEmpty() && !ItemStack.isSameItemSameComponents(carried, slot.getItem()))) {
            return;
        }
        int room = carried.isEmpty() ? slot.getItem().getMaxStackSize() : carried.getMaxStackSize() - carried.getCount();
        int amount = Math.min(request.amount(), room);
        if (amount <= 0) {
            return;
        }
        ItemStack taken = slot.safeTake(amount, amount, player);
        if (taken.isEmpty()) {
            return;
        }
        if (carried.isEmpty()) {
            menu.setCarried(taken);
        } else {
            carried.grow(taken.getCount());
        }
        menu.broadcastChanges();
    }

    /**
     * The container side of a storage menu, or an empty list when the menu is not plain storage (furnaces,
     * machines, crafting...). Vanilla chests, barrels, ender chests and shulker boxes qualify; a modded menu does
     * when all its other slots are plain slots of one large block entity container.
     */
    public static List<Slot> storageSlots(AbstractContainerMenu menu, net.minecraft.world.entity.player.Player player) {
        List<Slot> slots = new ArrayList<>();
        Container shared = null;
        boolean plain = true;
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory) {
                continue;
            }
            if (shared == null) {
                shared = slot.container;
            } else if (shared != slot.container) {
                plain = false;
            }
            if (slot.getClass() != Slot.class) {
                plain = false;
            }
            slots.add(slot);
        }
        if (menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu) {
            return slots;
        }
        ServerConfig config = ServerConfig.get();
        boolean storage = plain && shared instanceof BaseContainerBlockEntity
                && config.allowModdedContainers && slots.size() >= config.moddedContainerMinSlots;
        return storage ? slots : List.of();
    }

    /** Main-inventory slots of the menu, indexed by inventory slot (0-35), null where the menu shows none. */
    private static Slot[] playerSlots(ServerPlayer player) {
        Slot[] result = new Slot[Inventory.INVENTORY_SIZE];
        for (Slot slot : player.containerMenu.slots) {
            if (slot.container == player.getInventory() && slot.getContainerSlot() < Inventory.INVENTORY_SIZE) {
                result[slot.getContainerSlot()] = slot;
            }
        }
        return result;
    }

    private static void deposit(ServerPlayer player, List<Slot> containerSlots, boolean matchingOnly) {
        Set<Item> present = new HashSet<>();
        for (Slot slot : containerSlots) {
            if (slot.hasItem()) {
                present.add(slot.getItem().getItem());
            }
        }
        long locks = LockManager.mask(player);
        Slot[] mine = playerSlots(player);
        for (int i = 0; i < mine.length; i++) {
            Slot slot = mine[i];
            if (slot == null || !slot.hasItem() || LockManager.isLocked(locks, i)) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (matchingOnly && !present.contains(stack.getItem())) {
                continue;
            }
            ItemStack moving = stack.copy();
            Transfer.insertIntoSlots(containerSlots, moving);
            if (moving.getCount() != stack.getCount()) {
                slot.set(moving);
            }
        }
    }

    /** Takes items the player already carries out of the container, leaving {@code keep} of each kind behind. */
    private static void restock(ServerPlayer player, List<Slot> containerSlots, int keep) {
        Inventory inventory = player.getInventory();
        List<ItemStack> wanted = new ArrayList<>();
        for (ItemStack carried : inventory.getNonEquipmentItems()) {
            if (!carried.isEmpty() && wanted.stream().noneMatch(w -> ItemStack.isSameItemSameComponents(w, carried))) {
                wanted.add(carried.copyWithCount(1));
            }
        }
        for (ItemStack kind : wanted) {
            int total = 0;
            for (Slot slot : containerSlots) {
                if (ItemStack.isSameItemSameComponents(slot.getItem(), kind)) {
                    total += slot.getItem().getCount();
                }
            }
            int toTake = total - keep;
            // Take from the last slots first, so the kept items stay where the player put them.
            for (int i = containerSlots.size() - 1; i >= 0 && toTake > 0; i--) {
                Slot slot = containerSlots.get(i);
                ItemStack inside = slot.getItem();
                if (!ItemStack.isSameItemSameComponents(inside, kind) || !slot.mayPickup(player)) {
                    continue;
                }
                ItemStack taken = inside.copyWithCount(Math.min(toTake, inside.getCount()));
                int amount = taken.getCount();
                Transfer.insertIntoPlayer(inventory, taken);
                int moved = amount - taken.getCount();
                if (moved > 0) {
                    slot.remove(moved);
                    slot.setChanged();
                    toTake -= moved;
                }
                if (!taken.isEmpty()) {
                    return; // inventory is full
                }
            }
        }
    }

    private static void lootAll(ServerPlayer player, List<Slot> containerSlots) {
        Inventory inventory = player.getInventory();
        for (Slot slot : containerSlots) {
            if (!slot.hasItem() || !slot.mayPickup(player)) {
                continue;
            }
            ItemStack taken = slot.getItem().copy();
            int amount = taken.getCount();
            Transfer.insertIntoPlayer(inventory, taken);
            int moved = amount - taken.getCount();
            if (moved > 0) {
                slot.remove(moved);
                slot.setChanged();
            }
        }
    }

    public static void sortSlots(List<Slot> slots, SortMode mode) {
        List<ItemStack> stacks = new ArrayList<>(slots.size());
        for (Slot slot : slots) {
            stacks.add(slot.getItem());
        }
        List<ItemStack> sorted = Sorter.mergeAndSort(stacks, mode);
        for (int i = 0; i < slots.size(); i++) {
            slots.get(i).set(i < sorted.size() ? sorted.get(i) : ItemStack.EMPTY);
        }
    }

    /** Sorts the unlocked main-inventory slots: the three main rows first, then unlocked hotbar slots. */
    private static void sortInventory(ServerPlayer player, SortMode mode) {
        Inventory inventory = player.getInventory();
        long locks = LockManager.mask(player);
        List<Integer> indices = new ArrayList<>();
        for (int i = Inventory.SELECTION_SIZE; i < Inventory.INVENTORY_SIZE; i++) {
            if (!LockManager.isLocked(locks, i)) {
                indices.add(i);
            }
        }
        for (int i = 0; i < Inventory.SELECTION_SIZE; i++) {
            if (!LockManager.isLocked(locks, i)) {
                indices.add(i);
            }
        }
        List<ItemStack> stacks = new ArrayList<>(indices.size());
        for (int index : indices) {
            stacks.add(inventory.getItem(index));
        }
        List<ItemStack> sorted = Sorter.mergeAndSort(stacks, mode);
        for (int i = 0; i < indices.size(); i++) {
            inventory.setItem(indices.get(i), i < sorted.size() ? sorted.get(i) : ItemStack.EMPTY);
        }
        inventory.setChanged();
    }

    private static void toggleExcluded(ServerPlayer player, Container container) {
        List<BlockEntity> parts = new ArrayList<>();
        if (container instanceof CompoundContainer compound) {
            CompoundContainerAccessor accessor = (CompoundContainerAccessor) compound;
            if (accessor.quickstack$first() instanceof BlockEntity first) {
                parts.add(first);
            }
            if (accessor.quickstack$second() instanceof BlockEntity second) {
                parts.add(second);
            }
        } else if (container instanceof BlockEntity blockEntity) {
            parts.add(blockEntity);
        }
        if (parts.isEmpty()) {
            return;
        }
        boolean excluded = !Services.PLATFORM.isExcluded(parts.getFirst());
        for (BlockEntity part : parts) {
            Services.PLATFORM.setExcluded(part, excluded);
            part.setChanged();
        }
        player.displayClientMessage(Component.translatable(excluded ? "quickstack.message.excluded" : "quickstack.message.included"), true);
    }
}
