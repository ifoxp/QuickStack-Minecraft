package top.ifoxp.quickstack.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.ifoxp.quickstack.config.ServerConfig;
import top.ifoxp.quickstack.network.Payloads;
import top.ifoxp.quickstack.platform.Services;

/**
 * Crafting from nearby containers. The recipe book's server-side placement (see ServerPlaceRecipeMixin) counts the
 * items in containers around the player and pulls missing ingredients from them; the client gets a summary of
 * those items so its recipe book shows the recipes as craftable.
 */
public final class NearbyCrafting {

    private static final Set<UUID> OPTED_OUT = new HashSet<>();

    private NearbyCrafting() {
    }

    public static void request(ServerPlayer player, boolean enabled) {
        if (enabled) {
            OPTED_OUT.remove(player.getUUID());
        } else {
            OPTED_OUT.add(player.getUUID());
        }
        sendSummary(player);
    }

    public static void forget(UUID playerId) {
        OPTED_OUT.remove(playerId);
        LOANS.remove(playerId);
    }

    /** True while the player crafts in a crafting grid and both the server and the player allow it. */
    public static boolean activeFor(Player player) {
        return player instanceof ServerPlayer serverPlayer
                && ServerConfig.get().craftFromNearby
                && !OPTED_OUT.contains(player.getUUID())
                && !serverPlayer.isSpectator()
                && isCraftingGrid(player.containerMenu);
    }

    /** The crafting table and the inventory's 2x2 grid (one AbstractCraftingMenu from 1.21.2). */
    public static boolean isCraftingGrid(AbstractContainerMenu menu) {
        return menu instanceof CraftingMenu || menu instanceof InventoryMenu;
    }

    /** What the recipe book may use: not damaged, enchanted or renamed (Inventory.isUsableForCrafting from 1.21.2). */
    private static boolean usableForCrafting(ItemStack stack) {
        return !stack.isDamaged() && !stack.isEnchanted() && !stack.has(DataComponents.CUSTOM_NAME);
    }

    private static List<ContainerTarget> containers(ServerPlayer player) {
        return ContainerScanner.find(player, ServerConfig.get().craftRadius, ContainerScanner.Purpose.READ);
    }

    public static void sendSummary(ServerPlayer player) {
        Map<Holder<Item>, Integer> counts = new LinkedHashMap<>();
        if (activeFor(player)) {
            for (ContainerTarget target : containers(player)) {
                Container container = target.container();
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (!stack.isEmpty() && usableForCrafting(stack)) {
                        counts.merge(stack.getItemHolder(), stack.getCount(), Integer::sum);
                    }
                }
            }
        }
        List<Payloads.NearbyCount> entries = new ArrayList<>(counts.size());
        counts.forEach((item, count) -> entries.add(new Payloads.NearbyCount(item, count)));
        if (entries.size() > 4096) {
            entries.subList(4096, entries.size()).clear();
        }
        Services.PLATFORM.sendToPlayer(player, new Payloads.NearbyItems(entries));
    }

    /** Adds nearby items to what the recipe placement may use. */
    public static void account(Player player, StackedContents contents) {
        if (!activeFor(player)) {
            return;
        }
        for (ContainerTarget target : containers((ServerPlayer) player)) {
            Container container = target.container();
            for (int i = 0; i < container.getContainerSize(); i++) {
                contents.accountSimpleStack(container.getItem(i));
            }
        }
    }

    /**
     * Takes up to {@code count} of {@code item} from nearby containers, matching {@code existing} (the stack already
     * in the grid slot) when it is not empty. Returns what was taken, possibly empty.
     */
    public static ItemStack pull(Player player, Holder<Item> item, ItemStack existing, int count) {
        if (!activeFor(player) || count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = ItemStack.EMPTY;
        for (ContainerTarget target : containers((ServerPlayer) player)) {
            Container container = target.container();
            boolean changed = false;
            for (int i = 0; i < container.getContainerSize() && count > 0; i++) {
                ItemStack inside = container.getItem(i);
                if (inside.isEmpty() || !inside.is(item) || !usableForCrafting(inside)) {
                    continue;
                }
                ItemStack template = taken.isEmpty() ? existing : taken;
                if (!template.isEmpty() && !ItemStack.isSameItemSameComponents(template, inside)) {
                    continue;
                }
                ItemStack part = container.removeItem(i, Math.min(count, inside.getCount()));
                count -= part.getCount();
                changed = true;
                lend((ServerPlayer) player, target, part);
                if (taken.isEmpty()) {
                    taken = part;
                } else {
                    taken.grow(part.getCount());
                }
            }
            if (changed) {
                container.setChanged();
            }
            if (count <= 0) {
                break;
            }
        }
        return taken;
    }

    // ---- loans: ingredients taken from containers go back there when the grid is cleared or closed ----

    /** {@code count} items like {@code kind} were taken from the container at {@code pos} into a crafting grid. */
    private static final class Loan {
        final ServerLevel level;
        final BlockPos pos;
        final ItemStack kind;
        int count;

        Loan(ServerLevel level, BlockPos pos, ItemStack kind, int count) {
            this.level = level;
            this.pos = pos;
            this.kind = kind;
            this.count = count;
        }
    }

    private static final Map<UUID, List<Loan>> LOANS = new HashMap<>();

    private static void lend(ServerPlayer player, ContainerTarget target, ItemStack part) {
        List<Loan> loans = LOANS.computeIfAbsent(player.getUUID(), id -> new ArrayList<>());
        for (Loan loan : loans) {
            if (loan.pos.equals(target.pos()) && loan.level == player.serverLevel() && ItemStack.isSameItemSameComponents(loan.kind, part)) {
                loan.count += part.getCount();
                return;
            }
        }
        loans.add(new Loan(player.serverLevel(), target.pos(), part.copyWithCount(1), part.getCount()));
    }

    public static boolean hasLoans(ServerPlayer player) {
        List<Loan> loans = LOANS.get(player.getUUID());
        return loans != null && !loans.isEmpty();
    }

    /**
     * Puts as much of {@code stack} as was borrowed back into the containers it came from; {@code stack} shrinks by
     * what went back. The player's own items are counted as used first, so leftovers return to the containers.
     */
    public static void returnLoans(ServerPlayer player, ItemStack stack) {
        List<Loan> loans = LOANS.get(player.getUUID());
        if (loans == null || stack.isEmpty()) {
            return;
        }
        for (Iterator<Loan> it = loans.iterator(); it.hasNext() && !stack.isEmpty(); ) {
            Loan loan = it.next();
            if (!ItemStack.isSameItemSameComponents(loan.kind, stack)) {
                continue;
            }
            ContainerTarget target = ContainerScanner.resolve(loan.level, loan.pos);
            if (target != null) {
                ItemStack back = stack.copyWithCount(Math.min(loan.count, stack.getCount()));
                int moved = Transfer.insert(target.container(), back, null);
                stack.shrink(moved);
                loan.count -= moved;
            }
            if (loan.count <= 0 || target == null) {
                it.remove();
            }
        }
    }

    public static void clearLoans(UUID playerId) {
        LOANS.remove(playerId);
    }
}
