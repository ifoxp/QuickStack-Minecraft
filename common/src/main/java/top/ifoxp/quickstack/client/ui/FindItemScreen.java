package top.ifoxp.quickstack.client.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;
import top.ifoxp.quickstack.client.ClientState;
import top.ifoxp.quickstack.client.QuickStackClient;
import top.ifoxp.quickstack.network.Payloads;

/**
 * Shows everything the containers around the player hold as a grid of icons. Typing filters it by name (in the
 * game's language) or id; items that match but are in no nearby container follow, faded. Clicking an item (or Enter
 * for all matches) closes the screen and makes the containers holding it sparkle.
 */
public class FindItemScreen extends Screen {

    private static final int COLUMNS = 9;
    private static final int ROWS = 6;
    private static final int SLOT = 18;
    private static final int PANEL_WIDTH = COLUMNS * SLOT + 14;
    private static final int PANEL_HEIGHT = 44 + ROWS * SLOT + 16;
    /** Cap on faded "not in containers" matches, so a one-letter query stays cheap. */
    private static final int MAX_MISSING = 9 * 20;

    private record Entry(ItemStack stack, int count, boolean stored) {
    }

    private record Named(Item item, String name, String id) {
    }

    private static List<Named> allItems;

    private final @Nullable Screen parent;
    private EditBox box;
    private List<Entry> entries = List.of();
    private Payloads.StorageSummary shownSummary;
    private int scrollRow;
    private int left;
    private int top;

    public FindItemScreen(@Nullable Screen parent) {
        super(Component.translatable("quickstack.find.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;
        String previous = box != null ? box.getValue() : "";
        box = new EditBox(font, left + 7, top + 20, PANEL_WIDTH - 14, 16, Component.translatable("quickstack.find.title"));
        box.setHint(Component.translatable("quickstack.find.hint"));
        box.setMaxLength(64);
        box.setValue(previous);
        box.setResponder(text -> rebuild());
        addRenderableWidget(box);
        setInitialFocus(box);
        if (shownSummary == null) {
            QuickStackClient.requestStorage();
        }
        rebuild();
    }

    @Override
    public void tick() {
        // The summary arrives a moment after opening.
        if (ClientState.storage() != shownSummary) {
            rebuild();
        }
    }

    private void rebuild() {
        shownSummary = ClientState.storage();
        String query = box == null ? "" : box.getValue().trim().toLowerCase(Locale.ROOT);
        List<Entry> stored = new ArrayList<>();
        Set<Item> storedItems = new HashSet<>();
        if (shownSummary != null) {
            for (Payloads.NearbyCount count : shownSummary.items()) {
                ItemStack stack = new ItemStack(count.item());
                storedItems.add(stack.getItem());
                if (matches(stack, query)) {
                    stored.add(new Entry(stack, count.count(), true));
                }
            }
            stored.sort(Comparator.comparingInt(e -> BuiltInRegistries.ITEM.getId(e.stack.getItem())));
        }
        List<Entry> result = new ArrayList<>(stored);
        if (!query.isEmpty()) {
            int missing = 0;
            for (Named named : allItems()) {
                if (missing >= MAX_MISSING) {
                    break;
                }
                if (!storedItems.contains(named.item) && (named.name.contains(query) || named.id.contains(query))) {
                    result.add(new Entry(new ItemStack(named.item), 0, false));
                    missing++;
                }
            }
        }
        entries = result;
        scrollRow = Math.min(scrollRow, maxScroll());
    }

    private static boolean matches(ItemStack stack, String query) {
        return query.isEmpty()
                || stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)
                || BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().contains(query);
    }

    private static List<Named> allItems() {
        if (allItems == null) {
            List<Named> list = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item != Items.AIR) {
                    list.add(new Named(item, new ItemStack(item).getHoverName().getString().toLowerCase(Locale.ROOT),
                            BuiltInRegistries.ITEM.getKey(item).toString()));
                }
            }
            allItems = list;
        }
        return allItems;
    }

    private int maxScroll() {
        int rows = (entries.size() + COLUMNS - 1) / COLUMNS;
        return Math.max(0, rows - ROWS);
    }

    private int gridX() {
        return left + 7;
    }

    private int gridY() {
        return top + 42;
    }

    private @Nullable Entry entryAt(double mouseX, double mouseY) {
        int column = (int) Math.floor((mouseX - gridX()) / SLOT);
        int row = (int) Math.floor((mouseY - gridY()) / SLOT);
        if (mouseX < gridX() || mouseY < gridY() || column >= COLUMNS || row >= ROWS) {
            return null;
        }
        int index = (scrollRow + row) * COLUMNS + column;
        return index < entries.size() ? entries.get(index) : null;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int right = left + PANEL_WIDTH;
        int bottom = top + PANEL_HEIGHT;
        graphics.fill(left, top, right, bottom, 0xFF000000);
        graphics.fill(left + 1, top + 1, right - 1, bottom - 1, 0xFFC6C6C6);
        graphics.fill(left + 1, top + 1, right - 2, top + 2, 0xFFFFFFFF);
        graphics.fill(left + 1, top + 1, left + 2, bottom - 2, 0xFFFFFFFF);
        graphics.fill(right - 2, top + 2, right - 1, bottom - 1, 0xFF555555);
        graphics.fill(left + 2, bottom - 2, right - 1, bottom - 1, 0xFF555555);
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int x = gridX() + column * SLOT;
                int y = gridY() + row * SLOT;
                graphics.fill(x, y, x + SLOT, y + SLOT, 0xFF8B8B8B);
                graphics.fill(x, y, x + SLOT - 1, y + 1, 0xFF373737);
                graphics.fill(x, y, x + 1, y + SLOT - 1, 0xFF373737);
                graphics.fill(x + 1, y + SLOT - 1, x + SLOT, y + SLOT, 0xFFFFFFFF);
                graphics.fill(x + SLOT - 1, y + 1, x + SLOT, y + SLOT, 0xFFFFFFFF);
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.text(font, title, left + 8, top + 7, 0xFF404040, false);

        int first = scrollRow * COLUMNS;
        for (int i = 0; i < ROWS * COLUMNS && first + i < entries.size(); i++) {
            Entry entry = entries.get(first + i);
            int x = gridX() + (i % COLUMNS) * SLOT + 1;
            int y = gridY() + (i / COLUMNS) * SLOT + 1;
            graphics.item(entry.stack, x, y);
            if (!entry.stored) {
                graphics.fill(x, y, x + 16, y + 16, 0xB08B8B8B);
            }
        }
        // Counts at 2/3 size, so totals like "1.2k" or "130" stay inside their slot.
        for (int i = 0; i < ROWS * COLUMNS && first + i < entries.size(); i++) {
            Entry entry = entries.get(first + i);
            if (!entry.stored) {
                continue;
            }
            String count = shortCount(entry.count);
            int x = gridX() + (i % COLUMNS) * SLOT + 1;
            int y = gridY() + (i / COLUMNS) * SLOT + 1;
            graphics.pose().pushMatrix();
            graphics.pose().translate(x + 17, y + 17);
            graphics.pose().scale(2.0F / 3.0F, 2.0F / 3.0F);
            graphics.text(font, count, -font.width(count), -font.lineHeight + 1, 0xFFFFFFFF, true);
            graphics.pose().popMatrix();
        }

        Component status;
        if (shownSummary == null) {
            status = Component.translatable("quickstack.find.loading");
        } else {
            long stored = entries.stream().filter(Entry::stored).count();
            status = Component.translatable("quickstack.find.summary", stored, shownSummary.radius());
        }
        graphics.text(font, status, left + 8, gridY() + ROWS * SLOT + 4, 0xFF404040, false);
        if (maxScroll() > 0) {
            String pages = (scrollRow + 1) + "/" + (maxScroll() + 1);
            graphics.text(font, pages, left + PANEL_WIDTH - 8 - font.width(pages), gridY() + ROWS * SLOT + 4, 0xFF404040, false);
        }

        Entry hovered = entryAt(mouseX, mouseY);
        if (hovered != null) {
            List<Component> lines = new ArrayList<>();
            lines.add(hovered.stack.getHoverName());
            if (hovered.stored) {
                lines.add(Component.translatable("quickstack.find.in_containers", hovered.count).withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable("quickstack.find.click").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                lines.add(Component.translatable("quickstack.find.not_in_containers").withStyle(ChatFormatting.DARK_GRAY));
            }
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }

    private static String shortCount(int count) {
        if (count < 1000) {
            return Integer.toString(count);
        }
        if (count < 10_000) {
            return String.format(Locale.ROOT, "%.1fk", count / 1000.0);
        }
        return (count / 1000) + "k";
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Entry entry = entryAt(event.x(), event.y());
        if (entry != null && entry.stored) {
            minecraft.gui.setScreen(null);
            QuickStackClient.find(List.of(entry.stack));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY != 0) {
            scrollRow = Math.clamp(scrollRow - (int) Math.signum(scrollY), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isConfirmation()) {
            List<ItemStack> found = entries.stream().filter(Entry::stored).map(Entry::stack).toList();
            minecraft.gui.setScreen(null);
            QuickStackClient.find(found);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
