package gr1mly4memes.felt.common.mixin.core.world.level.block;

import gr1mly4memes.felt.common.bridge.core.world.level.block.MultifaceSpreaderSpreadPosBridge;
import gr1mly4memes.felt.common.mod.mixins.annotation.CreateConstructor;
import gr1mly4memes.felt.common.mod.mixins.annotation.ShadowConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.MultifaceSpreader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MultifaceSpreader.SpreadPos.class)
public abstract class MultifaceSpreader_SpreadPosMixin implements MultifaceSpreaderSpreadPosBridge {
    @Unique private BlockPos source;

    @ShadowConstructor
    abstract void arclight$constructor$this(BlockPos pos, Direction face);

    @SuppressWarnings("unused")
    @Unique
    @CreateConstructor
    public void arclight$constructor$new(BlockPos pos, Direction face, BlockPos source) {
        arclight$constructor$this(pos, face);
        arclight$setSource(source);
    }

    @Override
    public void arclight$setSource(BlockPos source) {
        this.source = source;
    }

    @Unique
    @Override
    public BlockPos source() {
        return source;
    }
}
