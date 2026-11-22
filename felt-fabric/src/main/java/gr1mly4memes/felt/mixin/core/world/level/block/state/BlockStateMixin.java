package gr1mly4memes.felt.mixin.core.world.level.block.state;

import gr1mly4memes.felt.bridge.core.world.level.block.state.BlockStateBridge;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Material;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockState.class)
public class BlockStateMixin implements BlockStateBridge {

    @Override
    public CraftBlockData asBlockData() {
        return CraftBlockData.createData((BlockState) (Object) this);
    }

    @Override
    public Material getBukkitMaterial() {
        return CraftMagicNumbers.getMaterial(((BlockState) (Object) this).getBlock());
    }
}
