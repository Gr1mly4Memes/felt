package gr1mly4memes.felt;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventRunner;
import io.papermc.paper.plugin.lifecycle.event.PaperLifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent.Cause;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.PaperTimelineRegistryEntry;
import io.papermc.paper.registry.data.util.Conversions;
import io.papermc.paper.registry.tag.TagKey;
import io.papermc.paper.tag.TagEntry;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.key.Key;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.Items;

final class PaperRegistryLifecycleTest {
    private final BootstrapContext owner;
    private final LifecycleEventManager<BootstrapContext> lifecycleManager;
    private final EnumSet<Cause> itemPre = EnumSet.noneOf(Cause.class);
    private final EnumSet<Cause> itemPost = EnumSet.noneOf(Cause.class);
    private final EnumSet<Cause> damagePre = EnumSet.noneOf(Cause.class);
    private final EnumSet<Cause> damagePost = EnumSet.noneOf(Cause.class);

    private PaperRegistryLifecycleTest(BootstrapContext owner) {
        this.owner = owner;
        this.lifecycleManager = new PaperLifecycleEventManager<>(owner, () -> true);
    }

    static PaperRegistryLifecycleTest register() {
        var loader = PaperRegistryLifecycleTest.class.getClassLoader();
        var meta = (PluginMeta) Proxy.newProxyInstance(loader, new Class<?>[] {PluginMeta.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getName", "getDisplayName" -> "FeltRegistryFixture";
                default -> null;
            });
        var owner = (BootstrapContext) Proxy.newProxyInstance(loader, new Class<?>[] {BootstrapContext.class},
            (proxy, method, args) -> method.getName().equals("getPluginMeta") ? meta : null);
        var test = new PaperRegistryLifecycleTest(owner);
        test.register(RegistryKey.ITEM, "stone", "dirt", test.itemPre, test.itemPost);
        test.register(RegistryKey.DAMAGE_TYPE, "generic", "fall", test.damagePre, test.damagePost);
        return test;
    }

    private <T> void register(RegistryKey<T> registry, String firstValue, String addedValue,
                              EnumSet<Cause> before, EnumSet<Cause> after) {
        var base = TagKey.create(registry, "felt:base_fixture");
        var nested = TagKey.create(registry, "felt:nested_fixture");
        var first = TypedKey.create(registry, Key.key("minecraft", firstValue));
        var added = TypedKey.create(registry, Key.key("minecraft", addedValue));
        this.lifecycleManager.registerEventHandler(LifecycleEvents.TAGS.preFlatten(registry), event -> {
            before.add(event.cause());
            event.registrar().setTag(base, List.of(TagEntry.valueEntry(first)));
            event.registrar().setTag(nested, List.of(TagEntry.tagEntry(base)));
        });
        this.lifecycleManager.registerEventHandler(LifecycleEvents.TAGS.postFlatten(registry), event -> {
            check(event.registrar().getTag(nested).contains(first), "Pre-flatten nested tag was not resolved");
            after.add(event.cause());
            event.registrar().addToTag(nested, List.of(added));
        });
    }

    void verify(MinecraftServer server) {
        try {
            check(this.itemPre.contains(Cause.INITIAL) && this.itemPost.contains(Cause.INITIAL)
                && this.damagePre.contains(Cause.INITIAL) && this.damagePost.contains(Cause.INITIAL),
                "Initial registry loading missed a Paper tag lifecycle phase");
            this.verifyBindings(server);
            var reload = server.reloadResources(server.getPackRepository().getSelectedIds());
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(45);
            server.managedBlock(() -> {
                check(System.nanoTime() < deadline, "Datapack reload did not finish");
                return reload.isDone();
            });
            reload.join();
            check(this.itemPre.contains(Cause.RELOAD) && this.itemPost.contains(Cause.RELOAD)
                && this.damagePre.contains(Cause.RELOAD) && this.damagePost.contains(Cause.RELOAD),
                "The native datapack reload did not pass Paper's reload cause to the tag handlers");
            this.verifyBindings(server);
            System.out.println("Paper tag lifecycle: built-in and datapack tags, nested flattening, post-flatten edits and reload passed");
            verifyTimeline(server);
            verifyUnboundBuilderCopy();
        } finally {
            LifecycleEventRunner.INSTANCE.unregisterEventHandlersForOwner(this.owner);
        }
    }

    private void verifyBindings(MinecraftServer server) {
        var name = Identifier.fromNamespaceAndPath("felt", "nested_fixture");
        var items = server.registryAccess().lookupOrThrow(Registries.ITEM);
        var itemTag = items.getOrThrow(net.minecraft.tags.TagKey.create(Registries.ITEM, name));
        check(itemTag.contains(Items.STONE.builtInRegistryHolder()) && itemTag.contains(Items.DIRT.builtInRegistryHolder()),
            "Both registered item tag values must reach the bound registry");
        var damage = server.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
        var damageTag = damage.getOrThrow(net.minecraft.tags.TagKey.create(Registries.DAMAGE_TYPE, name));
        check(damageTag.contains(damage.getOrThrow(DamageTypes.GENERIC)) && damageTag.contains(damage.getOrThrow(DamageTypes.FALL)),
            "Both registered damage tag values must reach the bound registry");
    }

    private static void verifyTimeline(MinecraftServer server) {
        var original = server.registryAccess().lookupOrThrow(Registries.TIMELINE).listElements()
            .map(net.minecraft.core.Holder.Reference::value)
            .filter(timeline -> !timeline.attributes().isEmpty())
            .findFirst().orElseThrow();
        var builder = new PaperTimelineRegistryEntry.PaperBuilder(Conversions.global(), original);
        var copy = builder.build();
        check(original.equals(copy) && original.hashCode() == copy.hashCode() && original.attributes().equals(copy.attributes()),
            "A copied Paper timeline did not retain its tracks or value equality");
        builder.addTimeMarker(Key.key("felt", "fixture"), 1, false);
        check(!original.equals(builder.build()), "Timeline copying ignored a plugin's marker edit");
        System.out.println("Paper timeline builder: native tracks, equality and marker edits passed");
    }

    private static void verifyUnboundBuilderCopy() {
        var sounds = new net.minecraft.core.MappedRegistry<net.minecraft.sounds.SoundEvent>(Registries.SOUND_EVENT,
            com.mojang.serialization.Lifecycle.stable());
        var soundId = Identifier.fromNamespaceAndPath("felt", "unbound_sound");
        var key = net.minecraft.resources.ResourceKey.create(Registries.SOUND_EVENT, soundId);
        var sound = net.minecraft.sounds.SoundEvent.createFixedRangeEvent(soundId, 8);
        sounds.register(key, sound, net.minecraft.core.RegistrationInfo.BUILT_IN);
        var holder = net.minecraft.core.Holder.Reference.createStandAlone(sounds, key);
        var lookups = net.minecraft.core.HolderLookup.Provider.create(java.util.stream.Stream.of(sounds));
        var original = new net.minecraft.resources.RegistryOps.RegistryInfoLookup() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> java.util.Optional<net.minecraft.core.HolderGetter<T>> lookup(
                    net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<? extends T>> registry) {
                if (!registry.equals(Registries.SOUND_EVENT)) {
                    return java.util.Optional.empty();
                }
                var getter = new net.minecraft.core.HolderGetter<net.minecraft.sounds.SoundEvent>() {
                    @Override
                    public java.util.Optional<net.minecraft.core.Holder.Reference<net.minecraft.sounds.SoundEvent>> get(
                            net.minecraft.resources.ResourceKey<net.minecraft.sounds.SoundEvent> requested) {
                        return requested.equals(key) ? java.util.Optional.of(holder) : java.util.Optional.empty();
                    }
                    @Override
                    public java.util.Optional<net.minecraft.core.HolderSet.Named<net.minecraft.sounds.SoundEvent>> get(
                            net.minecraft.tags.TagKey<net.minecraft.sounds.SoundEvent> requested) {
                        return java.util.Optional.empty();
                    }
                };
                return java.util.Optional.of((net.minecraft.core.HolderGetter<T>) getter);
            }
        };
        var conversions = new Conversions(new gr1mly4memes.felt.mod.util.FeltRegistryCopyLookup(original, lookups));
        var apiKey = TypedKey.create(RegistryKey.SOUND_EVENT, Key.key("felt", "unbound_sound"));
        var copy = conversions.<net.minecraft.sounds.SoundEvent, org.bukkit.Sound,
            io.papermc.paper.registry.data.PaperSoundEventRegistryEntry.PaperBuilder>createHolderFromBuilder(RegistryKey.SOUND_EVENT,
            factory -> factory.copyFrom(apiKey)).value();
        check(copy.equals(sound) && !holder.isBound(), "A builder could not copy the registered value before its holder was bound");
        sounds.freeze();
        check(sounds.getOrThrow(key).isBound() && gr1mly4memes.felt.mod.util.FeltRegistryCopyLookup.forBuilders(conversions.lookup(), Registries.SOUND_EVENT)
            .apply(key).orElseThrow().equals(sound), "Freezing discarded the registry's copy lookup");
        System.out.println("Paper registry builders: value copies before and after holder binding passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
