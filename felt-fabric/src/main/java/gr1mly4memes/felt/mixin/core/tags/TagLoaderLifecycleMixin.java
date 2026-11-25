package gr1mly4memes.felt.mixin.core.tags;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gr1mly4memes.felt.mod.util.FeltTagLifecycle;
import io.papermc.paper.tag.PaperTagListenerManager;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TagLoader.class)
public abstract class TagLoaderLifecycleMixin {
    @WrapOperation(method = "loadPendingTags", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/tags/TagLoader;build(Ljava/util/Map;)Ljava/util/Map;"))
    private static <T> Map<Identifier, List<Holder<T>>> felt$buildFrozenTags(TagLoader<Holder<T>> loader,
            Map<Identifier, List<TagLoader.EntryWithSource>> entries, Operation<Map<Identifier, List<Holder<T>>>> original,
            ResourceManager resources, Registry<T> registry) {
        return felt$buildTags(loader, entries, original, registry);
    }

    @WrapOperation(method = "loadTagsForRegistry(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/tags/TagLoader$ElementLookup;)Ljava/util/Map;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/tags/TagLoader;build(Ljava/util/Map;)Ljava/util/Map;"))
    @SuppressWarnings("unchecked")
    private static <T> Map<Identifier, List<Holder<T>>> felt$buildWritableTags(TagLoader<Holder<T>> loader,
            Map<Identifier, List<TagLoader.EntryWithSource>> entries, Operation<Map<Identifier, List<Holder<T>>>> original) {
        var registry = (Registry<T>) FeltTagLifecycle.takeRegistry();
        return registry == null ? original.call(loader, entries) : felt$buildTags(loader, entries, original, registry);
    }

    @WrapOperation(method = "loadTagsForRegistry(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/core/WritableRegistry;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/tags/TagLoader;loadTagsForRegistry(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/tags/TagLoader$ElementLookup;)Ljava/util/Map;"))
    private static <T> Map<TagKey<T>, List<Holder<T>>> felt$scopeWritableRegistry(ResourceManager resources,
            ResourceKey<? extends Registry<T>> key, TagLoader.ElementLookup<Holder<T>> lookup,
            Operation<Map<TagKey<T>, List<Holder<T>>>> original, ResourceManager originalResources, WritableRegistry<T> registry) {
        return FeltTagLifecycle.withRegistry(registry, () -> original.call(resources, key, lookup));
    }

    @Unique
    private static <T> Map<Identifier, List<Holder<T>>> felt$buildTags(TagLoader<Holder<T>> loader,
            Map<Identifier, List<TagLoader.EntryWithSource>> entries, Operation<Map<Identifier, List<Holder<T>>>> original, Registry<T> registry) {
        var manager = PaperTagListenerManager.INSTANCE;
        var config = manager.createEventConfig(registry, FeltTagLifecycle.cause());
        var before = manager.firePreFlattenEvent(entries, config);
        return manager.firePostFlattenEvent(original.call(loader, before), config);
    }
}
