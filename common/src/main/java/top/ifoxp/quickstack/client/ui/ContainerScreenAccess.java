package top.ifoxp.quickstack.client.ui;

import net.minecraft.world.inventory.Slot;

/** Layout of a container screen, exposed by AbstractContainerScreenMixin (the fields are protected). */
public interface ContainerScreenAccess {

    int quickstack$leftPos();

    int quickstack$topPos();

    int quickstack$imageWidth();

    int quickstack$imageHeight();

    int quickstack$titleLabelY();

    int quickstack$inventoryLabelY();

    Slot quickstack$hoveredSlot();
}
