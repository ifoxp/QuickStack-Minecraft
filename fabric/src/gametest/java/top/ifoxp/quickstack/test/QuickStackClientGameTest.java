package top.ifoxp.quickstack.test;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ifoxp.quickstack.client.ClientState;
import top.ifoxp.quickstack.client.QuickStackClient;
import top.ifoxp.quickstack.client.ui.ContainerScreenAccess;
import top.ifoxp.quickstack.client.ui.IconButton;
import top.ifoxp.quickstack.network.Payloads;
import top.ifoxp.quickstack.platform.Services;
import top.ifoxp.quickstack.server.LockManager;

/**
 * Plays QuickStack the way a player does in a real client: keys, mouse clicks on the mod's buttons and on slots,
 * opening chests and a crafting table. Every check is logged as "[qs-test] name: OK|FAIL" and screenshots are
 * written to build/run/clientGameTest/screenshots. Any failed check fails the run.
 */
public class QuickStackClientGameTest implements FabricClientGameTest {

    private static final Logger LOG = LoggerFactory.getLogger("QuickStackTest");
    private final List<String> failures = new ArrayList<>();
    private int passed;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            TestServerContext server = world.getServer();
            server.runCommand("gamemode survival @a");
            context.waitTicks(20);
            BlockPos origin = server.computeOnServer(s -> player(s).blockPosition());
            LOG.info("[qs-test] player at {}", origin);

            quickStackScenario(context, world, origin);
            lockAndSortScenario(context, world);
            chestButtonsScenario(context, world, origin);
            craftingScenario(context, world, origin);
            undoScenario(context, world, origin);
            loadScenario(context, world, origin);
        }
        LOG.info("[qs-test] done: {} passed, {} failed", passed, failures.size());
        if (!failures.isEmpty()) {
            throw new AssertionError("QuickStack gametest failures: " + String.join("; ", failures));
        }
    }

    // ---- scenarios ----

    /** Z in the world: items fly to the chest, barrel and double chest that already hold them. */
    private void quickStackScenario(ClientGameTestContext context, TestSingleplayerContext world, BlockPos origin) {
        BlockPos chest = origin.offset(3, 0, -2);
        BlockPos barrel = origin.offset(-3, 0, -2);
        BlockPos doubleRight = origin.offset(-1, 0, -4);
        BlockPos doubleLeft = origin.offset(0, 0, -4);
        world.getServer().runOnServer(server -> {
            ServerLevel level = player(server).level();
            level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
            level.setBlockAndUpdate(barrel, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
            level.setBlockAndUpdate(doubleRight, Blocks.CHEST.defaultBlockState()
                    .setValue(ChestBlock.FACING, Direction.SOUTH).setValue(ChestBlock.TYPE, ChestType.RIGHT));
            level.setBlockAndUpdate(doubleLeft, Blocks.CHEST.defaultBlockState()
                    .setValue(ChestBlock.FACING, Direction.SOUTH).setValue(ChestBlock.TYPE, ChestType.LEFT));
            container(level, chest).setItem(0, new ItemStack(Items.COBBLESTONE));
            container(level, barrel).setItem(4, new ItemStack(Items.OAK_LOG));
            container(level, doubleRight).setItem(2, new ItemStack(Items.DIRT));

            Inventory inventory = player(server).getInventory();
            inventory.clearContent();
            inventory.setItem(0, new ItemStack(Items.DIRT, 20));
            inventory.setItem(9, new ItemStack(Items.COBBLESTONE, 64));
            inventory.setItem(10, new ItemStack(Items.COBBLESTONE, 64));
            inventory.setItem(11, new ItemStack(Items.OAK_LOG, 30));
            inventory.setItem(12, new ItemStack(Items.DIRT, 10));
            inventory.setItem(13, new ItemStack(Items.DIAMOND_SWORD));
        });
        context.getInput().lookAt(180, 20);
        world.getConnection().waitForChunksRender();
        context.waitTicks(10);

        context.getInput().pressKey(QuickStackClient.QUICK_STACK);
        context.waitTicks(10);
        context.takeScreenshot("qs_01_hover");
        context.waitTicks(16);
        context.takeScreenshot("qs_02_flying");
        context.waitTicks(80);
        context.takeScreenshot("qs_03_landed");

        world.getServer().runOnServer(server -> {
            ServerLevel level = player(server).level();
            Inventory inventory = player(server).getInventory();
            check("cobblestone landed in the chest", count(container(level, chest), Items.COBBLESTONE) == 129,
                    "chest has " + count(container(level, chest), Items.COBBLESTONE));
            check("logs landed in the barrel", count(container(level, barrel), Items.OAK_LOG) == 31,
                    "barrel has " + count(container(level, barrel), Items.OAK_LOG));
            int dirt = count(container(level, doubleRight), Items.DIRT) + count(container(level, doubleLeft), Items.DIRT);
            check("dirt landed in the double chest", dirt == 11, "double chest has " + dirt);
            check("locked hotbar dirt stayed", inventory.getItem(0).is(Items.DIRT) && inventory.getItem(0).getCount() == 20,
                    "slot 0 = " + inventory.getItem(0));
            check("sword without a matching chest stayed", count(inventory, Items.DIAMOND_SWORD) == 1, "");
            check("no cobblestone left in the inventory", count(inventory, Items.COBBLESTONE) == 0, "");
        });
    }

    /** Alt+click locks a slot, the sort button sorts around it. Real mouse input on the inventory screen. */
    private void lockAndSortScenario(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            Inventory inventory = player(server).getInventory();
            for (int i = 9; i < Inventory.INVENTORY_SIZE; i++) {
                inventory.setItem(i, ItemStack.EMPTY);
            }
            inventory.setItem(9, new ItemStack(Items.STONE, 5));
            inventory.setItem(11, new ItemStack(Items.ANDESITE, 3));
            inventory.setItem(12, new ItemStack(Items.STONE, 7));
            inventory.setItem(20, new ItemStack(Items.GRANITE, 1));
            inventory.setItem(30, new ItemStack(Items.STONE, 1));
        });
        context.waitTicks(2);
        context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
        context.waitTicks(3);

        double[] slot30 = slotCenter(context, 30);
        context.getInput().holdAlt();
        clickAt(context, slot30[0], slot30[1]);
        context.getInput().releaseAlt();
        world.getConnection().waitForServerboundPackets();
        world.getConnection().waitForClientboundPackets();
        context.waitTicks(2);
        boolean lockedOnServer = world.getServer().computeOnServer(s -> LockManager.isLocked(LockManager.mask(player(s)), 30));
        check("alt+click locked slot 30 on the server", lockedOnServer, "");
        check("client shows slot 30 locked", context.computeOnClient(mc -> ClientState.isLocked(30)), "");
        context.takeScreenshot("qs_04_locked_slot");

        clickButton(context, IconButton.Icon.SORT);
        world.getConnection().waitForServerboundPackets();
        context.waitTicks(3);
        context.takeScreenshot("qs_05_sorted");
        world.getServer().runOnServer(server -> {
            Inventory inventory = player(server).getInventory();
            check("sort merged stone into slot 9", inventory.getItem(9).is(Items.STONE) && inventory.getItem(9).getCount() == 12,
                    "slot 9 = " + inventory.getItem(9));
            check("sort put granite after stone", inventory.getItem(10).is(Items.GRANITE), "slot 10 = " + inventory.getItem(10));
            check("sort put andesite third", inventory.getItem(11).is(Items.ANDESITE), "slot 11 = " + inventory.getItem(11));
            check("locked slot 30 kept its stone", inventory.getItem(30).is(Items.STONE) && inventory.getItem(30).getCount() == 1,
                    "slot 30 = " + inventory.getItem(30));
            check("hotbar (locked by default) untouched", inventory.getItem(0).is(Items.DIRT), "slot 0 = " + inventory.getItem(0));
        });
        context.runOnClient(mc -> mc.player.closeContainer());
        context.waitTicks(2);
    }

    /** Buttons of an open chest: restock (keeps one), quick stack into it, sort it. */
    private void chestButtonsScenario(ClientGameTestContext context, TestSingleplayerContext world, BlockPos origin) {
        BlockPos chest = origin.offset(3, 0, -2);
        world.getServer().runOnServer(server -> {
            Inventory inventory = player(server).getInventory();
            for (int i = 9; i < Inventory.INVENTORY_SIZE; i++) {
                inventory.setItem(i, ItemStack.EMPTY);
            }
            inventory.setItem(15, new ItemStack(Items.COBBLESTONE, 1));
        });
        context.waitTicks(2);
        openBlock(context, chest);
        context.waitForScreen(ContainerScreen.class);
        context.waitTicks(3);
        context.takeScreenshot("qs_06_chest_buttons");

        clickButton(context, IconButton.Icon.RESTOCK);
        world.getConnection().waitForServerboundPackets();
        context.waitTicks(3);
        world.getServer().runOnServer(server -> {
            ServerLevel level = player(server).level();
            check("restock left one cobblestone in the chest", count(container(level, chest), Items.COBBLESTONE) == 1,
                    "chest has " + count(container(level, chest), Items.COBBLESTONE));
            check("restock moved the rest to the player", count(player(server).getInventory(), Items.COBBLESTONE) == 129,
                    "player has " + count(player(server).getInventory(), Items.COBBLESTONE));
        });

        clickButton(context, IconButton.Icon.DEPOSIT_MATCHING);
        world.getConnection().waitForServerboundPackets();
        context.waitTicks(3);
        clickButton(context, IconButton.Icon.SORT);
        world.getConnection().waitForServerboundPackets();
        context.waitTicks(3);
        context.takeScreenshot("qs_07_chest_sorted");
        world.getServer().runOnServer(server -> {
            ServerLevel level = player(server).level();
            Container container = container(level, chest);
            check("quick stack into chest moved all cobblestone back", count(container, Items.COBBLESTONE) == 130,
                    "chest has " + count(container, Items.COBBLESTONE));
            check("chest sort merged stacks to the front", container.getItem(0).getCount() == 64
                    && container.getItem(1).getCount() == 64 && container.getItem(2).getCount() == 2,
                    "slots = " + container.getItem(0) + ", " + container.getItem(1) + ", " + container.getItem(2));
            container.setItem(10, new ItemStack(Items.ANDESITE));
            container.setItem(20, new ItemStack(Items.STONE));
        });
        context.waitTicks(3);

        // Middle click on a chest slot sorts the chest.
        double[] chestSlot = context.computeOnClient(mc -> {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) mc.gui.screen();
            ContainerScreenAccess layout = (ContainerScreenAccess) screen;
            Slot slot = screen.getMenu().slots.get(5);
            return new double[] {layout.quickstack$leftPos() + slot.x + 8, layout.quickstack$topPos() + slot.y + 8};
        });
        clickAt(context, chestSlot[0], chestSlot[1], InputConstants.MOUSE_BUTTON_MIDDLE);
        world.getConnection().waitForServerboundPackets();
        context.waitTicks(3);
        world.getServer().runOnServer(server -> {
            Container container = container(player(server).level(), chest);
            check("middle click sorted the chest", container.getItem(0).is(Items.STONE) && container.getItem(1).is(Items.ANDESITE)
                    && container.getItem(2).is(Items.COBBLESTONE), "slots = " + container.getItem(0) + ", " + container.getItem(1));
            container.setItem(0, ItemStack.EMPTY);
            container.setItem(1, ItemStack.EMPTY);
        });

        clickButton(context, IconButton.Icon.EXCLUDE);
        world.getConnection().waitForServerboundPackets();
        context.waitTicks(2);
        boolean excluded = world.getServer().computeOnServer(s -> Services.PLATFORM.isExcluded(player(s).level().getBlockEntity(chest)));
        check("exclude button marks the chest", excluded, "");
        clickButton(context, IconButton.Icon.EXCLUDE);
        world.getConnection().waitForServerboundPackets();
        context.waitTicks(2);
        boolean included = !world.getServer().computeOnServer(s -> Services.PLATFORM.isExcluded(player(s).level().getBlockEntity(chest)));
        check("second click includes it again", included, "");

        context.runOnClient(mc -> mc.player.closeContainer());
        context.waitTicks(2);
    }

    /** A crafting table next to a chest of planks: the recipe book sees them and placement takes them from the chest. */
    private void craftingScenario(ClientGameTestContext context, TestSingleplayerContext world, BlockPos origin) {
        BlockPos table = origin.offset(0, 0, 2);
        BlockPos chest = origin.offset(2, 0, 2);
        world.getServer().runOnServer(server -> {
            ServerLevel level = player(server).level();
            level.setBlockAndUpdate(table, Blocks.CRAFTING_TABLE.defaultBlockState());
            level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH));
            container(level, chest).setItem(0, new ItemStack(Items.OAK_PLANKS, 10));
        });
        context.waitTicks(2);
        openBlock(context, table);
        context.waitForScreen(CraftingScreen.class);
        context.waitTicks(30);
        int planksSeen = context.computeOnClient(mc -> ClientState.nearbyItems().stream()
                .filter(e -> e.item().value() == Items.OAK_PLANKS).mapToInt(Payloads.NearbyCount::count).sum());
        check("client knows the planks in the nearby chest", planksSeen == 10, "client sees " + planksSeen);
        context.takeScreenshot("qs_08_crafting_table");

        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            RecipeHolder<?> stick = server.getRecipeManager()
                    .byKey(ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("stick")))
                    .orElseThrow();
            AbstractCraftingMenu menu = (AbstractCraftingMenu) player.containerMenu;
            menu.handlePlacement(false, false, stick, player.level(), player.getInventory());
            int inGrid = 0;
            for (Slot slot : menu.getInputGridSlots()) {
                if (slot.getItem().is(Items.OAK_PLANKS)) {
                    inGrid += slot.getItem().getCount();
                }
            }
            check("recipe placement pulled planks from the chest into the grid", inGrid == 2, "grid has " + inGrid);
            check("the chest gave exactly those planks", count(container(player.level(), chest), Items.OAK_PLANKS) == 8,
                    "chest has " + count(container(player.level(), chest), Items.OAK_PLANKS));
            check("the result slot shows sticks", menu.getResultSlot().getItem().is(Items.STICK), "result = " + menu.getResultSlot().getItem());
        });
        context.waitTicks(3);
        context.takeScreenshot("qs_09_crafting_placed");
        context.runOnClient(mc -> mc.player.closeContainer());
        context.waitTicks(2);
    }

    /** Quick stack, then undo: the items come back from the chest. */
    private void undoScenario(ClientGameTestContext context, TestSingleplayerContext world, BlockPos origin) {
        BlockPos chest = origin.offset(3, 0, -2);
        world.getServer().runOnServer(server -> {
            Inventory inventory = player(server).getInventory();
            for (int i = 9; i < Inventory.INVENTORY_SIZE; i++) {
                inventory.setItem(i, ItemStack.EMPTY);
            }
            inventory.setItem(9, new ItemStack(Items.COBBLESTONE, 10));
        });
        context.waitTicks(30);
        context.getInput().pressKey(QuickStackClient.QUICK_STACK);
        context.waitTicks(80);
        int afterStack = world.getServer().computeOnServer(s -> count(container(player(s).level(), chest), Items.COBBLESTONE));
        check("second quick stack landed", afterStack == 140, "chest has " + afterStack);
        context.runOnClient(mc -> QuickStackClient.undo());
        world.getConnection().waitForServerboundPackets();
        context.waitTicks(3);
        world.getServer().runOnServer(server -> {
            check("undo returned the cobblestone", count(player(server).getInventory(), Items.COBBLESTONE) == 10,
                    "player has " + count(player(server).getInventory(), Items.COBBLESTONE));
            check("undo took it out of the chest", count(container(player(server).level(), chest), Items.COBBLESTONE) == 130,
                    "chest has " + count(container(player(server).level(), chest), Items.COBBLESTONE));
        });
    }

    /** 36 full stacks into 32 different chests at once; logs the worst server tick while they land. */
    private void loadScenario(ClientGameTestContext context, TestSingleplayerContext world, BlockPos origin) {
        Item[] kinds = {Items.STONE, Items.GRANITE, Items.DIORITE, Items.ANDESITE, Items.COBBLESTONE, Items.DIRT,
                Items.OAK_LOG, Items.SPRUCE_LOG, Items.BIRCH_LOG, Items.JUNGLE_LOG, Items.ACACIA_LOG, Items.DARK_OAK_LOG,
                Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.BIRCH_PLANKS, Items.SAND, Items.RED_SAND, Items.GRAVEL,
                Items.COAL, Items.IRON_INGOT, Items.GOLD_INGOT, Items.COPPER_INGOT, Items.REDSTONE, Items.LAPIS_LAZULI,
                Items.QUARTZ, Items.BONE, Items.STRING, Items.FEATHER, Items.GUNPOWDER, Items.WHEAT_SEEDS, Items.GLASS,
                Items.BRICKS};
        BlockPos center = origin.offset(40, 0, 0);
        List<BlockPos> chests = new ArrayList<>();
        for (int i = 0; i < kinds.length; i++) {
            double angle = Math.PI * 2 * (i % 16) / 16;
            int radius = i < 16 ? 5 : 7;
            chests.add(center.offset((int) Math.round(Math.cos(angle) * radius), i % 2, (int) Math.round(Math.sin(angle) * radius)));
        }
        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            ServerLevel level = player.level();
            player.teleportTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
            for (int i = 0; i < kinds.length; i++) {
                BlockPos pos = chests.get(i);
                level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
                container(level, pos).setItem(0, new ItemStack(kinds[i]));
            }
            LockManager.toggle(player, Payloads.ToggleLock.UNLOCK_ALL);
            Inventory inventory = player.getInventory();
            inventory.clearContent();
            for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
                inventory.setItem(i, new ItemStack(kinds[i % kinds.length], 64));
            }
        });
        world.getConnection().waitForChunksRender();
        context.getInput().lookAt(0, 10);
        context.waitTicks(110);
        long baseline = world.getServer().computeOnServer(s -> max(s.getTickTimesNanos()));

        context.getInput().pressKey(QuickStackClient.QUICK_STACK);
        context.waitTicks(20);
        context.takeScreenshot("qs_10_load_hover");
        context.waitTicks(40);
        context.takeScreenshot("qs_11_load_flying");
        context.waitTicks(60);
        long worst = world.getServer().computeOnServer(s -> max(s.getTickTimesNanos()));
        LOG.info("[qs-test] load: worst server tick before {} ms, during {} ms", baseline / 1_000_000.0, worst / 1_000_000.0);
        check("36 stacks into 32 chests kept server ticks under 50 ms", worst < 50_000_000L, "worst tick " + worst / 1_000_000.0 + " ms");

        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            ServerLevel level = player.level();
            check("every stack left the inventory", player.getInventory().isEmpty(), "inventory not empty");
            int total = 0;
            for (int i = 0; i < kinds.length; i++) {
                total += count(container(level, chests.get(i)), kinds[i]);
            }
            check("every item landed in its chest", total == kinds.length + 36 * 64, "chests hold " + total);
            LockManager.toggle(player, Payloads.ToggleLock.RESET_DEFAULT);
        });
    }

    // ---- helpers ----

    private void check(String name, boolean ok, String detail) {
        if (ok) {
            passed++;
            LOG.info("[qs-test] {}: OK", name);
        } else {
            failures.add(name + " (" + detail + ")");
            LOG.error("[qs-test] {}: FAIL {}", name, detail);
        }
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static Container container(ServerLevel level, BlockPos pos) {
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

    private static void openBlock(ClientGameTestContext context, BlockPos pos) {
        context.runOnClient(mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
    }

    private static void clickAt(ClientGameTestContext context, double guiX, double guiY) {
        clickAt(context, guiX, guiY, InputConstants.MOUSE_BUTTON_LEFT);
    }

    /** Mouse buttons are SDL numbers since 26.1: left = 1, middle = 2, right = 3. */
    private static void clickAt(ClientGameTestContext context, double guiX, double guiY, int button) {
        double scale = context.computeOnClient(mc -> (double) mc.getWindow().getScreenWidth() / mc.getWindow().getGuiScaledWidth());
        context.getInput().setCursorPos(guiX * scale, guiY * scale);
        context.waitTick();
        context.getInput().pressMouse(button);
        context.waitTick();
    }

    private static void clickButton(ClientGameTestContext context, IconButton.Icon icon) {
        double[] center = context.computeOnClient(mc -> {
            for (GuiEventListener child : mc.gui.screen().children()) {
                if (child instanceof IconButton button && button.icon() == icon) {
                    return new double[] {button.getX() + IconButton.SIZE / 2.0, button.getY() + IconButton.SIZE / 2.0};
                }
            }
            return null;
        });
        if (center == null) {
            throw new AssertionError("No " + icon + " button on " + context.computeOnClient(mc -> String.valueOf(mc.gui.screen())));
        }
        clickAt(context, center[0], center[1]);
    }

    private static double[] slotCenter(ClientGameTestContext context, int inventorySlot) {
        return context.computeOnClient(mc -> {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) mc.gui.screen();
            ContainerScreenAccess layout = (ContainerScreenAccess) screen;
            for (Slot slot : screen.getMenu().slots) {
                if (slot.container == mc.player.getInventory() && slot.getContainerSlot() == inventorySlot) {
                    return new double[] {layout.quickstack$leftPos() + slot.x + 8, layout.quickstack$topPos() + slot.y + 8};
                }
            }
            throw new AssertionError("No slot " + inventorySlot);
        });
    }
}
