package top.ifoxp.quickstack.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import top.ifoxp.quickstack.QuickStack;
import top.ifoxp.quickstack.client.ui.ConfigScreen;
import top.ifoxp.quickstack.config.ClientConfig;
import top.ifoxp.quickstack.network.MenuAction;
import top.ifoxp.quickstack.network.Payloads;
import top.ifoxp.quickstack.platform.Services;
import top.ifoxp.quickstack.server.NearbyCrafting;

/** Client side: key mappings, the actions behind the buttons and the handlers of server packets. */
public final class QuickStackClient {

    /** Translation key of the key mapping category (a plain string before 1.21.9). */
    public static final String CATEGORY = "key.category." + QuickStack.MOD_ID + ".main";

    public static final KeyMapping QUICK_STACK = new KeyMapping("key.quickstack.quick_stack", InputConstants.KEY_Z, CATEGORY);
    public static final KeyMapping SORT = new KeyMapping("key.quickstack.sort", InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping UNDO = new KeyMapping("key.quickstack.undo", InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping FIND = new KeyMapping("key.quickstack.find", InputConstants.KEY_F6, CATEGORY);
    public static final KeyMapping CONFIG = new KeyMapping("key.quickstack.config", InputConstants.UNKNOWN.getValue(), CATEGORY);

    public static final List<KeyMapping> KEYS = List.of(QUICK_STACK, SORT, UNDO, FIND, CONFIG);

    /** Ask for the nearby item summary this often while a crafting screen is open (containers change). */
    private static final int NEARBY_REFRESH_TICKS = 20;
    private static int nearbyRefresh;

    private QuickStackClient() {
    }

    public static boolean serverHasMod() {
        return Minecraft.getInstance().getConnection() != null && Services.client().serverHasMod();
    }

    private static boolean ready() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        if (!serverHasMod()) {
            minecraft.player.displayClientMessage(Component.translatable("quickstack.message.server_missing"), true);
            return false;
        }
        return true;
    }

    // ---- actions ----

    public static void quickStackNearby() {
        if (!ready()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            minecraft.player.closeContainer();
        }
        Services.client().sendToServer(new Payloads.QuickStackNearby(ClientConfig.get().animation));
    }

    public static void menuAction(MenuAction action) {
        if (!ready()) {
            return;
        }
        ClientConfig config = ClientConfig.get();
        Services.client().sendToServer(new Payloads.MenuActionRequest(action, config.keepInChest, config.sortMode.ordinal()));
    }

    public static void toggleLock(int slot) {
        if (!ready()) {
            return;
        }
        if (slot >= 0) {
            ClientState.toggleLocal(slot);
        }
        Services.client().sendToServer(new Payloads.ToggleLock(slot));
    }

    public static void undo() {
        if (ready()) {
            Services.client().sendToServer(new Payloads.Undo());
        }
    }

    public static void find(List<ItemStack> stacks) {
        if (!ready() || stacks.isEmpty()) {
            return;
        }
        Services.client().sendToServer(new Payloads.FindItems(
                stacks.stream().map(ItemStack::getItemHolder).distinct().limit(Payloads.FindItems.MAX).toList()));
    }

    // ---- ticks ----

    public static void tick(Minecraft minecraft) {
        if (SelfCheck.enabled()) {
            SelfCheck.tick(minecraft);
        }
        if (minecraft.player == null || minecraft.level == null) {
            FlightAnimator.clear();
            Highlighter.clear();
            return;
        }
        while (QUICK_STACK.consumeClick()) {
            quickStackNearby();
        }
        while (SORT.consumeClick()) {
            menuAction(MenuAction.SORT_INVENTORY);
        }
        while (UNDO.consumeClick()) {
            undo();
        }
        while (FIND.consumeClick()) {
            minecraft.setScreen(new top.ifoxp.quickstack.client.ui.FindItemScreen(null));
        }
        while (CONFIG.consumeClick()) {
            minecraft.setScreen(new ConfigScreen(null));
        }
        FlightAnimator.tick(minecraft);
        Highlighter.tick(minecraft);
        tickNearbyItems(minecraft);
    }

    /** Keeps the recipe book's view of nearby containers fresh while a crafting grid is open. */
    private static void tickNearbyItems(Minecraft minecraft) {
        boolean crafting = minecraft.screen instanceof AbstractContainerScreen<?> screen
                && NearbyCrafting.isCraftingGrid(screen.getMenu());
        if (!crafting) {
            nearbyRefresh = 0;
            return;
        }
        if (nearbyRefresh-- <= 0 && serverHasMod()) {
            nearbyRefresh = NEARBY_REFRESH_TICKS;
            Services.client().sendToServer(new Payloads.RequestNearbyItems(ClientConfig.get().craftFromNearby));
        }
    }

    public static void onDisconnect() {
        FlightAnimator.clear();
        Highlighter.clear();
        ClientState.reset();
    }

    // ---- packet handlers (registered by each loader's client entrypoint) ----

    public static void onFlights(Payloads.Flights payload) {
        FlightAnimator.start(Minecraft.getInstance(), payload.flights(), payload.cancelPrevious());
    }

    public static void onLockSync(Payloads.LockSync payload) {
        ClientState.setLockMask(payload.mask());
    }

    public static void onNearbyItems(Payloads.NearbyItems payload) {
        ClientState.setNearbyItems(ClientConfig.get().craftFromNearby ? payload.items() : List.of());
    }

    /** Asks the server for the contents of nearby containers; the answer replaces {@link ClientState#storage()}. */
    public static void requestStorage() {
        ClientState.setStorage(null);
        if (serverHasMod()) {
            Services.client().sendToServer(new Payloads.RequestStorage());
        }
    }

    public static void onStorageSummary(Payloads.StorageSummary payload) {
        ClientState.setStorage(payload);
    }

    public static void onHighlight(Payloads.Highlight payload) {
        if (payload.search() || ClientConfig.get().highlightTargets) {
            Highlighter.add(payload.positions(), payload.ticks(), payload.search());
        }
    }
}
