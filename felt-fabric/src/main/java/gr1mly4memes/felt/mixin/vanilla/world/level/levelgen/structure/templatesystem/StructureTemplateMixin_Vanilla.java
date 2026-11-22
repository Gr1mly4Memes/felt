package gr1mly4memes.felt.mixin.vanilla.world.level.levelgen.structure.templatesystem;

import gr1mly4memes.felt.bridge.core.world.level.levelgen.structure.templatesystem.StructureTemplateBridge;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = StructureTemplate.class)
public abstract class StructureTemplateMixin_Vanilla implements StructureTemplateBridge {

}
