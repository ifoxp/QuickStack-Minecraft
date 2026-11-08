package top.ifoxp.quickstack.client.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import top.ifoxp.quickstack.client.QuickStackClient;
import top.ifoxp.quickstack.config.ClientConfig;
import top.ifoxp.quickstack.config.ServerConfig;
import top.ifoxp.quickstack.network.Payloads;
import top.ifoxp.quickstack.sort.SortMode;

/**
 * QuickStack settings, built from vanilla widgets (no config library). One page holds the player's own
 * preferences; a second tab holds the world (server) limits and only appears when this client runs the server
 * (singleplayer), because a remote server reads its own file.
 */
public class ConfigScreen extends Screen {

    private static final int WIDTH = 150;
    private static final int HEIGHT = 20;
    private static final int ROW = 22;

    private final @Nullable Screen parent;
    /** Showing the world (server) settings instead of the player's own. */
    private boolean serverPage;

    public ConfigScreen(@Nullable Screen parent) {
        super(Component.translatable("quickstack.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        boolean editServer = editsServer();
        if (!editServer) {
            serverPage = false;
        }
        int top = 34;
        if (editServer) {
            // Two tabs: the player's preferences and the world (server) limits.
            int tabWidth = 120;
            Button clientTab = Button.builder(Component.translatable("quickstack.config.client_column"), button -> showPage(false))
                    .bounds(width / 2 - tabWidth - 2, top - 4, tabWidth, HEIGHT).build();
            Button serverTab = Button.builder(Component.translatable("quickstack.config.server_column"), button -> showPage(true))
                    .bounds(width / 2 + 2, top - 4, tabWidth, HEIGHT).build();
            clientTab.active = serverPage;
            serverTab.active = !serverPage;
            addRenderableWidget(clientTab);
            addRenderableWidget(serverTab);
            top += ROW + 2;
        }

        List<AbstractWidget> widgets = serverPage ? serverWidgets() : clientWidgets();
        // As many columns as the screen height needs, so no widget ends up off screen (a widget below the edge
        // gets a zero-height scissor, which crashes the 26.3 GUI renderer).
        int bottom = height - 30;
        int rows = Math.max(1, (bottom - top) / ROW);
        int columns = (widgets.size() + rows - 1) / rows;
        rows = (widgets.size() + columns - 1) / columns;
        int totalWidth = columns * WIDTH + (columns - 1) * 10;
        int left = width / 2 - totalWidth / 2;
        for (int i = 0; i < widgets.size(); i++) {
            AbstractWidget widget = widgets.get(i);
            widget.setX(left + (i / rows) * (WIDTH + 10));
            widget.setY(top + (i % rows) * ROW);
            widget.setWidth(WIDTH);
            addRenderableWidget(widget);
        }

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 - 100, Math.min(height - 26, top + rows * ROW + 6), 200, HEIGHT)
                .build());
    }

    private boolean editsServer() {
        return minecraft.hasSingleplayerServer() || minecraft.level == null;
    }

    private void showPage(boolean server) {
        serverPage = server;
        rebuildWidgets();
    }

    private List<AbstractWidget> clientWidgets() {
        ClientConfig client = ClientConfig.get();
        List<AbstractWidget> list = new ArrayList<>();
        list.add(new IntSlider(Component.translatable("quickstack.config.keep_in_chest"), 0, 64,
                client.keepInChest, value -> client.keepInChest = value));
        list.add(toggle("quickstack.config.animation", client.animation, value -> client.animation = value));
        list.add(toggle("quickstack.config.highlight", client.highlightTargets, value -> client.highlightTargets = value));
        list.add(toggle("quickstack.config.inventory_buttons", client.showInventoryButtons,
                value -> client.showInventoryButtons = value));
        list.add(CycleButton.<ClientConfig.ButtonPlacement>builder(placement -> Component.translatable(placement.translationKey()))
                .withInitialValue(client.buttonPlacement)
                .withValues(ClientConfig.ButtonPlacement.values())
                .create(0, 0, WIDTH, HEIGHT, Component.translatable("quickstack.config.button_placement"),
                        (button, value) -> client.buttonPlacement = value));
        list.add(toggle("quickstack.config.container_buttons", client.showContainerButtons,
                value -> client.showContainerButtons = value));
        list.add(toggle("quickstack.config.middle_click_sort", client.middleClickSort,
                value -> client.middleClickSort = value));
        list.add(CycleButton.<SortMode>builder(mode -> Component.translatable(mode.translationKey()))
                .withInitialValue(client.sortMode)
                .withValues(SortMode.values())
                .create(0, 0, WIDTH, HEIGHT, Component.translatable("quickstack.config.sort_mode"),
                        (button, value) -> client.sortMode = value));
        list.add(toggle("quickstack.config.craft_nearby_client", client.craftFromNearby,
                value -> client.craftFromNearby = value));
        list.add(CycleButton.<ClientConfig.ShiftDrag>builder(mode -> Component.translatable(mode.translationKey()))
                .withInitialValue(client.shiftDrag)
                .withValues(ClientConfig.ShiftDrag.values())
                .create(0, 0, WIDTH, HEIGHT, Component.translatable("quickstack.config.shift_drag"),
                        (button, value) -> client.shiftDrag = value));
        list.add(toggle("quickstack.config.scroll_pick", client.scrollPickAmount,
                value -> client.scrollPickAmount = value));
        if (minecraft.player != null) {
            list.add(Button.builder(Component.translatable("quickstack.config.reset_locks"),
                    button -> QuickStackClient.toggleLock(Payloads.ToggleLock.RESET_DEFAULT)).size(WIDTH, HEIGHT).build());
            list.add(Button.builder(Component.translatable("quickstack.config.unlock_all"),
                    button -> QuickStackClient.toggleLock(Payloads.ToggleLock.UNLOCK_ALL)).size(WIDTH, HEIGHT).build());
        }
        return list;
    }

    private List<AbstractWidget> serverWidgets() {
        ServerConfig server = ServerConfig.get();
        List<AbstractWidget> list = new ArrayList<>();
        list.add(new IntSlider(Component.translatable("quickstack.config.radius"), 1, 32,
                server.quickStackRadius, value -> server.quickStackRadius = value));
        list.add(new IntSlider(Component.translatable("quickstack.config.craft_radius"), 1, 32,
                server.craftRadius, value -> server.craftRadius = value));
        list.add(new IntSlider(Component.translatable("quickstack.config.search_radius"), 1, 48,
                server.searchRadius, value -> server.searchRadius = value));
        list.add(new IntSlider(Component.translatable("quickstack.config.max_inserts"), 1, 128,
                server.maxInsertsPerTick, value -> server.maxInsertsPerTick = value));
        list.add(new DoubleSlider(Component.translatable("quickstack.config.flight_speed"), 0.2, 3.0,
                server.flightSpeed, value -> server.flightSpeed = value));
        list.add(new IntSlider(Component.translatable("quickstack.config.cooldown"), 0, 100,
                server.cooldownTicks, value -> server.cooldownTicks = value));
        list.add(new IntSlider(Component.translatable("quickstack.config.undo_seconds"), 0, 60,
                server.undoSeconds, value -> server.undoSeconds = value));
        list.add(toggle("quickstack.config.craft_nearby_server", server.craftFromNearby,
                value -> server.craftFromNearby = value));
        list.add(toggle("quickstack.config.modded_containers", server.allowModdedContainers,
                value -> server.allowModdedContainers = value));
        return list;
    }

    private static CycleButton<Boolean> toggle(String key, boolean value, Consumer<Boolean> setter) {
        return CycleButton.onOffBuilder(value).create(0, 0, WIDTH, HEIGHT, Component.translatable(key),
                (button, newValue) -> setter.accept(newValue));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float a) {
        super.render(graphics, mouseX, mouseY, a);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        ClientConfig.save();
        if (editsServer()) {
            ServerConfig.save();
        }
        minecraft.setScreen(parent);
    }

    private static final class IntSlider extends AbstractSliderButton {
        private final Component label;
        private final int min;
        private final int max;
        private final IntConsumer setter;

        IntSlider(Component label, int min, int max, int value, IntConsumer setter) {
            super(0, 0, WIDTH, HEIGHT, Component.empty(), (double) (value - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.setter = setter;
            updateMessage();
        }

        private int current() {
            return min + (int) Math.round(value * (max - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(label.copy().append(": ").append(Integer.toString(current())));
        }

        @Override
        protected void applyValue() {
            setter.accept(current());
        }
    }

    private static final class DoubleSlider extends AbstractSliderButton {
        private final Component label;
        private final double min;
        private final double max;
        private final DoubleConsumer setter;

        DoubleSlider(Component label, double min, double max, double value, DoubleConsumer setter) {
            super(0, 0, WIDTH, HEIGHT, Component.empty(), Mth.clamp((value - min) / (max - min), 0, 1));
            this.label = label;
            this.min = min;
            this.max = max;
            this.setter = setter;
            updateMessage();
        }

        private double current() {
            return Math.round((min + value * (max - min)) * 10) / 10.0;
        }

        @Override
        protected void updateMessage() {
            setMessage(label.copy().append(": ").append(Double.toString(current())));
        }

        @Override
        protected void applyValue() {
            setter.accept(current());
        }
    }
}
