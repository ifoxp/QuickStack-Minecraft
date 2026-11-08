package top.ifoxp.quickstack.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
            player.sendOverlayMessage(Component.translatable("quickstack.message.not_found"));
            return;
        }
        Services.PLATFORM.sendToPlayer(player, new Payloads.Highlight(found, HIGHLIGHT_TICKS, true));
        player.sendOverlayMessage(Component.translatable("quickstack.message.found", found.size()));
    }
}
