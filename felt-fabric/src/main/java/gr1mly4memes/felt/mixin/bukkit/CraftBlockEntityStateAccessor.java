package gr1mly4memes.felt.mixin.bukkit;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.bukkit.craftbukkit.block.CraftBlockEntityState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = CraftBlockEntityState.class, remap = false)
public interface CraftBlockEntityStateAccessor {
    @Accessor("blockEntity")
    BlockEntity felt$getBlockEntity();
}
