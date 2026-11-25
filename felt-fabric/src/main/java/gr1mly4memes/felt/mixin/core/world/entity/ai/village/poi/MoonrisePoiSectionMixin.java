package gr1mly4memes.felt.mixin.core.world.entity.ai.village.poi;

import ca.spottedleaf.moonrise.patches.chunk_system.level.poi.ChunkSystemPoiSection;
import gr1mly4memes.felt.mod.mixins.annotation.LoadIfMod;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.entity.ai.village.poi.PoiSection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PoiSection.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class MoonrisePoiSectionMixin implements ChunkSystemPoiSection {
    @Shadow @Final private Map<?, ?> byType;
    @Unique private final Optional<PoiSection> felt$optional = Optional.of((PoiSection) (Object) this);
    @Override public boolean moonrise$isEmpty() { return this.byType.isEmpty(); }
    @Override public Optional<PoiSection> moonrise$asOptional() { return this.felt$optional; }
}
