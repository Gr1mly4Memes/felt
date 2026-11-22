package gr1mly4memes.felt.mixin.core.world.level.block;

import gr1mly4memes.felt.mixin.core.world.SimpleContainerMixin;
import gr1mly4memes.felt.mod.mixins.annotation.CreateConstructor;
import gr1mly4memes.felt.mod.mixins.annotation.ShadowConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ComposterBlock;
import org.bukkit.craftbukkit.inventory.CraftBlockInventoryHolder;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ComposterBlock.EmptyContainer.class)
public abstract class ComposterBlock_EmptyContainerMixin extends SimpleContainerMixin {

    @ShadowConstructor
    public void felt$constructor() {
        throw new RuntimeException();
    }

    @CreateConstructor
    public void felt$constructor(LevelAccessor world, BlockPos blockPos) {
        felt$constructor();
        this.setOwner(new CraftBlockInventoryHolder(world, blockPos, this));
    }
}
