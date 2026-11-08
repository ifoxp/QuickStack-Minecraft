package top.ifoxp.quickstack.client.ui;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import top.ifoxp.quickstack.client.QuickStackClient;
import top.ifoxp.quickstack.config.ClientConfig;
import top.ifoxp.quickstack.config.ServerConfig;
import top.ifoxp.quickstack.network.Payloads;
import top.ifoxp.quickstack.sort.SortMode;

/**
 * QuickStack settings, built from vanilla widgets (no config library). The left column holds the player's own
 * preferences; the right column holds the server limits and is only shown when this client runs the server
 * (singleplayer), because a remote server reads its own file.
 */
public class ConfigScreen extends Screen {

    private static final int WIDTH = 150;
    private static final int HEIGHT = 20;
    private static final int ROW = 22;

    private final @Nullable Screen parent;

    public ConfigScreen(@Nullable Screen parent) {
        super(Component.translatable("quickstack.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ClientConfig client = ClientConfig.get();
        boolean editServer = minecraft.hasSingleplayerServer() || minecraft.level == null;
        int left = editServer ? width / 2 - WIDTH - 5 : width / 2 - WIDTH / 2;
        int right = width / 2 + 5;
        int top = 40;

        int row = 0;
        add(left, top, row++, new IntSlider(Component.translatable("quickstack.config.keep_in_chest"), 0, 64,
                client.keepInChest, value -> client.keepInChest = value));
        add(left, top, row++, toggle("quickstack.config.animation", client.animation, value -> client.animation = value));
        add(left, top, row++, toggle("quickstack.config.highlight", client.highlightTargets, value -> client.highlightTargets = value));
        add(left, top, row++, toggle("quickstack.config.inventory_buttons", client.showInventoryButtons,
                value -> client.showInventoryButtons = value));
        add(left, top, row++, CycleButton.<ClientConfig.ButtonPlacement>builder(
                        placement -> Component.translatable(placement.translationKey()), client.buttonPlacement)
                .withValues(ClientConfig.ButtonPlacement.values())
                .create(0, 0, WIDTH, HEIGHT, Component.translatable("quickstack.config.button_placement"),
                        (button, value) -> client.buttonPlacement = value));
        add(left, top, row++, toggle("quickstack.config.container_buttons", client.showContainerButtons,
                value -> client.showContainerButtons = value));
        add(left, top, row++, toggle("quickstack.config.middle_click_sort", client.middleClickSort,
                value -> client.middleClickSort = value));
        add(left, top, row++, CycleButton.<SortMode>builder(mode -> Component.translatable(mode.translationKey()), client.sortMode)
                .withValues(SortMode.values())
                .create(0, 0, WIDTH, HEIGHT, Component.translatable("quickstack.config.sort_mode"),
                        (button, value) -> client.sortMode = value));
        add(left, top, row++, toggle("quickstack.config.craft_nearby_client", client.craftFromNearby,
                value -> client.craftFromNearby = value));
        add(left, top, row++, CycleButton.<ClientConfig.ShiftDrag>builder(
                        mode -> Component.translatable(mode.translationKey()), client.shiftDrag)
                .withValues(ClientConfig.ShiftDrag.values())
                .create(0, 0, WIDTH, HEIGHT, Component.translatable("quickstack.config.shift_drag"),
                        (button, value) -> client.shiftDrag = value));
        add(left, top, row++, toggle("quickstack.config.scroll_pick", client.scrollPickAmount,
                value -> client.scrollPickAmount = value));
        if (minecraft.player != null) {
            int half = (WIDTH - 4) / 2;
            int y = top + row++ * ROW;
            addRenderableWidget(Button.builder(Component.translatable("quickstack.config.reset_locks"),
                    button -> QuickStackClient.toggleLock(Payloads.ToggleLock.RESET_DEFAULT)).bounds(left, y, half, HEIGHT).build());
            addRenderableWidget(Button.builder(Component.translatable("quickstack.config.unlock_all"),
                    button -> QuickStackClient.toggleLock(Payloads.ToggleLock.UNLOCK_ALL)).bounds(left + WIDTH - half, y, half, HEIGHT).build());
        }

        if (editServer) {
            ServerConfig server = ServerConfig.get();
            int srow = 0;
            add(right, top, srow++, new IntSlider(Component.translatable("quickstack.config.radius"), 1, 32,
                    server.quickStackRadius, value -> server.quickStackRadius = value));
            add(right, top, srow++, new IntSlider(Component.translatable("quickstack.config.craft_radius"), 1, 32,
                    server.craftRadius, value -> server.craftRadius = value));
            add(right, top, srow++, new IntSlider(Component.translatable("quickstack.config.search_radius"), 1, 48,
                    server.searchRadius, value -> server.searchRadius = value));
            add(right, top, srow++, new IntSlider(Component.translatable("quickstack.config.max_inserts"), 1, 128,
                    server.maxInsertsPerTick, value -> server.maxInsertsPerTick = value));
            add(right, top, srow++, new DoubleSlider(Component.translatable("quickstack.config.flight_speed"), 0.2, 3.0,
                    server.flightSpeed, value -> server.flightSpeed = value));
            add(right, top, srow++, new IntSlider(Component.translatable("quickstack.config.cooldown"), 0, 100,
                    server.cooldownTicks, value -> server.cooldownTicks = value));
            add(right, top, srow++, new IntSlider(Component.translatable("quickstack.config.undo_seconds"), 0, 60,
                    server.undoSeconds, value -> server.undoSeconds = value));
            add(right, top, srow++, toggle("quickstack.config.craft_nearby_server", server.craftFromNearby,
                    value -> server.craftFromNearby = value));
            add(right, top, srow++, toggle("quickstack.config.modded_containers", server.allowModdedContainers,
                    value -> server.allowModdedContainers = value));
            row = Math.max(row, srow);
        }

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 - 100, Math.min(height - 28, top + row * ROW + 10), 200, HEIGHT)
                .build());
    }

    private void add(int x, int top, int row, AbstractWidget widget) {
        widget.setX(x);
        widget.setY(top + row * ROW);
        widget.setWidth(WIDTH);
        addRenderableWidget(widget);
    }

    private static CycleButton<Boolean> toggle(String key, boolean value, Consumer<Boolean> setter) {
        return CycleButton.onOffBuilder(value).create(0, 0, WIDTH, HEIGHT, Component.translatable(key),
                (button, newValue) -> setter.accept(newValue));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.centeredText(font, title, width / 2, 15, 0xFFFFFFFF);
        if (minecraft.hasSingleplayerServer() || minecraft.level == null) {
            graphics.centeredText(font, Component.translatable("quickstack.config.client_column"), width / 2 - WIDTH / 2 - 5, 28, 0xFFA0A0A0);
            graphics.centeredText(font, Component.translatable("quickstack.config.server_column"), width / 2 + WIDTH / 2 + 5, 28, 0xFFA0A0A0);
        }
    }

    @Override
    public void onClose() {
        ClientConfig.save();
        if (minecraft.hasSingleplayerServer() || minecraft.level == null) {
            ServerConfig.save();
        }
        minecraft.gui.setScreen(parent);
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
