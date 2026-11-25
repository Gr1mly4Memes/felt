package gr1mly4memes.felt.mod.util;

import gr1mly4memes.felt.bridge.core.core.MappedRegistryBridge;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;

public record FeltRegistryCopyLookup(RegistryOps.RegistryInfoLookup delegate,
                                     HolderLookup.Provider builderLookups) implements RegistryOps.RegistryInfoLookup {
    @Override
    public <T> Optional<HolderGetter<T>> lookup(ResourceKey<? extends Registry<? extends T>> registryKey) {
        return this.delegate.lookup(registryKey);
    }

    @SuppressWarnings("unchecked")
    public static <T> Function<ResourceKey<T>, Optional<T>> forBuilders(RegistryOps.RegistryInfoLookup lookup,
            ResourceKey<? extends Registry<T>> registryKey) {
        if (lookup instanceof FeltRegistryCopyLookup context) {
            var builderLookup = context.builderLookups().lookupOrThrow(registryKey);
            if (builderLookup instanceof MappedRegistryBridge<?> mapped) {
                return ((MappedRegistryBridge<T>) mapped)::bridge$valueForCopying;
            }
            return key -> builderLookup.get(key).filter(Holder.Reference::isBound).map(Holder.Reference::value);
        }
        var holderGetter = lookup.lookup(registryKey).orElseThrow();
        return key -> holderGetter.get(key).filter(Holder.Reference::isBound).map(Holder.Reference::value);
    }
}
