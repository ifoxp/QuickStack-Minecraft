package top.ifoxp.quickstack.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.ifoxp.quickstack.server.NearbyCrafting;

/**
 * Crafting from nearby containers: the recipe book placement counts their items and, when the inventory has run
 * out of an ingredient, moves it from a container straight into the grid slot.
 */
@Mixin(ServerPlaceRecipe.class)
public abstract class ServerPlaceRecipeMixin {

    @Shadow @Final private Inventory inventory;

    @WrapOperation(
            method = "placeRecipe(Lnet/minecraft/recipebook/ServerPlaceRecipe$CraftingMenuAccess;IILjava/util/List;Ljava/util/List;Lnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/crafting/RecipeHolder;ZZ)Lnet/minecraft/world/inventory/RecipeBookMenu$PostPlaceAction;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/recipebook/ServerPlaceRecipe$CraftingMenuAccess;fillCraftSlotsStackedContents(Lnet/minecraft/world/entity/player/StackedItemContents;)V"))
    private static void quickstack$accountNearby(ServerPlaceRecipe.CraftingMenuAccess<?> menu, StackedItemContents contents,
            Operation<Void> original, @Local(argsOnly = true) Inventory inventory) {
        original.call(menu, contents);
        NearbyCrafting.account(inventory.player, contents);
    }

    @Inject(method = "moveItemToGrid", at = @At("HEAD"), cancellable = true)
    private void quickstack$pullFromNearby(Slot targetSlot, Holder<Item> item, int count, CallbackInfoReturnable<Integer> cir) {
        ItemStack inGrid = targetSlot.getItem();
        if (inventory.findSlotMatchingCraftingIngredient(item, inGrid) != -1) {
            return; // vanilla takes it from the inventory first
        }
        ItemStack taken = NearbyCrafting.pull(inventory.player, item, inGrid, count);
        if (taken.isEmpty()) {
            return;
        }
        if (inGrid.isEmpty()) {
            targetSlot.set(taken);
        } else {
            inGrid.grow(taken.getCount());
        }
        cir.setReturnValue(count - taken.getCount());
    }
}
