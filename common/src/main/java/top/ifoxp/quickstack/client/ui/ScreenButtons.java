package top.ifoxp.quickstack.client.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import top.ifoxp.quickstack.client.ClientState;
import top.ifoxp.quickstack.client.QuickStackClient;
import top.ifoxp.quickstack.config.ClientConfig;
import top.ifoxp.quickstack.network.MenuAction;

/** Adds QuickStack's buttons to container screens and draws the lock marks on locked slots. */
public final class ScreenButtons {

    private static final int STEP = IconButton.SIZE + 1;
    /** Height of the button tab above the window, including the 3 px that overlap its border. */
    private static final int TAB_HEIGHT = 15;
    private static final int LOCK_MARK_COLOR = 0xA0FFFFFF;

    private ScreenButtons() {
    }

    private record Spec(IconButton.Icon icon, MutableComponent tooltip, Runnable action) {
    }

    public static void addButtons(AbstractContainerScreen<?> screen, Consumer<AbstractWidget> add, Consumer<Renderable> addRenderable) {
        if (screen instanceof CreativeModeInventoryScreen || !QuickStackClient.serverHasMod()) {
            return;
        }
        AbstractContainerMenu menu = screen.getMenu();
        if (!showsMainInventory(menu)) {
            return;
        }
        ContainerScreenAccess layout = (ContainerScreenAccess) screen;
        ClientConfig config = ClientConfig.get();

        boolean storage = !(screen instanceof InventoryScreen) && isStorageMenu(menu);
        if (config.showInventoryButtons) {
            boolean tabFits = layout.quickstack$topPos() >= TAB_HEIGHT;
            if (storage) {
                // Container screens: the inventory's buttons sit on the "Inventory" label row, right under the
                // container's own row, so each group is next to what it acts on and nothing looks doubled.
                addRightAligned(add, layout, inventorySpecs(true), () -> layout.quickstack$inventoryLabelY() - 1);
            } else if (config.buttonPlacement == ClientConfig.ButtonPlacement.TAB && tabFits) {
                addTab(add, addRenderable, layout, inventorySpecs(true));
            } else if (screen instanceof InventoryScreen) {
                // The free corner right of the recipe book button, under the crafting grid.
                List<Spec> specs = inventorySpecs(true);
                for (int i = 0; i < specs.size(); i++) {
                    int index = i;
                    Spec spec = specs.get(i);
                    add.accept(new IconButton(
                            () -> layout.quickstack$leftPos() + 127 + index * STEP,
                            () -> layout.quickstack$topPos() + 64,
                            spec.icon, spec.tooltip, spec.action));
                }
            } else {
                addRightAligned(add, layout, inventorySpecs(false), () -> layout.quickstack$inventoryLabelY() - 1);
            }
        }
        if (config.showContainerButtons && storage) {
            List<Spec> specs = List.of(
                    new Spec(IconButton.Icon.DEPOSIT_MATCHING, Component.translatable("quickstack.button.deposit_matching"),
                            () -> QuickStackClient.menuAction(MenuAction.DEPOSIT_MATCHING)),
                    new Spec(IconButton.Icon.RESTOCK, Component.translatable("quickstack.button.restock", config.keepInChest),
                            () -> QuickStackClient.menuAction(MenuAction.RESTOCK)),
                    new Spec(IconButton.Icon.DEPOSIT_ALL, Component.translatable("quickstack.button.deposit_all"),
                            () -> QuickStackClient.menuAction(MenuAction.DEPOSIT_ALL)),
                    new Spec(IconButton.Icon.LOOT_ALL, Component.translatable("quickstack.button.loot_all"),
                            () -> QuickStackClient.menuAction(MenuAction.LOOT_ALL)),
                    new Spec(IconButton.Icon.SORT, Component.translatable("quickstack.button.sort_container"),
                            () -> QuickStackClient.menuAction(MenuAction.SORT_CONTAINER)),
                    new Spec(IconButton.Icon.EXCLUDE, Component.translatable("quickstack.button.toggle_excluded"),
                            () -> QuickStackClient.menuAction(MenuAction.TOGGLE_EXCLUDED)));
            addRightAligned(add, layout, specs, () -> layout.quickstack$titleLabelY() - 1);
        }
    }

    private static List<Spec> inventorySpecs(boolean full) {
        List<Spec> specs = new ArrayList<>();
        specs.add(new Spec(IconButton.Icon.QUICK_STACK,
                withKey(Component.translatable("quickstack.button.quick_stack"), QuickStackClient.QUICK_STACK),
                QuickStackClient::quickStackNearby));
        specs.add(new Spec(IconButton.Icon.SORT,
                withKey(Component.translatable("quickstack.button.sort_inventory"), QuickStackClient.SORT),
                () -> QuickStackClient.menuAction(MenuAction.SORT_INVENTORY)));
        if (full) {
            specs.add(new Spec(IconButton.Icon.FIND,
                    withKey(Component.translatable("quickstack.button.find"), QuickStackClient.FIND),
                    () -> Minecraft.getInstance().setScreen(new FindItemScreen(null))));
            specs.add(new Spec(IconButton.Icon.CONFIG, Component.translatable("quickstack.button.config"),
                    () -> Minecraft.getInstance().setScreen(new ConfigScreen(Minecraft.getInstance().screen))));
        }
        return specs;
    }

    private static MutableComponent withKey(MutableComponent text, KeyMapping key) {
        if (key.isUnbound()) {
            return text;
        }
        return text.append(Component.literal(" (")).append(key.getTranslatedKeyMessage()).append(Component.literal(")"));
    }

    /**
     * A small tab sticking out of the top edge of the window, right-aligned, holding the buttons. It stays off the
     * inventory area, the recipe book (left) and item list mods (right).
     */
    private static void addTab(Consumer<AbstractWidget> add, Consumer<Renderable> addRenderable, ContainerScreenAccess layout,
            List<Spec> specs) {
        int tabWidth = specs.size() * STEP + 3;
        IntSupplier tabX = () -> layout.quickstack$leftPos() + layout.quickstack$imageWidth() - 4 - tabWidth;
        IntSupplier tabY = () -> layout.quickstack$topPos() - TAB_HEIGHT + 3;
        addRenderable.accept((graphics, mouseX, mouseY, a) -> drawTab(graphics, tabX.getAsInt(), tabY.getAsInt(), tabWidth));
        for (int i = 0; i < specs.size(); i++) {
            int offset = 2 + i * STEP;
            Spec spec = specs.get(i);
            add.accept(new IconButton(() -> tabX.getAsInt() + offset, () -> tabY.getAsInt() + 2, spec.icon, spec.tooltip, spec.action));
        }
    }

    /** Vanilla panel colours; the bottom runs 3 px into the window so it merges with its top border. */
    private static void drawTab(GuiGraphics graphics, int x, int y, int w) {
        int bottom = y + TAB_HEIGHT;
        graphics.fill(x, y, x + w, bottom, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + w - 1, bottom, 0xFFC6C6C6);
        graphics.fill(x + 1, y + 1, x + w - 2, y + 2, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + 2, bottom - 2, 0xFFFFFFFF);
        graphics.fill(x + w - 2, y + 2, x + w - 1, bottom - 2, 0xFF555555);
    }

    private static void addRightAligned(Consumer<AbstractWidget> add, ContainerScreenAccess layout, List<Spec> specs,
            IntSupplier relativeY) {
        int count = specs.size();
        for (int i = 0; i < count; i++) {
            int fromRight = count - i;
            Spec spec = specs.get(i);
            add.accept(new IconButton(
                    () -> layout.quickstack$leftPos() + layout.quickstack$imageWidth() - 7 - fromRight * STEP + 1,
                    () -> layout.quickstack$topPos() + relativeY.getAsInt(),
                    spec.icon, spec.tooltip, spec.action));
        }
    }

    private static boolean showsMainInventory(AbstractContainerMenu menu) {
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory && slot.getContainerSlot() >= Inventory.SELECTION_SIZE
                    && slot.getContainerSlot() < Inventory.INVENTORY_SIZE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Client-side guess of "plain storage". The server decides for real (MenuActions.storageSlots); here vanilla
     * chests/barrels/shulkers qualify, and modded menus whose other slots are plain slots of one big container.
     */
    public static boolean isStorageMenu(AbstractContainerMenu menu) {
        if (menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu) {
            return true;
        }
        Container shared = null;
        int count = 0;
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory) {
                continue;
            }
            if (slot.getClass() != Slot.class || (shared != null && shared != slot.container)) {
                return false;
            }
            shared = slot.container;
            count++;
        }
        return count >= ClientConfig.STORAGE_MIN_SLOTS;
    }

    public static boolean isLockableSlot(Slot slot) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && slot.container == minecraft.player.getInventory()
                && slot.getContainerSlot() < Inventory.INVENTORY_SIZE;
    }

    /** Called after vanilla drew a slot (coordinates are relative to the screen's image). */
    public static void drawSlotOverlay(GuiGraphics graphics, Slot slot) {
        if (!isLockableSlot(slot) || !ClientState.isLocked(slot.getContainerSlot()) || !QuickStackClient.serverHasMod()) {
            return;
        }
        // Kept quiet on purpose: a small faded padlock in the corner, no frame or tint over the item.
        // Drawn in front of the item (1.21.5 items render at z 150, decorations at 200).
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 200.0F);
        IconButton.drawIcon(graphics, IconButton.Icon.SMALL_LOCK, slot.x - 1, slot.y - 1, LOCK_MARK_COLOR);
        graphics.pose().popPose();
    }
}
