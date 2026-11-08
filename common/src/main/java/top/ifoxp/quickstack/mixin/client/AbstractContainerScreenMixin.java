package top.ifoxp.quickstack.mixin.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.ifoxp.quickstack.client.ClientState;
import top.ifoxp.quickstack.client.QuickStackClient;
import top.ifoxp.quickstack.client.ui.ContainerScreenAccess;
import top.ifoxp.quickstack.client.ui.FindItemScreen;
import top.ifoxp.quickstack.client.ui.ScreenButtons;
import top.ifoxp.quickstack.config.ClientConfig;
import top.ifoxp.quickstack.network.MenuAction;
import top.ifoxp.quickstack.network.Payloads;
import top.ifoxp.quickstack.platform.Services;

/**
 * Buttons on every container screen, lock marks on locked slots, Alt+click to lock, middle click to sort, Shift
 * sweeping, one-item moves with the mouse wheel, and the mod's keys while a container screen is open.
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

    @Shadow protected abstract void slotClicked(Slot slot, int slotId, int buttonNum, ClickType clickType);

    @Shadow
    private @Nullable Slot getHoveredSlot(double x, double y) {
        throw new AssertionError();
    }

    @Unique private final Set<Slot> quickstack$swept = new HashSet<>();
    @Unique private final Set<Slot> quickstack$hoverSwept = new HashSet<>();
    @Unique private boolean quickstack$sweeping;

    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void quickstack$addButtons(CallbackInfo ci) {
        ScreenButtons.addButtons((AbstractContainerScreen<?>) (Object) this, this::quickstack$addWidget, this::quickstack$addRenderable);
    }

    private void quickstack$addWidget(AbstractWidget widget) {
        this.addRenderableWidget(widget);
    }

    private void quickstack$addRenderable(Renderable renderable) {
        this.addRenderableOnly(renderable);
    }

    @Inject(method = "renderSlot", at = @At("RETURN"))
    private void quickstack$drawLock(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
        ScreenButtons.drawSlotOverlay(graphics, slot);
    }

    // ---- Shift sweep: shift-click every slot the cursor passes over ----

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void quickstack$startSweep(double x, double y, int button, CallbackInfoReturnable<Boolean> cir) {
        quickstack$swept.clear();
        quickstack$sweeping = button == InputConstants.MOUSE_BUTTON_LEFT
                && ClientConfig.get().shiftDrag == ClientConfig.ShiftDrag.WITH_MOUSE
                && hasShiftDown()
                && menu.getCarried().isEmpty()
                && !((Object) this instanceof CreativeModeInventoryScreen);
        if (!quickstack$sweeping || hoveredSlot == null) {
            return;
        }
        // Vanilla reads Shift from the keyboard here too and shift-clicks this first one itself.
        quickstack$swept.add(hoveredSlot);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void quickstack$sweep(double x, double y, int button, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        if (!quickstack$sweeping || !hasShiftDown()) {
            return;
        }
        Slot slot = getHoveredSlot(x, y);
        if (slot != null) {
            quickstack$sweepSlot(slot);
        }
        cir.setReturnValue(true);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void quickstack$endSweep(double x, double y, int button, CallbackInfoReturnable<Boolean> cir) {
        quickstack$sweeping = false;
    }

    /** The "Shift only" mode: no button needed, checked every frame. */
    @Inject(method = "renderContents", at = @At("TAIL"))
    private void quickstack$hoverSweep(GuiGraphics graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (ClientConfig.get().shiftDrag != ClientConfig.ShiftDrag.SHIFT_ONLY
                || (Object) this instanceof CreativeModeInventoryScreen || !hasShiftDown()) {
            quickstack$hoverSwept.clear();
            return;
        }
        if (hoveredSlot != null && quickstack$hoverSwept.add(hoveredSlot)) {
            quickstack$sweepSlot(hoveredSlot);
        }
    }

    private void quickstack$sweepSlot(Slot slot) {
        boolean locked = ScreenButtons.isLockableSlot(slot) && ClientState.isLocked(slot.getContainerSlot());
        if (slot.hasItem() && !locked && menu.getCarried().isEmpty() && quickstack$swept.add(slot)) {
            slotClicked(slot, slot.index, 0, ClickType.QUICK_MOVE);
        }
    }

    // ---- Mouse wheel: one item at a time, like right clicks ----

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void quickstack$wheel(double x, double y, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        Slot slot = hoveredSlot;
        if (!ClientConfig.get().scrollPickAmount || slot == null || scrollY == 0 || minecraft.player == null
                || (Object) this instanceof CreativeModeInventoryScreen) {
            return;
        }
        // Bundles use the wheel to choose their item.
        if (slot.getItem().has(DataComponents.BUNDLE_CONTENTS) || menu.getCarried().has(DataComponents.BUNDLE_CONTENTS)) {
            return;
        }
        ItemStack carried = menu.getCarried();
        ItemStack inSlot = slot.getItem();
        if (scrollY > 0) {
            // Up: one more item from the slot onto the cursor.
            boolean fits = carried.isEmpty() || (ItemStack.isSameItemSameComponents(carried, inSlot) && carried.getCount() < carried.getMaxStackSize());
            if (inSlot.isEmpty() || !fits || !QuickStackClient.serverHasMod()) {
                return;
            }
            Services.client().sendToServer(new Payloads.TakeFromSlot(menu.containerId, slot.index, 1));
            if (carried.isEmpty()) {
                menu.setCarried(inSlot.copyWithCount(1)); // predicted; the server's sync confirms it
            } else {
                carried.grow(1);
            }
            inSlot.shrink(1);
        } else {
            // Down: one item from the cursor into the slot, exactly a vanilla right click.
            boolean fits = inSlot.isEmpty() || ItemStack.isSameItemSameComponents(carried, inSlot);
            if (carried.isEmpty() || !fits || !slot.mayPlace(carried)) {
                return;
            }
            slotClicked(slot, slot.index, 1, ClickType.PICKUP);
        }
        cir.setReturnValue(true);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void quickstack$mouseClicked(double x, double y, int button, CallbackInfoReturnable<Boolean> cir) {
        Slot slot = this.hoveredSlot;
        if (slot == null || minecraft.player == null || !menu.getCarried().isEmpty() || !QuickStackClient.serverHasMod()) {
            return;
        }
        // Mouse buttons are GLFW numbers in 1.21.x (left = 0); modifiers are read from the keyboard.
        if (button == InputConstants.MOUSE_BUTTON_LEFT && hasAltDown() && ScreenButtons.isLockableSlot(slot)) {
            QuickStackClient.toggleLock(slot.getContainerSlot());
            AbstractWidget.playButtonClickSound(minecraft.getSoundManager());
            cir.setReturnValue(true);
            return;
        }
        if (button == InputConstants.MOUSE_BUTTON_MIDDLE && ClientConfig.get().middleClickSort && !minecraft.player.hasInfiniteMaterials()) {
            if (sortUnder(slot)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void quickstack$keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof CreativeModeInventoryScreen || minecraft.player == null) {
            return;
        }
        // Typing in a text field (anvil name, recipe search) must not trigger the mod's keys.
        if (getFocused() instanceof EditBox box && box.canConsumeInput()) {
            return;
        }
        if (QuickStackClient.QUICK_STACK.matches(keyCode, scanCode)) {
            QuickStackClient.quickStackNearby();
            cir.setReturnValue(true);
        } else if (QuickStackClient.UNDO.matches(keyCode, scanCode)) {
            QuickStackClient.undo();
            cir.setReturnValue(true);
        } else if (QuickStackClient.SORT.matches(keyCode, scanCode)) {
            if (hoveredSlot == null || !sortUnder(hoveredSlot)) {
                QuickStackClient.menuAction(MenuAction.SORT_INVENTORY);
            }
            cir.setReturnValue(true);
        } else if (QuickStackClient.FIND.matches(keyCode, scanCode)) {
            if (hoveredSlot != null && hoveredSlot.hasItem()) {
                // Close first so the highlighted containers are visible.
                var stack = hoveredSlot.getItem().copy();
                minecraft.player.closeContainer();
                QuickStackClient.find(List.of(stack));
            } else {
                minecraft.setScreen(new FindItemScreen(null));
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
