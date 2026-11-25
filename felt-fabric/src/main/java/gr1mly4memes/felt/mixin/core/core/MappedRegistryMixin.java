package gr1mly4memes.felt.mixin.core.core;

import gr1mly4memes.felt.bridge.core.core.MappedRegistryBridge;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MappedRegistry.class)
public abstract class MappedRegistryMixin<T> implements MappedRegistryBridge<T> {
    @Shadow private Map<T, Holder.Reference<T>> unregisteredIntrusiveHolders;
    @Shadow private boolean frozen;
    @Unique private final Map<ResourceKey<T>, T> temporaryUnfrozenMap = new java.util.concurrent.ConcurrentHashMap<>(); // Paper added

    @Override
    @Invoker("validateWrite")
    public abstract void bridge$validateWrite(ResourceKey<T> key);

    @Override
    public void bridge$clearIntrusiveHolder(T value) {
        if (this.unregisteredIntrusiveHolders != null) this.unregisteredIntrusiveHolders.remove(value);
    }

    @Override
    public Optional<T> bridge$valueForCopying(ResourceKey<T> key) {
        return this.frozen ? ((MappedRegistry<T>) (Object) this).getOptional(key)
            : Optional.ofNullable(this.temporaryUnfrozenMap.get(key));
    }

    @Inject(method = "register", at = @At("RETURN"))
    private void felt$copyValue(ResourceKey<T> key, T value, RegistrationInfo info, CallbackInfoReturnable<Holder.Reference<T>> cir) {
        this.temporaryUnfrozenMap.put(key, value);
    }

    @Inject(method = "freeze", at = @At("RETURN"))
    private void felt$clearCopyValues(CallbackInfoReturnable<?> cir) {
        this.temporaryUnfrozenMap.clear();
    }

    public void clearIntrusiveHolder(T value) {
        this.bridge$clearIntrusiveHolder(value);
    }

}
