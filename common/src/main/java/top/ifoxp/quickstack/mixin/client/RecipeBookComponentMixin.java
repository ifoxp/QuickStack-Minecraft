package top.ifoxp.quickstack.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ifoxp.quickstack.client.ClientState;
import top.ifoxp.quickstack.config.ClientConfig;
import top.ifoxp.quickstack.network.Payloads;
import top.ifoxp.quickstack.server.NearbyCrafting;

/** Lets the recipe book count items in nearby containers, so recipes craftable from them show as craftable. */
@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin {

    @Shadow private int timesInventoryChanged;

    @Unique private int quickstack$nearbyVersion = -1;

    @WrapOperation(
            method = {"initVisuals", "updateStackedContents"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/RecipeBookMenu;fillCraftSlotsStackedContents(Lnet/minecraft/world/entity/player/StackedContents;)V"))
    private void quickstack$addNearby(RecipeBookMenu<?, ?> menu, StackedContents contents, Operation<Void> original) {
        original.call(menu, contents);
        if (NearbyCrafting.isCraftingGrid(menu) && ClientConfig.get().craftFromNearby) {
            for (Payloads.NearbyCount entry : ClientState.nearbyItems()) {
                contents.accountStack(new ItemStack(entry.item(), entry.count()), entry.count());
            }
        }
    }

    /** A fresh nearby summary arrived: make the next tick recount, as if the inventory had changed. */
    @Inject(method = "tick", at = @At("HEAD"))
    private void quickstack$refreshNearby(CallbackInfo ci) {
        int version = ClientState.nearbyVersion();
        if (quickstack$nearbyVersion != version) {
            quickstack$nearbyVersion = version;
            timesInventoryChanged = -1;
        }
    }
}
