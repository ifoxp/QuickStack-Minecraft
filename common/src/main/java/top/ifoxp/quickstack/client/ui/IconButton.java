package top.ifoxp.quickstack.client.ui;

import java.util.function.IntSupplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import top.ifoxp.quickstack.QuickStack;

/**
 * A 10x10 button with an 8x8 icon from the mod's icon strip. Its position comes from suppliers so it follows the
 * screen when the recipe book shifts it.
 */
public class IconButton extends AbstractButton {

    public static final Identifier ICONS = QuickStack.id("textures/gui/icons.png");
    public static final int SIZE = 10;
    private static final int ATLAS_WIDTH = 128;
    private static final int ATLAS_HEIGHT = 16;

    private final IntSupplier xSupplier;
    private final IntSupplier ySupplier;
    private final Icon icon;
    private final Runnable action;

    public IconButton(IntSupplier x, IntSupplier y, Icon icon, Component tooltip, Runnable action) {
        super(x.getAsInt(), y.getAsInt(), SIZE, SIZE, tooltip);
        this.xSupplier = x;
        this.ySupplier = y;
        this.icon = icon;
        this.action = action;
        this.setTooltip(Tooltip.create(tooltip));
    }

    public Icon icon() {
        return icon;
    }

    private void reposition() {
        this.setX(xSupplier.getAsInt());
        this.setY(ySupplier.getAsInt());
    }

    @Override
    public void onPress(InputWithModifiers input) {
        action.run();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        reposition();
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        reposition();
        int x = getX();
        int y = getY();
        boolean hot = isHoveredOrFocused();
        graphics.fill(x, y, x + SIZE, y + SIZE, hot ? 0xFFFFFFFF : 0xFF373737);
        graphics.fill(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, hot ? 0xFFB5B5B5 : 0xFF8B8B8B);
        drawIcon(graphics, icon, x + 1, y + 1);
    }

    public static void drawIcon(GuiGraphicsExtractor graphics, Icon icon, int x, int y) {
        drawIcon(graphics, icon, x, y, 0xFFFFFFFF);
    }

    /** {@code color} is ARGB; its alpha fades the icon. */
    public static void drawIcon(GuiGraphicsExtractor graphics, Icon icon, int x, int y, int color) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, ICONS, x, y, icon.u(), 0, 8, 8, ATLAS_WIDTH, ATLAS_HEIGHT, color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }

    /** Positions in textures/gui/icons.png (8x8 each, left to right). */
    public enum Icon {
        QUICK_STACK,
        SORT,
        DEPOSIT_MATCHING,
        RESTOCK,
        DEPOSIT_ALL,
        LOOT_ALL,
        EXCLUDE,
        FIND,
        CONFIG,
        LOCK,
        SMALL_LOCK,
        UNDO;

        public int u() {
            return ordinal() * 8;
        }
    }
}
