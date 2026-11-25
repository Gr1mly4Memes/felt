package gr1mly4memes.felt.mixin.core.world.level.chunk.storage;

import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(net.minecraft.world.level.chunk.storage.SerializableChunkData.SectionData.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonriseSectionDataMixin implements ca.spottedleaf.moonrise.patches.starlight.storage.StarlightSectionData {
    @Unique private int felt$blockState = -1;
    @Unique private int felt$skyState = -1;
    @Override public int starlight$getBlockLightState() { return this.felt$blockState; }
    @Override public int starlight$getSkyLightState() { return this.felt$skyState; }
    @Override public void starlight$setBlockLightState(int value) { this.felt$blockState = value; }
    @Override public void starlight$setSkyLightState(int value) { this.felt$skyState = value; }

}
