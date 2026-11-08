package top.ifoxp.quickstack.sort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Merges partial stacks and orders them. Pure item logic, no slots or containers involved. */
public final class Sorter {

    private Sorter() {
    }

    /** Returns new stacks: equal items merged into as few stacks as possible, then sorted. Inputs are not modified. */
    public static List<ItemStack> mergeAndSort(List<ItemStack> stacks, SortMode mode) {
        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack remaining = stack.copy();
            for (ItemStack target : merged) {
                if (remaining.isEmpty()) {
                    break;
                }
                if (target.getCount() < target.getMaxStackSize() && ItemStack.isSameItemSameComponents(target, remaining)) {
                    int moved = Math.min(remaining.getCount(), target.getMaxStackSize() - target.getCount());
                    target.grow(moved);
                    remaining.shrink(moved);
                }
            }
            if (!remaining.isEmpty()) {
                merged.add(remaining);
            }
        }
        merged.sort(comparator(mode, merged));
        return merged;
    }

    private static Comparator<ItemStack> comparator(SortMode mode, List<ItemStack> all) {
        Comparator<ItemStack> byRegistry = Comparator.comparingInt(s -> BuiltInRegistries.ITEM.getId(s.getItem()));
        Comparator<ItemStack> tieBreak = Comparator
                .comparingInt(ItemStack::getDamageValue)
                .thenComparing(s -> s.getHoverName().getString())
                .thenComparing(Comparator.comparingInt(ItemStack::getCount).reversed());
        return switch (mode) {
            case CATEGORY -> byRegistry.thenComparing(tieBreak);
            case NAME -> Comparator.<ItemStack, String>comparing(s -> s.getHoverName().getString())
                    .thenComparing(byRegistry)
                    .thenComparing(tieBreak);
            case COUNT -> {
                Map<Item, Integer> totals = new HashMap<>();
                for (ItemStack stack : all) {
                    totals.merge(stack.getItem(), stack.getCount(), Integer::sum);
                }
                yield Comparator.<ItemStack>comparingInt(s -> -totals.get(s.getItem()))
                        .thenComparing(byRegistry)
                        .thenComparing(tieBreak);
            }
        };
    }
}
