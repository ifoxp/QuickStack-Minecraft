package top.ifoxp.quickstack.mixin;

import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CompoundContainer.class)
public interface CompoundContainerAccessor {

    @Accessor("container1")
    Container quickstack$first();

    @Accessor("container2")
    Container quickstack$second();
}
