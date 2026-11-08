package top.ifoxp.quickstack.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractCraftingMenu;
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
    }

    /** True while the player crafts in a crafting grid and both the server and the player allow it. */
    public static boolean activeFor(Player player) {
        return player instanceof ServerPlayer serverPlayer
                && ServerConfig.get().craftFromNearby
                && !OPTED_OUT.contains(player.getUUID())
                && !serverPlayer.isSpectator()
                && player.containerMenu instanceof AbstractCraftingMenu;
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
                    if (!stack.isEmpty() && Inventory.isUsableForCrafting(stack)) {
                        counts.merge(stack.typeHolder(), stack.getCount(), Integer::sum);
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
    public static void account(Player player, StackedItemContents contents) {
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
                if (inside.isEmpty() || !inside.is(item) || !Inventory.isUsableForCrafting(inside)) {
                    continue;
                }
                ItemStack template = taken.isEmpty() ? existing : taken;
                if (!template.isEmpty() && !ItemStack.isSameItemSameComponents(template, inside)) {
                    continue;
                }
                ItemStack part = container.removeItem(i, Math.min(count, inside.getCount()));
                count -= part.getCount();
                changed = true;
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
}
