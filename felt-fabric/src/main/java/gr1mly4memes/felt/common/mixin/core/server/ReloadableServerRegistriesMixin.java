package gr1mly4memes.felt.common.mixin.core.server;

import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import gr1mly4memes.felttools.mixin.Local;
import gr1mly4memes.felt.common.mixin.core.resources.RegistryOpsAccessor;
import io.papermc.paper.registry.PaperRegistryAccess;
import io.papermc.paper.registry.PaperRegistryListenerManager;
import io.papermc.paper.registry.data.util.Conversions;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.Validatable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@gr1mly4memes.felt.common.mod.mixins.annotation.OnlyInPlatform(gr1mly4memes.felttools.api.FeltPlatform.FABRIC)
@Mixin(ReloadableServerRegistries.class)
public class ReloadableServerRegistriesMixin {
    @Shadow @Final private static RegistrationInfo DEFAULT_REGISTRATION_INFO;

    @Decorate(method = "lambda$scheduleRegistryLoad$0", at = @At(value = "INVOKE",
        target = "Ljava/util/Map;forEach(Ljava/util/function/BiConsumer;)V"))
    private static <T extends Validatable> void arclight$entries(Map<Identifier, T> entries, BiConsumer<Identifier, T> action, LootDataType<T> type, ResourceManager resources, RegistryOps<?> ops,
            @Local(ordinal = 0) WritableRegistry<T> registry) throws Throwable {
        PaperRegistryAccess.instance().registerReloadableRegistry(registry);
        Conversions conversions = new Conversions(((RegistryOpsAccessor) ops).arclight$lookupProvider());
        DecorationOps.callsite().invoke(entries, (BiConsumer<Identifier, T>) (id, value) ->
            PaperRegistryListenerManager.INSTANCE.registerWithListeners(registry,
                ResourceKey.create(type.registryKey(), id), value, DEFAULT_REGISTRATION_INFO, conversions));
    }
}
