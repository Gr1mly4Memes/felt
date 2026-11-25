package gr1mly4memes.felt.mixin.core.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gr1mly4memes.felt.mod.util.FeltTagLifecycle;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent.Cause;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerTagLifecycleMixin {
    @WrapOperation(method = "lambda$reloadResources$1", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/tags/TagLoader;loadTagsForExistingRegistries(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/core/RegistryAccess;)Ljava/util/List;"))
    private List<Registry.PendingTags<?>> felt$reloadExistingTags(ResourceManager resources, RegistryAccess registries,
            Operation<List<Registry.PendingTags<?>>> original) {
        return FeltTagLifecycle.withCause(Cause.RELOAD, () -> original.call(resources, registries));
    }

    @WrapOperation(method = "lambda$reloadResources$1", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/ReloadableServerResources;loadResources(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/core/LayeredRegistryAccess;Ljava/util/List;Lnet/minecraft/world/flag/FeatureFlagSet;Lnet/minecraft/commands/Commands$CommandSelection;Lnet/minecraft/server/permissions/PermissionSet;Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<ReloadableServerResources> felt$reloadDynamicTags(ResourceManager resources, LayeredRegistryAccess<RegistryLayer> registries,
            List<Registry.PendingTags<?>> pending, FeatureFlagSet features, Commands.CommandSelection commands, PermissionSet permissions,
            Executor background, Executor main, Operation<CompletableFuture<ReloadableServerResources>> original) {
        return original.call(resources, registries, pending, features, commands, permissions, FeltTagLifecycle.reloadExecutor(background), main);
    }
}
