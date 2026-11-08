package top.ifoxp.quickstack.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ifoxp.quickstack.network.MenuAction;
import top.ifoxp.quickstack.network.Payloads;
import top.ifoxp.quickstack.platform.Services;
import top.ifoxp.quickstack.server.ContainerScanner;
import top.ifoxp.quickstack.server.ContainerTarget;
import top.ifoxp.quickstack.server.LockManager;
import top.ifoxp.quickstack.server.MenuActions;
import top.ifoxp.quickstack.server.QuickStackManager;
import top.ifoxp.quickstack.sort.SortMode;

/**
 * Server side of QuickStack on Minecraft versions without client gametests (before 1.21.4): the scenarios of the
 * client gametest on main, driven through the same server entry points the mod's packets call, with a mock player.
 * Client-only parts (buttons, mouse, key presses, screens, highlighting) are not covered here. Every check is logged as
 * "[qs-test] name: OK|FAIL"; any failed check fails the test.
 */
public class QuickStackServerGameTest implements FabricGameTest {

    private static final Logger LOG = LoggerFactory.getLogger("QuickStackTest");
    private static final int SORT = SortMode.CATEGORY.ordinal();
    private static final Item[] KINDS = {Items.STONE, Items.GRANITE, Items.DIORITE, Items.ANDESITE, Items.COBBLESTONE, Items.DIRT,
            Items.OAK_LOG, Items.SPRUCE_LOG, Items.BIRCH_LOG, Items.JUNGLE_LOG, Items.ACACIA_LOG, Items.DARK_OAK_LOG,
            Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.BIRCH_PLANKS, Items.SAND, Items.RED_SAND, Items.GRAVEL,
            Items.COAL, Items.IRON_INGOT, Items.GOLD_INGOT, Items.COPPER_INGOT, Items.REDSTONE, Items.LAPIS_LAZULI,
            Items.QUARTZ, Items.BONE, Items.STRING, Items.FEATHER, Items.GUNPOWDER, Items.WHEAT_SEEDS, Items.GLASS,
            Items.BRICKS};

    private final List<String> failures = new ArrayList<>();
    private final Set<Long> forcedChunks = new HashSet<>();
    private int passed;

    private GameTestHelper helper;
    private ServerLevel level;
    private ServerPlayer player;
    private BlockPos origin;
    /** Test tick of the next step. */
    private int clock;

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 2400)
    public void quickStack(GameTestHelper helper) {
        this.helper = helper;
        this.level = helper.getLevel();
        this.origin = helper.absolutePos(new BlockPos(4, 1, 4));
        this.clock = 1;
        forceChunks(origin, 10);
        forceChunks(origin.offset(40, 0, 0), 10);
        step(0, () -> {
            player = helper.makeMockServerPlayerInLevel();
            player.teleportTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            LOG.info("[qs-test] player at {}", player.blockPosition());
        });
        quickStackScenario();
        lockAndSortScenario();
        chestButtonsScenario();
        craftingScenario();
        undoScenario();
        loadScenario();
        step(1, () -> {
            for (long chunk : forcedChunks) {
                level.setChunkForced(SectionPos.x(chunk), SectionPos.z(chunk), false);
            }
            player.connection.disconnect(net.minecraft.network.chat.Component.literal("done"));
            LOG.info("[qs-test] done: {} passed, {} failed", passed, failures.size());
            if (failures.isEmpty()) {
                helper.succeed();
            } else {
                helper.fail("QuickStack gametest failures: " + String.join("; ", failures));
            }
        });
    }

    // ---- scenarios ----

    /** Quick stack: items land in the chest, barrel and double chest that already hold them. */
    private void quickStackScenario() {
        BlockPos chest = origin.offset(3, 0, -2);
        BlockPos barrel = origin.offset(-3, 0, -2);
        BlockPos doubleRight = origin.offset(-1, 0, -4);
        BlockPos doubleLeft = origin.offset(0, 0, -4);
        step(1, () -> {
            level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
            level.setBlockAndUpdate(barrel, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
            level.setBlockAndUpdate(doubleRight, Blocks.CHEST.defaultBlockState()
                    .setValue(ChestBlock.FACING, Direction.SOUTH).setValue(ChestBlock.TYPE, ChestType.RIGHT));
            level.setBlockAndUpdate(doubleLeft, Blocks.CHEST.defaultBlockState()
                    .setValue(ChestBlock.FACING, Direction.SOUTH).setValue(ChestBlock.TYPE, ChestType.LEFT));
            container(chest).setItem(0, new ItemStack(Items.COBBLESTONE));
            container(barrel).setItem(4, new ItemStack(Items.OAK_LOG));
            container(doubleRight).setItem(2, new ItemStack(Items.DIRT));

            Inventory inventory = player.getInventory();
            inventory.clearContent();
            inventory.setItem(0, new ItemStack(Items.DIRT, 20));
            inventory.setItem(9, new ItemStack(Items.COBBLESTONE, 64));
            inventory.setItem(10, new ItemStack(Items.COBBLESTONE, 64));
            inventory.setItem(11, new ItemStack(Items.OAK_LOG, 30));
            inventory.setItem(12, new ItemStack(Items.DIRT, 10));
            inventory.setItem(13, new ItemStack(Items.DIAMOND_SWORD));
        });
        step(10, () -> QuickStackManager.requestQuickStack(player, true));
        step(120, () -> {
            Inventory inventory = player.getInventory();
            check("cobblestone landed in the chest", count(container(chest), Items.COBBLESTONE) == 129,
                    "chest has " + count(container(chest), Items.COBBLESTONE));
            check("logs landed in the barrel", count(container(barrel), Items.OAK_LOG) == 31,
                    "barrel has " + count(container(barrel), Items.OAK_LOG));
            int dirt = count(container(doubleRight), Items.DIRT) + count(container(doubleLeft), Items.DIRT);
            check("dirt landed in the double chest", dirt == 11, "double chest has " + dirt);
            check("locked hotbar dirt stayed", inventory.getItem(0).is(Items.DIRT) && inventory.getItem(0).getCount() == 20,
                    "slot 0 = " + inventory.getItem(0));
            check("sword without a matching chest stayed", count(inventory, Items.DIAMOND_SWORD) == 1, "");
            check("no cobblestone left in the inventory", count(inventory, Items.COBBLESTONE) == 0, "");
        });
    }

    /** A locked slot (Alt+click sends ToggleLock) and the sort button sorting around it. */
    private void lockAndSortScenario() {
        step(1, () -> {
            Inventory inventory = player.getInventory();
            for (int i = 9; i < Inventory.INVENTORY_SIZE; i++) {
                inventory.setItem(i, ItemStack.EMPTY);
            }
            inventory.setItem(9, new ItemStack(Items.STONE, 5));
            inventory.setItem(11, new ItemStack(Items.ANDESITE, 3));
            inventory.setItem(12, new ItemStack(Items.STONE, 7));
            inventory.setItem(20, new ItemStack(Items.GRANITE, 1));
            inventory.setItem(30, new ItemStack(Items.STONE, 1));
            LockManager.toggle(player, 30);
            check("toggling slot 30 locked it on the server", LockManager.isLocked(LockManager.mask(player), 30), "");
        });
        step(1, () -> MenuActions.handle(new Payloads.MenuActionRequest(MenuAction.SORT_INVENTORY, 1, SORT), player));
        step(2, () -> {
            Inventory inventory = player.getInventory();
            check("sort merged stone into slot 9", inventory.getItem(9).is(Items.STONE) && inventory.getItem(9).getCount() == 12,
                    "slot 9 = " + inventory.getItem(9));
            check("sort put granite after stone", inventory.getItem(10).is(Items.GRANITE), "slot 10 = " + inventory.getItem(10));
            check("sort put andesite third", inventory.getItem(11).is(Items.ANDESITE), "slot 11 = " + inventory.getItem(11));
            check("locked slot 30 kept its stone", inventory.getItem(30).is(Items.STONE) && inventory.getItem(30).getCount() == 1,
                    "slot 30 = " + inventory.getItem(30));
            check("hotbar (locked by default) untouched", inventory.getItem(0).is(Items.DIRT), "slot 0 = " + inventory.getItem(0));
        });
    }

    /** Actions of an open chest: restock (keeps one), quick stack into it, sort, exclude, wheel moves. */
    private void chestButtonsScenario() {
        BlockPos chest = origin.offset(3, 0, -2);
        step(1, () -> {
            Inventory inventory = player.getInventory();
            for (int i = 9; i < Inventory.INVENTORY_SIZE; i++) {
                inventory.setItem(i, ItemStack.EMPTY);
            }
            inventory.setItem(15, new ItemStack(Items.COBBLESTONE, 1));
            open(chest);
        });
        step(2, () -> menuAction(MenuAction.RESTOCK));
        step(2, () -> {
            check("restock left one cobblestone in the chest", count(container(chest), Items.COBBLESTONE) == 1,
                    "chest has " + count(container(chest), Items.COBBLESTONE));
            check("restock moved the rest to the player", count(player.getInventory(), Items.COBBLESTONE) == 129,
                    "player has " + count(player.getInventory(), Items.COBBLESTONE));
            menuAction(MenuAction.DEPOSIT_MATCHING);
        });
        step(2, () -> menuAction(MenuAction.SORT_CONTAINER));
        step(2, () -> {
            Container container = container(chest);
            check("quick stack into chest moved all cobblestone back", count(container, Items.COBBLESTONE) == 130,
                    "chest has " + count(container, Items.COBBLESTONE));
            check("chest sort merged stacks to the front", container.getItem(0).getCount() == 64
                    && container.getItem(1).getCount() == 64 && container.getItem(2).getCount() == 2,
                    "slots = " + container.getItem(0) + ", " + container.getItem(1) + ", " + container.getItem(2));
            container.setItem(10, new ItemStack(Items.ANDESITE));
            container.setItem(20, new ItemStack(Items.STONE));
        });
        // A middle click on a chest slot sends the same request.
        step(2, () -> menuAction(MenuAction.SORT_CONTAINER));
        step(2, () -> {
            Container container = container(chest);
            check("middle click sorted the chest", container.getItem(0).is(Items.STONE) && container.getItem(1).is(Items.ANDESITE)
                    && container.getItem(2).is(Items.COBBLESTONE), "slots = " + container.getItem(0) + ", " + container.getItem(1));
            container.setItem(0, ItemStack.EMPTY);
            container.setItem(1, ItemStack.EMPTY);
            menuAction(MenuAction.TOGGLE_EXCLUDED);
        });
        step(2, () -> {
            check("exclude button marks the chest", Services.PLATFORM.isExcluded(level.getBlockEntity(chest)), "");
            menuAction(MenuAction.TOGGLE_EXCLUDED);
        });
        step(2, () -> check("second click includes it again", !Services.PLATFORM.isExcluded(level.getBlockEntity(chest)), ""));

        // Mouse wheel: up sends TakeFromSlot (one item onto the cursor), down is a vanilla right click.
        step(2, () -> {
            AbstractContainerMenu menu = player.containerMenu;
            int slot = cobblestoneSlot(menu);
            for (int i = 0; i < 3; i++) {
                MenuActions.takeFromSlot(new Payloads.TakeFromSlot(menu.containerId, slot, 1), player);
            }
            check("three wheel notches up put 3 items on the cursor", menu.getCarried().getCount() == 3,
                    "cursor has " + menu.getCarried().getCount());
            for (int i = 0; i < 3; i++) {
                menu.clicked(slot, 1, ClickType.PICKUP, player);
            }
            check("three notches down put them back one by one", menu.getCarried().isEmpty()
                    && count(container(chest), Items.COBBLESTONE) == 130,
                    "cursor " + menu.getCarried() + ", chest " + count(container(chest), Items.COBBLESTONE));
            player.closeContainer();
        });
    }

    /** A crafting table next to a chest of planks: placement takes them from the chest, leftovers go back. */
    private void craftingScenario() {
        BlockPos table = origin.offset(0, 0, 2);
        BlockPos chest = origin.offset(2, 0, 2);
        step(2, () -> {
            level.setBlockAndUpdate(table, Blocks.CRAFTING_TABLE.defaultBlockState());
            level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH));
            container(chest).setItem(0, new ItemStack(Items.OAK_PLANKS, 10));
        });
        step(2, () -> {
            open(table);
            int planksSeen = 0;
            for (ContainerTarget target : ContainerScanner.find(player, 8, ContainerScanner.Purpose.READ)) {
                planksSeen += count(target.container(), Items.OAK_PLANKS);
            }
            check("the server sees the planks in the nearby chest", planksSeen == 10, "sees " + planksSeen);
            RecipeBookMenu<?, ?> menu = (RecipeBookMenu<?, ?>) player.containerMenu;
            menu.handlePlacement(false, stick(), player);
            int inGrid = 0;
            for (Slot slot : gridSlots(menu)) {
                if (slot.getItem().is(Items.OAK_PLANKS)) {
                    inGrid += slot.getItem().getCount();
                }
            }
            check("recipe placement pulled planks from the chest into the grid", inGrid == 2, "grid has " + inGrid);
            check("the chest gave exactly those planks", count(container(chest), Items.OAK_PLANKS) == 8,
                    "chest has " + count(container(chest), Items.OAK_PLANKS));
            Slot result = menu.getSlot(menu.getResultSlotIndex());
            check("the result slot shows sticks", result.getItem().is(Items.STICK), "result = " + result.getItem());
        });
        step(3, () -> player.closeContainer());
        step(3, () -> {
            check("closing the table returned the borrowed planks to the chest",
                    count(container(chest), Items.OAK_PLANKS) == 10,
                    "chest has " + count(container(chest), Items.OAK_PLANKS));
            check("no planks ended up in the inventory", count(player.getInventory(), Items.OAK_PLANKS) == 0,
                    "player has " + count(player.getInventory(), Items.OAK_PLANKS));
            // Shift-click (max) placement, craft once, close: what is left goes back to the chest.
            open(table);
        });
        step(3, () -> {
            RecipeBookMenu<?, ?> menu = (RecipeBookMenu<?, ?>) player.containerMenu;
            menu.handlePlacement(true, stick(), player);
            check("max placement emptied the chest of planks", count(container(chest), Items.OAK_PLANKS) == 0,
                    "chest has " + count(container(chest), Items.OAK_PLANKS));
            Slot resultSlot = menu.getSlot(menu.getResultSlotIndex());
            ItemStack result = resultSlot.getItem().copy();
            resultSlot.onTake(player, result);
            player.closeContainer();
        });
        step(3, () -> {
            check("after crafting once the 8 leftover planks went back to the chest",
                    count(container(chest), Items.OAK_PLANKS) == 8,
                    "chest has " + count(container(chest), Items.OAK_PLANKS));
            check("leftovers did not land in the inventory", count(player.getInventory(), Items.OAK_PLANKS) == 0,
                    "player has " + count(player.getInventory(), Items.OAK_PLANKS));
        });
    }

    /** Quick stack, then undo: the items come back from the chest. */
    private void undoScenario() {
        BlockPos chest = origin.offset(3, 0, -2);
        step(1, () -> {
            Inventory inventory = player.getInventory();
            for (int i = 9; i < Inventory.INVENTORY_SIZE; i++) {
                inventory.setItem(i, ItemStack.EMPTY);
            }
            inventory.setItem(9, new ItemStack(Items.COBBLESTONE, 10));
        });
        step(30, () -> QuickStackManager.requestQuickStack(player, true));
        step(80, () -> {
            int afterStack = count(container(chest), Items.COBBLESTONE);
            check("second quick stack landed", afterStack == 140, "chest has " + afterStack);
            QuickStackManager.undo(player);
        });
        step(3, () -> {
            check("undo returned the cobblestone", count(player.getInventory(), Items.COBBLESTONE) == 10,
                    "player has " + count(player.getInventory(), Items.COBBLESTONE));
            check("undo took it out of the chest", count(container(chest), Items.COBBLESTONE) == 130,
                    "chest has " + count(container(chest), Items.COBBLESTONE));
        });
    }

    /** 36 full stacks into 32 different chests at once; logs the worst server tick while they land. */
    private void loadScenario() {
        BlockPos center = origin.offset(40, 0, 0);
        List<BlockPos> chests = new ArrayList<>();
        for (int i = 0; i < KINDS.length; i++) {
            double angle = Math.PI * 2 * (i % 16) / 16;
            int radius = i < 16 ? 5 : 7;
            chests.add(center.offset((int) Math.round(Math.cos(angle) * radius), i % 2, (int) Math.round(Math.sin(angle) * radius)));
        }
        long[] baseline = new long[1];
        step(1, () -> {
            player.teleportTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
            for (int i = 0; i < KINDS.length; i++) {
                BlockPos pos = chests.get(i);
                level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
                container(pos).setItem(0, new ItemStack(KINDS[i]));
            }
            LockManager.toggle(player, Payloads.ToggleLock.UNLOCK_ALL);
            Inventory inventory = player.getInventory();
            inventory.clearContent();
            for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
                inventory.setItem(i, new ItemStack(KINDS[i % KINDS.length], 64));
            }
        });
        step(110, () -> {
            baseline[0] = max(level.getServer().getTickTimesNanos());
            QuickStackManager.requestQuickStack(player, true);
        });
        step(120, () -> {
            long worst = max(level.getServer().getTickTimesNanos());
            LOG.info("[qs-test] load: worst server tick before {} ms, during {} ms", baseline[0] / 1_000_000.0, worst / 1_000_000.0);
            check("36 stacks into 32 chests kept server ticks under 50 ms", worst < 50_000_000L, "worst tick " + worst / 1_000_000.0 + " ms");
            check("every stack left the inventory", player.getInventory().isEmpty(), "inventory not empty");
            int total = 0;
            for (int i = 0; i < KINDS.length; i++) {
                total += count(container(chests.get(i)), KINDS[i]);
            }
            check("every item landed in its chest", total == KINDS.length + 36 * 64, "chests hold " + total);
            LockManager.toggle(player, Payloads.ToggleLock.RESET_DEFAULT);
        });
    }

    // ---- helpers ----

    /** Runs {@code action} {@code delay} ticks after the previous step (at least one tick later). */
    private void step(int delay, Runnable action) {
        clock += Math.max(1, delay);
        helper.runAtTickTime(clock, () -> {
            try {
                action.run();
            } catch (Throwable e) {
                LOG.error("[qs-test] step at tick {} threw", helper.getTick(), e);
                failures.add("step at tick " + helper.getTick() + " threw " + e);
            }
        });
    }

    private void check(String name, boolean ok, String detail) {
        if (ok) {
            passed++;
            LOG.info("[qs-test] {}: OK", name);
        } else {
            failures.add(name + " (" + detail + ")");
            LOG.error("[qs-test] {}: FAIL {}", name, detail);
        }
    }

    /** Keeps the test area loaded: QuickStack only looks at chunks that are already loaded. */
    private void forceChunks(BlockPos center, int radius) {
        for (int x = SectionPos.blockToSectionCoord(center.getX() - radius); x <= SectionPos.blockToSectionCoord(center.getX() + radius); x++) {
            for (int z = SectionPos.blockToSectionCoord(center.getZ() - radius); z <= SectionPos.blockToSectionCoord(center.getZ() + radius); z++) {
                if (level.setChunkForced(x, z, true)) {
                    forcedChunks.add(SectionPos.asLong(x, 0, z));
                }
            }
        }
    }

    private void open(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        MenuProvider provider = state.getMenuProvider(level, pos);
        player.openMenu(provider);
    }

    private void menuAction(MenuAction action) {
        MenuActions.handle(new Payloads.MenuActionRequest(action, 1, SORT), player);
    }

    private int cobblestoneSlot(AbstractContainerMenu menu) {
        for (Slot slot : menu.slots) {
            if (!(slot.container instanceof Inventory) && slot.getItem().is(Items.COBBLESTONE)) {
                return slot.index;
            }
        }
        throw new AssertionError("no cobblestone in the chest");
    }

    /** The stick recipe, unlocked for the player (before 1.21.2 placement only takes recipes from the recipe book). */
    private RecipeHolder<?> stick() {
        MinecraftServer server = level.getServer();
        RecipeHolder<?> recipe = server.getRecipeManager().byKey(ResourceLocation.withDefaultNamespace("stick")).orElseThrow();
        player.awardRecipes(List.of(recipe));
        return recipe;
    }

    /** The crafting grid (AbstractCraftingMenu.getInputGridSlots from 1.21.2). */
    private static List<Slot> gridSlots(RecipeBookMenu<?, ?> menu) {
        List<Slot> slots = new ArrayList<>();
        for (int i = 0; i < menu.getGridWidth() * menu.getGridHeight() + 1; i++) {
            if (i != menu.getResultSlotIndex()) {
                slots.add(menu.getSlot(i));
            }
        }
        return slots;
    }

    private Container container(BlockPos pos) {
        return (Container) level.getBlockEntity(pos);
    }

    private static int count(Container container, Item item) {
        int total = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).is(item)) {
                total += container.getItem(i).getCount();
            }
        }
        return total;
    }

    private static long max(long[] values) {
        return Arrays.stream(values).max().orElse(0);
    }
}
