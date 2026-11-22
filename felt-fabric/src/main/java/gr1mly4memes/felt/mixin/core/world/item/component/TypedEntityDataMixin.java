package gr1mly4memes.felt.mixin.core.world.item.component;

import gr1mly4memes.felt.mod.mixins.annotation.TransformAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TypedEntityData.class)
public abstract class TypedEntityDataMixin implements gr1mly4memes.felt.bridge.core.world.item.component.TypedEntityDataBridge {

    @Override
    public CompoundTag copyTagWithEntityId() {
        TypedEntityData<?> data = (TypedEntityData<?>) (Object) this;
        CompoundTag tag = data.copyTagWithoutId();
        tag.putString("id", EntityType.getKey((EntityType<?>) data.type()).toString());
        return tag;
    }

    @Override
    public CompoundTag copyTagWithBlockEntityId() {
        TypedEntityData<?> data = (TypedEntityData<?>) (Object) this;
        CompoundTag tag = data.copyTagWithoutId();
        tag.putString("id", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey((BlockEntityType<?>) data.type()).toString());
        return tag;
    }

    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static TypedEntityData<EntityType<?>> decodeEntity(CompoundTag tag) {
        return TypedEntityData.codec(EntityType.CODEC).decode(NbtOps.INSTANCE, tag).result().orElseThrow().getFirst();
    }

    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static TypedEntityData<BlockEntityType<?>> decodeBlockEntity(CompoundTag tag) {
        return TypedEntityData.codec(BuiltInRegistries.BLOCK_ENTITY_TYPE.byNameCodec()).decode(NbtOps.INSTANCE, tag).result().orElseThrow().getFirst();
    }
}
