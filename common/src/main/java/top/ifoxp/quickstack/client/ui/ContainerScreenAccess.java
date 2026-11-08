package top.ifoxp.quickstack.client.ui;

import net.minecraft.world.inventory.Slot;

/** Layout of a container screen and its wheel handling, exposed by AbstractContainerScreenMixin (the fields are protected). */
public interface ContainerScreenAccess {

    int quickstack$leftPos();

    int quickstack$topPos();

    int quickstack$imageWidth();

    int quickstack$imageHeight();

    int quickstack$titleLabelY();

    int quickstack$inventoryLabelY();

    Slot quickstack$hoveredSlot();

    /** Mouse wheel over the screen; true when QuickStack used it (one item moved). */
    boolean quickstack$wheel(double scrollY);
}
