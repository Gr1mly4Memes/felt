package gr1mly4memes.felt.mixin.core.resources;

import gr1mly4memes.felt.bridge.core.resources.RegistryLoadTaskBridge;
import gr1mly4memes.felt.mod.util.FeltRegistryCopyLookup;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryLoadTask;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RegistryDataLoader.class)
public abstract class RegistryDataLoaderCopyMixin {
    @Inject(method = "createContext", at = @At("RETURN"), cancellable = true)
    private static void felt$useValueLookups(List<HolderLookup.RegistryLookup<?>> context, List<RegistryLoadTask<?>> tasks,
            CallbackInfoReturnable<RegistryOps.RegistryInfoLookup> cir) {
        var registries = new LinkedHashMap<ResourceKey<? extends Registry<?>>, HolderLookup.RegistryLookup<?>>();
        context.forEach(registry -> registries.put(registry.key(), registry));
        for (var task : tasks) {
            var registry = ((RegistryLoadTaskBridge) task).bridge$registryForCopying();
            registries.put(registry.key(), registry);
        }
        var builderLookups = HolderLookup.Provider.create(registries.values().stream());
        cir.setReturnValue(new FeltRegistryCopyLookup(cir.getReturnValue(), builderLookups));
    }
}
