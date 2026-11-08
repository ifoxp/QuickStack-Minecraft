package top.ifoxp.quickstack.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import top.ifoxp.quickstack.config.ServerConfig;
import top.ifoxp.quickstack.network.Payloads;
import top.ifoxp.quickstack.platform.Services;

/** "Where did I put it?": makes the nearby containers holding any of the wanted items sparkle. */
public final class ItemFinder {

    private static final int HIGHLIGHT_TICKS = 100;

    private ItemFinder() {
    }

    /** Totals of every item kind in the containers within the search radius. */
    public static void sendSummary(ServerPlayer player) {
        int radius = ServerConfig.get().searchRadius;
        Map<Holder<Item>, Integer> totals = new LinkedHashMap<>();
        for (ContainerTarget target : ContainerScanner.find(player, radius, ContainerScanner.Purpose.READ)) {
            Container container = target.container();
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (!stack.isEmpty()) {
                    totals.merge(stack.getItemHolder(), stack.getCount(), Integer::sum);
                }
            }
        }
        List<Payloads.NearbyCount> items = new ArrayList<>(totals.size());
        totals.forEach((item, count) -> items.add(new Payloads.NearbyCount(item, count)));
        if (items.size() > 8192) {
            items.subList(8192, items.size()).clear();
        }
        Services.PLATFORM.sendToPlayer(player, new Payloads.StorageSummary(items, radius));
    }

    public static void find(ServerPlayer player, List<Holder<Item>> wanted) {
        if (wanted.isEmpty()) {
            return;
        }
        Set<Item> items = new HashSet<>();
        for (Holder<Item> holder : wanted) {
            items.add(holder.value());
        }
        List<BlockPos> found = new ArrayList<>();
        for (ContainerTarget target : ContainerScanner.find(player, ServerConfig.get().searchRadius, ContainerScanner.Purpose.READ)) {
            Container container = target.container();
            if (container.hasAnyOf(items)) {
                found.add(target.pos());
            }
        }
        if (found.isEmpty()) {
            player.displayClientMessage(Component.translatable("quickstack.message.not_found"), true);
            return;
        }
        Services.PLATFORM.sendToPlayer(player, new Payloads.Highlight(found, HIGHLIGHT_TICKS, true));
        player.displayClientMessage(Component.translatable("quickstack.message.found", found.size()), true);
    }
}
