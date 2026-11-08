package top.ifoxp.quickstack.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.ifoxp.quickstack.server.NearbyCrafting;

/**
 * Crafting from nearby containers: the recipe book placement counts their items and, when the inventory has run
 * out of an ingredient, moves it from a container straight into the grid slot. (Before 1.21.2 the placement is an
 * instance per click working on StackedContents; the logic is the same as on newer versions.)
 */
@Mixin(ServerPlaceRecipe.class)
public abstract class ServerPlaceRecipeMixin {

    @Shadow protected Inventory inventory;
    @Shadow @Final protected StackedContents stackedContents;

    /** After the inventory and the grid were counted, the nearby containers are counted too. */
    @Inject(method = "recipeClicked",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/RecipeBookMenu;fillCraftSlotsStackedContents(Lnet/minecraft/world/entity/player/StackedContents;)V",
                    shift = At.Shift.AFTER))
    private void quickstack$accountNearby(ServerPlayer player, RecipeHolder<?> recipe, boolean useMaxItems, CallbackInfo ci) {
        NearbyCrafting.account(player, stackedContents);
    }

    /** Clearing the grid for a new recipe sends borrowed ingredients back to their containers first. */
    @WrapOperation(
            method = "clearGrid",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Inventory;placeItemBackInInventory(Lnet/minecraft/world/item/ItemStack;Z)V"))
    private void quickstack$returnLoans(Inventory instance, ItemStack stack, boolean sendPacket,
            Operation<Void> original) {
        if (instance.player instanceof ServerPlayer player) {
            NearbyCrafting.returnLoans(player, stack);
        }
        if (!stack.isEmpty()) {
            original.call(instance, stack, sendPacket);
        }
    }

    @Inject(method = "moveItemToGrid", at = @At("HEAD"), cancellable = true)
    private void quickstack$pullFromNearby(Slot targetSlot, ItemStack ingredient, int count, CallbackInfoReturnable<Integer> cir) {
        if (inventory.findSlotMatchingUnusedItem(ingredient) != -1) {
            return; // vanilla takes it from the inventory first
        }
        ItemStack inGrid = targetSlot.getItem();
        ItemStack taken = NearbyCrafting.pull(inventory.player, ingredient.getItemHolder(), inGrid, count);
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
