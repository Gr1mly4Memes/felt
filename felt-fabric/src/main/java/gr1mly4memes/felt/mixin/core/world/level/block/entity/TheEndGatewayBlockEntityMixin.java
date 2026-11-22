package gr1mly4memes.felt.mixin.core.world.level.block.entity;

import gr1mly4memes.felt.bridge.core.world.level.block.entity.TheEndGatewayBlockEntityBridge;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TheEndGatewayBlockEntity.class)
public abstract class TheEndGatewayBlockEntityMixin extends BlockEntityMixin implements TheEndGatewayBlockEntityBridge {

}
