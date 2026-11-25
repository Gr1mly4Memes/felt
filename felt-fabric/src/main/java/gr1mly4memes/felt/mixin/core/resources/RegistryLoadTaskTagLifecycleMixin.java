package gr1mly4memes.felt.mixin.core.resources;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Lifecycle;
import gr1mly4memes.felt.mod.util.FeltTagLifecycle;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryLoadTask;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceManagerRegistryLoadTask;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ResourceManagerRegistryLoadTask.class)
public abstract class RegistryLoadTaskTagLifecycleMixin<T> extends RegistryLoadTask<T> {
    protected RegistryLoadTaskTagLifecycleMixin(RegistryDataLoader.RegistryData<T> data, Lifecycle lifecycle, Map<ResourceKey<?>, Exception> errors) {
        super(data, lifecycle, errors);
    }

    @WrapOperation(method = "lambda$load$3", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/tags/TagLoader;loadTagsForRegistry(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/tags/TagLoader$ElementLookup;)Ljava/util/Map;"))
    private Map<TagKey<T>, List<Holder<T>>> felt$scopeDynamicRegistry(ResourceManager manager, ResourceKey<? extends Registry<T>> key,
            TagLoader.ElementLookup<Holder<T>> lookup, Operation<Map<TagKey<T>, List<Holder<T>>>> original) {
        return FeltTagLifecycle.withRegistry(this.readOnlyRegistry(), () -> original.call(manager, key, lookup));
    }
}
