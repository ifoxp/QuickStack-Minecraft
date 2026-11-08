package top.ifoxp.quickstack.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ifoxp.quickstack.server.NearbyCrafting;

/**
 * Closing a crafting table or the inventory empties the grid into the player's inventory. Ingredients that were
 * borrowed from nearby containers go back to those containers instead.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    @Inject(method = "clearContainer", at = @At("HEAD"))
    private void quickstack$returnLoans(Player player, Container container, CallbackInfo ci) {
        if (!NearbyCrafting.isCraftingGrid((AbstractContainerMenu) (Object) this) || !(player instanceof ServerPlayer serverPlayer)
                || !NearbyCrafting.hasLoans(serverPlayer)) {
            return;
        }
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                NearbyCrafting.returnLoans(serverPlayer, stack);
                container.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
            }
        }
        NearbyCrafting.clearLoans(serverPlayer.getUUID());
    }
}
