package top.ifoxp.quickstack.server;

import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Moving stacks into containers, slot lists and the player's inventory: merge into equal stacks first, then fill empty slots. */
public final class Transfer {

    private Transfer() {
    }

    public static boolean contains(Container container, Item item) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).is(item)) {
                return true;
            }
        }
        return false;
    }

    public static boolean fitsIn(ContainerTarget target, ItemStack stack) {
        return !target.isShulkerBox() || stack.getItem().canFitInsideContainerItems();
    }

    /**
     * Inserts into a world container. {@code stack} is reduced by what went in; returns how many items moved.
     * {@code simulated} receives the insert instead of the real container when not null (planning).
     */
    public static int insert(Container container, ItemStack stack, ItemStack[] simulated) {
        int before = stack.getCount();
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int i = 0; i < container.getContainerSize() && !stack.isEmpty(); i++) {
                ItemStack existing = simulated != null ? simulated[i] : container.getItem(i);
                boolean empty = existing.isEmpty();
                if (pass == 0 ? empty || !ItemStack.isSameItemSameComponents(existing, stack) : !empty) {
                    continue;
                }
                if (!container.canPlaceItem(i, stack)) {
                    continue;
                }
                int limit = Math.min(container.getMaxStackSize(stack), stack.getMaxStackSize());
                int room = limit - existing.getCount();
                if (room <= 0) {
                    continue;
                }
                int moved = Math.min(room, stack.getCount());
                ItemStack placed = empty ? stack.copyWithCount(moved) : existing.copyWithCount(existing.getCount() + moved);
                if (simulated != null) {
                    simulated[i] = placed;
                } else {
                    container.setItem(i, placed);
                }
                stack.shrink(moved);
            }
        }
        int moved = before - stack.getCount();
        if (moved > 0 && simulated == null) {
            container.setChanged();
        }
        return moved;
    }

    public static ItemStack[] snapshot(Container container) {
        ItemStack[] copy = new ItemStack[container.getContainerSize()];
        for (int i = 0; i < copy.length; i++) {
            copy[i] = container.getItem(i).copy();
        }
        return copy;
    }

    /** Inserts into menu slots (the open container side of a menu), respecting each slot's rules. */
    public static void insertIntoSlots(List<Slot> slots, ItemStack stack) {
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (Slot slot : slots) {
                if (stack.isEmpty()) {
                    break;
                }
                ItemStack existing = slot.getItem();
                boolean match = pass == 0
                        ? !existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)
                        : existing.isEmpty();
                if (match && slot.mayPlace(stack)) {
                    slot.safeInsert(stack);
                }
            }
        }
    }

    /**
     * Puts a stack into the player's main inventory: merges into equal stacks anywhere, then empty slots of the
     * main rows, then the hotbar. Whatever does not fit stays in {@code stack}.
     */
    public static void insertIntoPlayer(Inventory inventory, ItemStack stack) {
        List<ItemStack> items = inventory.items;
        for (int i = 0; i < items.size() && !stack.isEmpty(); i++) {
            ItemStack existing = items.get(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
                int room = Math.min(existing.getMaxStackSize(), inventory.getMaxStackSize(existing)) - existing.getCount();
                if (room > 0) {
                    int moved = Math.min(room, stack.getCount());
                    existing.grow(moved);
                    stack.shrink(moved);
                }
            }
        }
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            int from = pass == 0 ? Inventory.SELECTION_SIZE : 0;
            int to = pass == 0 ? items.size() : Inventory.SELECTION_SIZE;
            for (int i = from; i < to && !stack.isEmpty(); i++) {
                if (items.get(i).isEmpty()) {
                    int moved = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), inventory.getMaxStackSize(stack)));
                    items.set(i, stack.split(moved));
                }
            }
        }
        inventory.setChanged();
    }
}
