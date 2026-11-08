package top.ifoxp.quickstack.mixin.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.ifoxp.quickstack.client.QuickStackClient;
import top.ifoxp.quickstack.client.ui.ContainerScreenAccess;
import top.ifoxp.quickstack.client.ui.FindItemScreen;
import top.ifoxp.quickstack.client.ui.ScreenButtons;
import top.ifoxp.quickstack.config.ClientConfig;
import top.ifoxp.quickstack.network.MenuAction;

/**
 * Buttons on every container screen, lock marks on locked slots, Alt+click to lock, middle click to sort, and the
 * mod's keys while a container screen is open.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen implements ContainerScreenAccess {

    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow @Final protected int imageWidth;
    @Shadow @Final protected int imageHeight;
    @Shadow protected int titleLabelY;
    @Shadow protected int inventoryLabelY;
    @Shadow protected @Nullable Slot hoveredSlot;
    @Shadow @Final protected AbstractContainerMenu menu;

    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void quickstack$addButtons(CallbackInfo ci) {
        ScreenButtons.addButtons((AbstractContainerScreen<?>) (Object) this, this::quickstack$addWidget);
    }

    private void quickstack$addWidget(AbstractWidget widget) {
        this.addRenderableWidget(widget);
    }

    @Inject(method = "extractSlot", at = @At("RETURN"))
    private void quickstack$drawLock(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        ScreenButtons.drawSlotOverlay(graphics, slot);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void quickstack$mouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        Slot slot = this.hoveredSlot;
        if (slot == null || minecraft.player == null || !menu.getCarried().isEmpty() || !QuickStackClient.serverHasMod()) {
            return;
        }
        // Mouse buttons are SDL numbers since 26.1 (left = 1). The event's modifiers can miss a held Alt, so ask the keyboard too.
        boolean alt = event.hasAltDown() || minecraft.hasAltDown();
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && alt && ScreenButtons.isLockableSlot(slot)) {
            QuickStackClient.toggleLock(slot.getContainerSlot());
            AbstractWidget.playButtonClickSound(minecraft.getSoundManager());
            cir.setReturnValue(true);
            return;
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_MIDDLE && ClientConfig.get().middleClickSort && !minecraft.player.hasInfiniteMaterials()) {
            if (sortUnder(slot)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void quickstack$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof CreativeModeInventoryScreen || minecraft.player == null) {
            return;
        }
        // Typing in a text field (anvil name, recipe search) must not trigger the mod's keys.
        if (getFocused() instanceof EditBox box && box.canConsumeInput()) {
            return;
        }
        if (QuickStackClient.QUICK_STACK.matches(event)) {
            QuickStackClient.quickStackNearby();
            cir.setReturnValue(true);
        } else if (QuickStackClient.UNDO.matches(event)) {
            QuickStackClient.undo();
            cir.setReturnValue(true);
        } else if (QuickStackClient.SORT.matches(event)) {
            if (hoveredSlot == null || !sortUnder(hoveredSlot)) {
                QuickStackClient.menuAction(MenuAction.SORT_INVENTORY);
            }
            cir.setReturnValue(true);
        } else if (QuickStackClient.FIND.matches(event)) {
            if (hoveredSlot != null && hoveredSlot.hasItem()) {
                // Close first so the highlighted containers are visible.
                var stack = hoveredSlot.getItem().copy();
                minecraft.player.closeContainer();
                QuickStackClient.find(List.of(stack));
            } else {
                minecraft.gui.setScreen(new FindItemScreen(null));
            }
            cir.setReturnValue(true);
        }
    }

    /** Sorts the inventory or the container under the cursor; false when the slot belongs to neither. */
    private boolean sortUnder(Slot slot) {
        if (ScreenButtons.isLockableSlot(slot)) {
            QuickStackClient.menuAction(MenuAction.SORT_INVENTORY);
            return true;
        }
        if (ScreenButtons.isStorageMenu(menu)) {
            QuickStackClient.menuAction(MenuAction.SORT_CONTAINER);
            return true;
        }
        return false;
    }

    @Override
    public int quickstack$leftPos() {
        return leftPos;
    }

    @Override
    public int quickstack$topPos() {
        return topPos;
    }

    @Override
    public int quickstack$imageWidth() {
        return imageWidth;
    }

    @Override
    public int quickstack$imageHeight() {
        return imageHeight;
    }

    @Override
    public int quickstack$titleLabelY() {
        return titleLabelY;
    }

    @Override
    public int quickstack$inventoryLabelY() {
        return inventoryLabelY;
    }

    @Override
    public Slot quickstack$hoveredSlot() {
        return hoveredSlot;
    }
}
