package top.ifoxp.quickstack.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import top.ifoxp.quickstack.client.ui.ContainerScreenAccess;

/**
 * Gives container screens' mouse wheel to QuickStack first. From 1.21.2 AbstractContainerScreen has its own
 * mouseScrolled that every container screen calls first; before that the wheel has to be caught here.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @WrapOperation(method = "onScroll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z"))
    private boolean quickstack$wheel(Screen screen, double x, double y, double scrollX, double scrollY, Operation<Boolean> original) {
        if (screen instanceof AbstractContainerScreen<?> && ((ContainerScreenAccess) screen).quickstack$wheel(scrollY)) {
            return true;
        }
        return original.call(screen, x, y, scrollX, scrollY);
    }
}
