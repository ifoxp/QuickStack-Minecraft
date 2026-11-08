package top.ifoxp.quickstack.client.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;
import top.ifoxp.quickstack.client.QuickStackClient;
import top.ifoxp.quickstack.network.Payloads;

/**
 * Type part of an item name (in the game's language) or id; Enter highlights the nearby containers that hold a
 * matching item. Names are matched here because only the client knows the translated names.
 */
public class FindItemScreen extends Screen {

    private final @Nullable Screen parent;
    private EditBox box;
    private int matches;

    public FindItemScreen(@Nullable Screen parent) {
        super(Component.translatable("quickstack.find.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        box = new EditBox(font, width / 2 - 100, height / 2 - 10, 200, 20, Component.translatable("quickstack.find.title"));
        box.setHint(Component.translatable("quickstack.find.hint"));
        box.setMaxLength(64);
        box.setResponder(text -> matches = search(text).size());
        addRenderableWidget(box);
        setInitialFocus(box);
    }

    private static List<ItemStack> search(String text) {
        String query = text.trim().toLowerCase(Locale.ROOT);
        List<ItemStack> result = new ArrayList<>();
        if (query.isEmpty()) {
            return result;
        }
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            if (name.contains(query) || id.contains(query)) {
                result.add(stack);
                if (result.size() >= Payloads.FindItems.MAX) {
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isConfirmation()) {
            List<ItemStack> found = search(box.getValue());
            minecraft.gui.setScreen(null);
            QuickStackClient.find(found);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.centeredText(font, title, width / 2, height / 2 - 30, 0xFFFFFFFF);
        if (!box.getValue().isBlank()) {
            graphics.centeredText(font, Component.translatable("quickstack.find.matches", matches), width / 2, height / 2 + 16, 0xFFA0A0A0);
        }
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
