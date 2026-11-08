package top.ifoxp.quickstack.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.Prediction;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import top.ifoxp.quickstack.config.ServerConfig;
import top.ifoxp.quickstack.network.Payloads;
import top.ifoxp.quickstack.platform.Services;

/**
 * Quick stack to nearby containers, Terraria style.
 *
 * <p>On request the plan is made at once: unlocked inventory stacks are matched to nearby containers that already
 * hold the same item, with the inserts simulated on copies so nothing is sent to a full container. The items leave
 * the inventory immediately and wait in a queue while the client shows them flying; each one lands in its container
 * on its arrival tick, at most {@link ServerConfig#maxInsertsPerTick} per tick across all players. A landing
 * re-checks the container, and anything that no longer fits goes back to the player. Disconnects and server
 * shutdown land everything at once, so items are never lost in the air.
 */
public final class QuickStackManager {

    /** Ticks the items take to fly out and spread in front of the camera. */
    private static final int BURST_TICKS = 6;
    /** First launch towards the containers, after a short hover. */
    private static final int FIRST_LAUNCH = 14;
    private static final double HOVER_DISTANCE = 3.0;

    private static final PriorityQueue<Pending> QUEUE = new PriorityQueue<>((a, b) -> Long.compare(a.dueTick, b.dueTick));
    private static final PriorityQueue<LidEvent> LIDS = new PriorityQueue<>((a, b) -> Long.compare(a.tick(), b.tick()));
    private static final Map<UUID, Long> LAST_REQUEST = new HashMap<>();
    private static final Map<UUID, Batch> LAST_BATCH = new HashMap<>();
    private static long nextBatchId;

    private QuickStackManager() {
    }

    private record Pending(UUID playerId, long batchId, ServerLevel level, BlockPos pos, ItemStack stack, long dueTick) {
    }

    /** One quick stack, remembered for undo: what landed where, and what is still in the air. */
    private static final class Batch {
        final long id;
        final long createdTick;
        final List<Pending> landed = new ArrayList<>();

        Batch(long id, long createdTick) {
            this.id = id;
            this.createdTick = createdTick;
        }
    }

    private record Allocation(ContainerTarget target, ItemStack stack) {
    }

    public static void requestQuickStack(ServerPlayer player, boolean animate) {
        MinecraftServer server = player.level().getServer();
        long now = server.getTickCount();
        ServerConfig config = ServerConfig.get();
        Long last = LAST_REQUEST.get(player.getUUID());
        if (last != null && now - last < config.cooldownTicks) {
            return;
        }
        LAST_REQUEST.put(player.getUUID(), now);

        List<ContainerTarget> targets = ContainerScanner.find(player, config.quickStackRadius, ContainerScanner.Purpose.QUICK_STACK);
        if (targets.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("quickstack.message.no_containers"));
            return;
        }

        List<Allocation> allocations = plan(player, targets);
        if (allocations.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("quickstack.message.nothing_to_stack"));
            return;
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();

        Batch batch = new Batch(nextBatchId++, now);
        LAST_BATCH.put(player.getUUID(), batch);
        schedule(player, batch, allocations, animate, now);

        Set<BlockPos> containers = new HashSet<>();
        int items = 0;
        for (Allocation allocation : allocations) {
            containers.add(allocation.target().pos());
            items += allocation.stack().getCount();
        }
        player.sendOverlayMessage(Component.translatable("quickstack.message.stacked", items, containers.size()));
    }

    /** Takes the matched items out of the player's inventory and decides where each part goes. */
    private static List<Allocation> plan(ServerPlayer player, List<ContainerTarget> targets) {
        long locks = LockManager.mask(player);
        List<ItemStack> items = player.getInventory().getNonEquipmentItems();
        // Only the kinds the player can stack matter; skip indexing everything else in the containers.
        Set<Item> wanted = new HashSet<>();
        for (int slot = 0; slot < items.size(); slot++) {
            if (!items.get(slot).isEmpty() && !LockManager.isLocked(locks, slot)) {
                wanted.add(items.get(slot).getItem());
            }
        }
        Map<Item, List<ContainerTarget>> byItem = new HashMap<>();
        for (ContainerTarget target : targets) {
            Item last = null;
            for (int i = 0; i < target.container().getContainerSize(); i++) {
                Item item = target.container().getItem(i).getItem();
                if (item == last || !wanted.contains(item)) {
                    continue;
                }
                last = item;
                List<ContainerTarget> list = byItem.computeIfAbsent(item, k -> new ArrayList<>());
                if (list.isEmpty() || list.getLast() != target) {
                    list.add(target);
                }
            }
        }

        Map<ContainerTarget, ItemStack[]> simulated = new HashMap<>();
        List<Allocation> allocations = new ArrayList<>();
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty() || LockManager.isLocked(locks, slot)) {
                continue;
            }
            List<ContainerTarget> candidates = byItem.get(stack.getItem());
            if (candidates == null) {
                continue;
            }
            for (ContainerTarget target : candidates) {
                if (stack.isEmpty()) {
                    break;
                }
                if (!Transfer.fitsIn(target, stack)) {
                    continue;
                }
                ItemStack[] sim = simulated.computeIfAbsent(target, t -> Transfer.snapshot(t.container()));
                ItemStack probe = stack.copy();
                int moved = Transfer.insert(target.container(), probe, sim);
                if (moved > 0) {
                    allocations.add(new Allocation(target, stack.split(moved)));
                }
            }
        }
        return allocations;
    }

    private static void schedule(ServerPlayer player, Batch batch, List<Allocation> allocations, boolean animate, long now) {
        ServerConfig config = ServerConfig.get();
        if (!animate) {
            for (Allocation allocation : allocations) {
                QUEUE.add(new Pending(player.getUUID(), batch.id, player.level(), allocation.target().pos(), allocation.stack(), now + 1));
            }
            List<BlockPos> positions = allocations.stream().map(a -> a.target().pos()).distinct().toList();
            Services.PLATFORM.sendToPlayer(player, new Payloads.Highlight(positions, 40, false));
            return;
        }

        Vec3 hover = player.getEyePosition().add(player.getLookAngle().scale(HOVER_DISTANCE));
        int count = allocations.size();
        int stagger = count <= 8 ? 4 : count <= 20 ? 2 : 1;
        List<Payloads.Flight> flights = new ArrayList<>();
        Map<ContainerTarget, int[]> windows = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            Allocation allocation = allocations.get(i);
            ContainerTarget target = allocation.target();
            int launch = FIRST_LAUNCH + i * stagger;
            int travel = Mth.clamp(Mth.ceil(hover.distanceTo(target.center()) / config.flightSpeed), 5, 60);
            int arrive = launch + travel;
            flights.add(new Payloads.Flight(allocation.stack().copy(), target.center(), launch, arrive));
            QUEUE.add(new Pending(player.getUUID(), batch.id, player.level(), target.pos(), allocation.stack(), now + arrive));
            int[] window = windows.computeIfAbsent(target, t -> new int[] {Integer.MAX_VALUE, 0});
            window[0] = Math.min(window[0], launch + travel / 2);
            window[1] = Math.max(window[1], arrive);
        }
        windows.forEach((target, window) -> {
            LIDS.add(new LidEvent(player.level(), target.pos(), now + window[0], true));
            LIDS.add(new LidEvent(player.level(), target.pos(), now + window[1] + 10, false));
        });
        Services.PLATFORM.sendToPlayer(player, new Payloads.Flights(flights, false));
    }

    public static void tick(MinecraftServer server) {
        long now = server.getTickCount();
        int budget = ServerConfig.get().maxInsertsPerTick;
        while (budget > 0 && !QUEUE.isEmpty() && QUEUE.peek().dueTick <= now) {
            land(server, QUEUE.poll());
            budget--;
        }
        while (!LIDS.isEmpty() && LIDS.peek().tick() <= now) {
            LIDS.poll().apply();
        }
        if (now % 200 == 0) {
            long undoTicks = ServerConfig.get().undoSeconds * 20L;
            LAST_BATCH.values().removeIf(batch -> now - batch.createdTick > undoTicks && !hasPending(batch.id));
            LAST_REQUEST.values().removeIf(tick -> now - tick > 1200);
        }
    }

    private static boolean hasPending(long batchId) {
        for (Pending pending : QUEUE) {
            if (pending.batchId == batchId) {
                return true;
            }
        }
        return false;
    }

    private static void land(MinecraftServer server, Pending pending) {
        ItemStack stack = pending.stack.copy();
        ContainerTarget target = ContainerScanner.resolve(pending.level, pending.pos);
        if (target != null && Transfer.fitsIn(target, stack)) {
            int moved = Transfer.insert(target.container(), stack, null);
            if (moved > 0) {
                Batch batch = LAST_BATCH.get(pending.playerId);
                if (batch != null && batch.id == pending.batchId) {
                    batch.landed.add(new Pending(pending.playerId, pending.batchId, pending.level, pending.pos,
                            pending.stack.copyWithCount(moved), pending.dueTick));
                }
            }
        }
        if (!stack.isEmpty()) {
            giveBack(server, pending.playerId, pending.level, pending.pos, stack);
        }
    }

    private static void giveBack(MinecraftServer server, UUID playerId, ServerLevel level, BlockPos pos, ItemStack stack) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null && !player.hasDisconnected()) {
            player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
        } else {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, stack);
        }
    }

    /** Lands everything a player still has in the air (disconnect, death, dimension change). */
    public static void flush(MinecraftServer server, UUID playerId) {
        List<Pending> mine = new ArrayList<>();
        for (Iterator<Pending> it = QUEUE.iterator(); it.hasNext(); ) {
            Pending pending = it.next();
            if (pending.playerId.equals(playerId)) {
                mine.add(pending);
                it.remove();
            }
        }
        mine.forEach(pending -> land(server, pending));
    }

    public static void flushAll(MinecraftServer server) {
        while (!QUEUE.isEmpty()) {
            land(server, QUEUE.poll());
        }
        while (!LIDS.isEmpty()) {
            LidEvent event = LIDS.poll();
            if (!event.open()) {
                event.apply();
            }
        }
        LAST_BATCH.clear();
        LAST_REQUEST.clear();
    }

    public static void undo(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        Batch batch = LAST_BATCH.remove(player.getUUID());
        long undoTicks = ServerConfig.get().undoSeconds * 20L;
        if (batch == null || server.getTickCount() - batch.createdTick > undoTicks) {
            player.sendOverlayMessage(Component.translatable("quickstack.message.nothing_to_undo"));
            return;
        }
        boolean cancelledFlights = false;
        for (Iterator<Pending> it = QUEUE.iterator(); it.hasNext(); ) {
            Pending pending = it.next();
            if (pending.batchId == batch.id) {
                it.remove();
                player.getInventory().placeItemBackInInventory(pending.stack.copy(), Prediction.SERVER_ONLY);
                cancelledFlights = true;
            }
        }
        if (cancelledFlights) {
            Services.PLATFORM.sendToPlayer(player, new Payloads.Flights(List.of(), true));
        }
        for (Pending landed : batch.landed) {
            ContainerTarget target = ContainerScanner.resolve(landed.level, landed.pos);
            if (target == null) {
                continue;
            }
            int wanted = landed.stack.getCount();
            for (int i = 0; i < target.container().getContainerSize() && wanted > 0; i++) {
                ItemStack inside = target.container().getItem(i);
                if (!inside.isEmpty() && ItemStack.isSameItemSameComponents(inside, landed.stack)) {
                    ItemStack taken = target.container().removeItem(i, Math.min(wanted, inside.getCount()));
                    wanted -= taken.getCount();
                    player.getInventory().placeItemBackInInventory(taken, Prediction.SERVER_ONLY);
                }
            }
            target.container().setChanged();
        }
        player.containerMenu.broadcastChanges();
        player.sendOverlayMessage(Component.translatable("quickstack.message.undone"));
    }
}
