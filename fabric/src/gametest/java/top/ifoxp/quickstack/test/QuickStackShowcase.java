package top.ifoxp.quickstack.test;

import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.ChatVisiblity;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ifoxp.quickstack.client.QuickStackClient;
import top.ifoxp.quickstack.client.ui.ContainerScreenAccess;
import top.ifoxp.quickstack.client.ui.IconButton;
import top.ifoxp.quickstack.network.Payloads;
import top.ifoxp.quickstack.server.LockManager;

/**
 * Records the clips for the mod page: one per feature, in a small storage room, at 30 fps rendered with exact
 * partial ticks (ShowcaseOverlay draws the pointer and the key badge into the picture). Only runs with
 * {@code -Dquickstack.showcase=true} ({@code ./gradlew :fabric:runClientGameTest -Pshowcase [-Pclips=a,b]}). Frames go
 * to build/showcase/<clip>/f00000.png...; tools/make_media.py turns them into WebP and GIF.
 */
public class QuickStackShowcase implements FabricClientGameTest {

    private static final Logger LOG = LoggerFactory.getLogger("QuickStackShowcase");
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;

    private ClientGameTestContext context;
    private TestSingleplayerContext world;
    private BlockPos base;
    private static final int FPS = 30;
    private Path clipDir;
    private int frame;
    /** Ticks run since the clip began, and the tick time of the next frame to capture. */
    private long tickClock;
    private double nextFrameAt;
    /** Cursor in GUI units, or NaN when it should not be drawn (in the world view). */
    private double cursorX = Double.NaN;
    private double cursorY = Double.NaN;
    private String badge = "";

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!Boolean.getBoolean("quickstack.showcase")) {
            return;
        }
        this.context = context;
        ShowcaseOverlay.register();
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            this.world = world;
            context.getInput().resizeWindow(WIDTH, HEIGHT);
            context.runOnClient(mc -> {
                mc.options.guiScale().set(3);
                mc.options.chatVisibility().set(ChatVisiblity.HIDDEN);
                mc.resizeGui();
            });
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("recipe give @a *");
            BlockPos spawn = world.getServer().computeOnServer(s -> player(s).blockPosition());
            base = spawn.offset(500, 0, 0);
            buildRoom();
            world.getConnection().waitForChunksRender();

            java.util.Set<String> only = java.util.Set.of(System.getProperty("quickstack.showcase.clips", "").split(","));
            java.util.Map<String, Runnable> clips = new java.util.LinkedHashMap<>();
            clips.put("quick_stack", this::quickStackClip);
            clips.put("lock_slots", this::lockClip);
            clips.put("chest_buttons", this::chestButtonsClip);
            clips.put("sort", this::sortClip);
            clips.put("shift_sweep", this::shiftSweepClip);
            clips.put("wheel", this::wheelClip);
            clips.put("crafting", this::craftingClip);
            clips.put("search", this::searchClip);
            clips.forEach((name, clip) -> {
                if (only.contains("") || only.contains(name)) {
                    clip.run();
                }
            });
        }
    }

    // ---- the room ----

    private record Storage(int dx, boolean barrel, Item item) {
    }

    private static final Storage[] WALL = {
            new Storage(-4, false, Items.COBBLESTONE),
            new Storage(-3, true, Items.OAK_LOG),
            new Storage(-2, false, Items.DIRT),
            new Storage(-1, false, Items.IRON_INGOT),
            new Storage(0, true, Items.WHEAT),
            new Storage(1, false, Items.BONE),
            new Storage(2, false, Items.OAK_PLANKS),
            new Storage(3, true, Items.COAL),
            new Storage(4, false, Items.DIAMOND),
    };

    private BlockPos wallPos(int dx) {
        return base.offset(dx, 0, -5);
    }

    private BlockPos tablePos() {
        return base.offset(3, 0, -2);
    }

    private void buildRoom() {
        world.getServer().runOnServer(server -> {
            ServerLevel level = player(server).level();
            for (int dx = -7; dx <= 7; dx++) {
                for (int dz = -8; dz <= 4; dz++) {
                    level.setBlock(base.offset(dx, -1, dz), Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
                    for (int dy = 0; dy < 4; dy++) {
                        level.setBlock(base.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
            for (int dx = -6; dx <= 6; dx++) {
                for (int dy = 0; dy < 3; dy++) {
                    level.setBlock(base.offset(dx, dy, -6), Blocks.STONE_BRICKS.defaultBlockState(), 2);
                }
            }
            for (Storage storage : WALL) {
                BlockPos pos = wallPos(storage.dx);
                BlockState state = storage.barrel
                        ? Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.SOUTH)
                        : Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH);
                level.setBlock(pos, state, 3);
                container(level, pos).setItem(0, new ItemStack(storage.item, 3));
                if (storage.barrel) {
                    level.setBlock(pos.above(), Blocks.LANTERN.defaultBlockState(), 3);
                }
            }
            level.setBlock(tablePos(), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
            container(level, wallPos(2)).setItem(0, new ItemStack(Items.OAK_PLANKS, 40));
        });
    }

    private void standAt(BlockPos feet, float yaw, float pitch) {
        world.getServer().runOnServer(server -> player(server).teleportTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5));
        context.waitTicks(3);
        context.getInput().lookAt(yaw, pitch);
        world.getConnection().waitForChunksRender();
        context.waitTicks(5);
    }

    private void giveInventory(ItemStack... mainStacks) {
        world.getServer().runOnServer(server -> {
            Inventory inventory = player(server).getInventory();
            inventory.clearContent();
            inventory.setItem(0, new ItemStack(Items.DIAMOND_SWORD));
            inventory.setItem(1, new ItemStack(Items.IRON_PICKAXE));
            inventory.setItem(2, new ItemStack(Items.TORCH, 32));
            inventory.setItem(3, new ItemStack(Items.BREAD, 12));
            for (int i = 0; i < mainStacks.length; i++) {
                inventory.setItem(9 + i, mainStacks[i]);
            }
            LockManager.toggle(player(server), Payloads.ToggleLock.RESET_DEFAULT);
        });
        context.waitTicks(3);
    }

    // ---- clips ----

    private void quickStackClip() {
        standAt(base, 180, 18);
        giveInventory(new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.COBBLESTONE, 41), new ItemStack(Items.OAK_LOG, 32),
                new ItemStack(Items.DIRT, 64), new ItemStack(Items.IRON_INGOT, 23), new ItemStack(Items.WHEAT, 50),
                new ItemStack(Items.BONE, 17), new ItemStack(Items.COAL, 40), new ItemStack(Items.DIAMOND, 9));
        begin("quick_stack");
        context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
        shots(6);
        double[] button = buttonCenter(IconButton.Icon.QUICK_STACK, null);
        moveTo(button[0], button[1], 12);
        shots(8);
        badge = "Z";
        context.getInput().pressKey(QuickStackClient.QUICK_STACK);
        hideCursor();
        shots(30);
        badge = "";
        shots(80);
        end();
    }

    private void lockClip() {
        standAt(base, 180, 18);
        giveInventory(new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.IRON_INGOT, 23), new ItemStack(Items.COAL, 40),
                new ItemStack(Items.OAK_LOG, 32), new ItemStack(Items.WHEAT, 50), new ItemStack(Items.BONE, 17));
        begin("lock_slots");
        context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
        shots(6);
        for (int slot : new int[] {10, 11}) {
            double[] center = inventorySlot(slot);
            moveTo(center[0], center[1], 10);
            badge = "Alt + Click";
            context.getInput().holdAlt();
            context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            context.getInput().releaseAlt();
            shots(12);
        }
        badge = "";
        shots(10);
        badge = "Z";
        context.getInput().pressKey(QuickStackClient.QUICK_STACK);
        hideCursor();
        shots(25);
        badge = "";
        shots(60);
        context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
        double[] kept = inventorySlot(11);
        cursorX = kept[0] + 30;
        cursorY = kept[1] + 20;
        shots(35);
        context.setScreen(() -> null);
        end();
        world.getServer().runOnServer(server -> LockManager.toggle(player(server), Payloads.ToggleLock.RESET_DEFAULT));
    }

    private void chestButtonsClip() {
        BlockPos chest = wallPos(-4);
        standAt(chest.offset(0, 0, 2), 180, 30);
        world.getServer().runOnServer(server -> {
            Container container = container(player(server).level(), chest);
            for (int i = 0; i < container.getContainerSize(); i++) {
                container.setItem(i, ItemStack.EMPTY);
            }
            container.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
            container.setItem(4, new ItemStack(Items.COBBLESTONE, 30));
            container.setItem(9, new ItemStack(Items.ANDESITE, 20));
            container.setItem(13, new ItemStack(Items.COBBLESTONE, 12));
            container.setItem(20, new ItemStack(Items.ANDESITE, 9));
        });
        giveInventory(new ItemStack(Items.COBBLESTONE, 1), new ItemStack(Items.ANDESITE, 1), new ItemStack(Items.GRANITE, 16));
        begin("chest_buttons");
        openBlock(chest);
        context.waitForScreen(ContainerScreen.class);
        shots(6);
        clickButtonSlowly(IconButton.Icon.RESTOCK, null, "Click");
        shots(14);
        clickButtonSlowly(IconButton.Icon.DEPOSIT_MATCHING, null, "Click");
        shots(14);
        clickButtonSlowly(IconButton.Icon.SORT, "quickstack.button.sort_container", "Click");
        shots(20);
        context.runOnClient(mc -> mc.player.closeContainer());
        end();
    }

    private void sortClip() {
        standAt(base, 180, 18);
        giveInventory(new ItemStack(Items.COAL, 7), ItemStack.EMPTY, new ItemStack(Items.IRON_INGOT, 5), new ItemStack(Items.COAL, 20),
                ItemStack.EMPTY, new ItemStack(Items.OAK_LOG, 3), new ItemStack(Items.IRON_INGOT, 11), ItemStack.EMPTY,
                new ItemStack(Items.WHEAT, 6), new ItemStack(Items.OAK_LOG, 9), ItemStack.EMPTY, new ItemStack(Items.BONE, 4),
                ItemStack.EMPTY, new ItemStack(Items.WHEAT, 13), new ItemStack(Items.COAL, 2), ItemStack.EMPTY, ItemStack.EMPTY,
                new ItemStack(Items.BONE, 9));
        begin("sort");
        context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
        shots(8);
        clickButtonSlowly(IconButton.Icon.SORT, null, "Click");
        shots(30);
        context.setScreen(() -> null);
        end();
    }

    private void shiftSweepClip() {
        BlockPos chest = wallPos(1);
        standAt(chest.offset(0, 0, 2), 180, 30);
        world.getServer().runOnServer(server -> {
            Container container = container(player(server).level(), chest);
            for (int i = 0; i < container.getContainerSize(); i++) {
                container.setItem(i, ItemStack.EMPTY);
            }
        });
        giveInventory(new ItemStack(Items.BONE, 30), new ItemStack(Items.STRING, 12), new ItemStack(Items.ROTTEN_FLESH, 25),
                new ItemStack(Items.GUNPOWDER, 8), new ItemStack(Items.SPIDER_EYE, 4), new ItemStack(Items.ARROW, 40),
                new ItemStack(Items.BONE_MEAL, 18), new ItemStack(Items.SLIME_BALL, 3), new ItemStack(Items.FEATHER, 14));
        begin("shift_sweep");
        openBlock(chest);
        context.waitForScreen(ContainerScreen.class);
        shots(6);
        double[] first = inventorySlot(9);
        double[] last = inventorySlot(17);
        moveTo(first[0], first[1], 10);
        badge = "Shift + Drag";
        context.getInput().holdShift();
        context.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);
        shots(2);
        moveTo(last[0], last[1], 28);
        context.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.getInput().releaseShift();
        shots(6);
        badge = "";
        shots(16);
        context.runOnClient(mc -> mc.player.closeContainer());
        end();
    }

    private void wheelClip() {
        BlockPos chest = wallPos(4);
        standAt(chest.offset(0, 0, 2), 180, 30);
        world.getServer().runOnServer(server -> {
            Container container = container(player(server).level(), chest);
            for (int i = 0; i < container.getContainerSize(); i++) {
                container.setItem(i, ItemStack.EMPTY);
            }
            container.setItem(0, new ItemStack(Items.DIAMOND, 24));
            container.setItem(1, new ItemStack(Items.EMERALD, 17));
        });
        giveInventory();
        begin("wheel");
        openBlock(chest);
        context.waitForScreen(ContainerScreen.class);
        shots(6);
        double[] diamonds = menuSlot(0);
        moveTo(diamonds[0], diamonds[1], 10);
        badge = "Wheel ↑";
        for (int i = 0; i < 7; i++) {
            context.getInput().scroll(1);
            shots(3);
        }
        shots(4);
        double[] target = inventorySlot(13);
        badge = "";
        moveTo(target[0], target[1], 12);
        badge = "Wheel ↓";
        for (int i = 0; i < 4; i++) {
            context.getInput().scroll(-1);
            shots(3);
        }
        double[] next = inventorySlot(14);
        badge = "";
        moveTo(next[0], next[1], 6);
        badge = "Wheel ↓";
        for (int i = 0; i < 3; i++) {
            context.getInput().scroll(-1);
            shots(3);
        }
        badge = "";
        shots(12);
        context.runOnClient(mc -> mc.player.closeContainer());
        end();
    }

    private void craftingClip() {
        standAt(tablePos().offset(0, 0, 2), 180, 35);
        giveInventory();
        world.getServer().runOnServer(server -> container(player(server).level(), wallPos(2)).setItem(0, new ItemStack(Items.OAK_PLANKS, 40)));
        begin("crafting");
        openBlock(tablePos());
        context.waitForScreen(CraftingScreen.class);
        shots(6);
        double[] book = context.computeOnClient(mc -> {
            for (GuiEventListener child : mc.gui.screen().children()) {
                if (child instanceof ImageButton button) {
                    return new double[] {button.getX() + button.getWidth() / 2.0, button.getY() + button.getHeight() / 2.0};
                }
            }
            throw new AssertionError("no recipe book button");
        });
        moveTo(book[0], book[1], 10);
        badge = "Recipe book";
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        shots(30);
        badge = "";
        world.getServer().runOnServer(server -> {
            ServerPlayer player = player(server);
            RecipeHolder<?> recipe = server.getRecipeManager()
                    .byKey(ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("chest"))).orElseThrow();
            ((AbstractCraftingMenu) player.containerMenu).handlePlacement(false, false, recipe, player.level(), player.getInventory());
        });
        shots(30);
        context.runOnClient(mc -> mc.player.closeContainer());
        end();
    }

    private void searchClip() {
        standAt(base.offset(0, 0, 1), 180, 12);
        giveInventory();
        begin("search");
        shots(8);
        badge = "F6";
        context.getInput().pressKey(QuickStackClient.FIND);
        shots(10);
        badge = "";
        for (char c : "iron".toCharArray()) {
            context.getInput().typeChar(c);
            shots(4);
        }
        shots(6);
        double[] first = context.computeOnClient(mc -> {
            Screen screen = mc.gui.screen();
            return new double[] {(screen.width - (9 * 18 + 14)) / 2.0 + 7 + 9, (screen.height - (44 + 6 * 18 + 16)) / 2.0 + 42 + 9};
        });
        moveTo(first[0], first[1], 12);
        shots(8);
        badge = "Click";
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        hideCursor();
        shots(10);
        badge = "";
        shots(60);
        end();
    }

    // ---- recording ----

    private void begin(String clip) {
        hideCursor();
        badge = "";
        context.setScreen(() -> null);
        // Outside the run directory, which Loom clears before every test run.
        clipDir = Path.of(System.getProperty("quickstack.showcase.dir", "showcase"), clip).toAbsolutePath();
        try {
            Files.createDirectories(clipDir);
            try (var old = Files.list(clipDir)) {
                for (Path file : old.toList()) {
                    Files.delete(file);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        frame = 0;
        tickClock = 0;
        nextFrameAt = 0;
        ShowcaseOverlay.active = true;
        LOG.info("[showcase] recording {}", clip);
    }

    private void end() {
        shots(6);
        ShowcaseOverlay.active = false;
        context.setScreen(() -> null);
        context.waitTicks(3);
        LOG.info("[showcase] {} frames", frame);
    }

    /**
     * Runs {@code count} ticks and captures the frames that fall into them at {@link #FPS}: each frame is rendered
     * with its exact partial tick, so motion between ticks (flying items, lids, camera, the drawn pointer) is
     * interpolated by the game itself.
     */
    private void shots(int count) {
        for (int i = 0; i < count; i++) {
            context.runOnClient(mc -> mc.gui.toastManager().clear());
            ShowcaseOverlay.badge = badge;
            if (Double.isNaN(cursorX)) {
                ShowcaseOverlay.cursorVisible = false;
            } else {
                ShowcaseOverlay.cursorTo(cursorX, cursorY, !ShowcaseOverlay.cursorVisible);
            }
            while (nextFrameAt < tickClock + 1) {
                float delta = (float) (nextFrameAt - tickClock);
                String name = String.format("f%05d", frame++);
                context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix().withDestinationDir(clipDir).withDeltaTicks(delta));
                nextFrameAt += 20.0 / FPS;
            }
            tickClock++;
            context.waitTick();
        }
    }

    private void hideCursor() {
        cursorX = Double.NaN;
        cursorY = Double.NaN;
        ShowcaseOverlay.cursorVisible = false;
    }

    /** Glides the cursor to a GUI point, one step per tick (hover effects and drags follow it). */
    private void moveTo(double x, double y, int frames) {
        double fromX = Double.isNaN(cursorX) ? x - 60 : cursorX;
        double fromY = Double.isNaN(cursorY) ? y + 40 : cursorY;
        double scale = context.computeOnClient(mc -> (double) mc.getWindow().getScreenWidth() / mc.getWindow().getGuiScaledWidth());
        for (int i = 1; i <= frames; i++) {
            double t = (double) i / frames;
            double eased = t * t * (3 - 2 * t);
            cursorX = fromX + (x - fromX) * eased;
            cursorY = fromY + (y - fromY) * eased;
            context.getInput().setCursorPos(cursorX * scale, cursorY * scale);
            shots(1);
        }
    }

    private void clickButtonSlowly(IconButton.Icon icon, String tooltipKey, String label) {
        double[] center = buttonCenter(icon, tooltipKey);
        moveTo(center[0], center[1], 12);
        shots(10); // let the tooltip show
        badge = label;
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        shots(6);
        badge = "";
    }

    private double[] buttonCenter(IconButton.Icon icon, String tooltipKey) {
        return context.computeOnClient(mc -> {
            for (GuiEventListener child : mc.gui.screen().children()) {
                if (child instanceof IconButton button && button.icon() == icon && (tooltipKey == null
                        || (button.getMessage().getContents() instanceof TranslatableContents contents && contents.getKey().equals(tooltipKey)))) {
                    return new double[] {button.getX() + IconButton.SIZE / 2.0, button.getY() + IconButton.SIZE / 2.0};
                }
            }
            throw new AssertionError("No " + icon + " button");
        });
    }

    private double[] inventorySlot(int inventorySlot) {
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

    private double[] menuSlot(int index) {
        return context.computeOnClient(mc -> {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) mc.gui.screen();
            ContainerScreenAccess layout = (ContainerScreenAccess) screen;
            Slot slot = screen.getMenu().slots.get(index);
            return new double[] {layout.quickstack$leftPos() + slot.x + 8, layout.quickstack$topPos() + slot.y + 8};
        });
    }

    private void openBlock(BlockPos pos) {
        context.runOnClient(mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.SOUTH, pos, false)));
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static Container container(ServerLevel level, BlockPos pos) {
        return (Container) level.getBlockEntity(pos);
    }
}
