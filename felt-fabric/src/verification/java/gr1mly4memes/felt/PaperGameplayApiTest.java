package gr1mly4memes.felt;

import ca.spottedleaf.moonrise.patches.chunk_system.level.entity.EntityLookup;
import com.mojang.authlib.GameProfile;
import gr1mly4memes.felt.bridge.core.world.entity.SpawningEntityBridge;
import gr1mly4memes.felt.bridge.core.world.entity.decoration.DecorationDeathSourceBridge;
import io.papermc.paper.event.entity.WaterBottleSplashEvent;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonLandingPhase;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.CraftRaid;
import org.bukkit.craftbukkit.boss.CraftDragonBattle;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

final class PaperGameplayApiTest {
    static void verifyDecorationDeathVibrations(ServerLevel world, BlockPos pos, Plugin plugin) {
        var attacker = PaperReconfigurationTest.createTestPlayer(world, "DecorationFixture");
        attacker.getAbilities().instabuild = true;
        var damage = world.damageSources().playerAttack(attacker);
        var frame = java.util.Objects.requireNonNull(EntityTypes.ITEM_FRAME.create(world, EntitySpawnReason.COMMAND));
        var stand = java.util.Objects.requireNonNull(EntityTypes.ARMOR_STAND.create(world, EntitySpawnReason.COMMAND));
        var cushion = java.util.Objects.requireNonNull(EntityTypes.CUSHION.create(world, EntitySpawnReason.COMMAND));
        var lightning = java.util.Objects.requireNonNull(EntityTypes.LIGHTNING_BOLT.create(world, EntitySpawnReason.COMMAND));
        frame.setPos(Vec3.atBottomCenterOf(pos));
        stand.setPos(Vec3.atBottomCenterOf(pos.east()));
        cushion.setPos(Vec3.atBottomCenterOf(pos.west()));

        var emitters = new java.util.ArrayList<UUID>();
        var gameEvents = GenericGameEvent.getHandlerList();
        var vibrations = new RegisteredListener(new Listener() {}, (listener, raw) -> {
            var event = (GenericGameEvent) raw;
            if (event.getEvent() == org.bukkit.GameEvent.ENTITY_DIE && event.getEntity() != null) {
                emitters.add(event.getEntity().getUniqueId());
            }
        }, EventPriority.NORMAL, plugin, false);
        var deaths = EntityDeathEvent.getHandlerList();
        var veto = new RegisteredListener(new Listener() {}, (listener, raw) -> {
            var event = (EntityDeathEvent) raw;
            if (event.getEntity().getUniqueId().equals(stand.getUUID())) {
                var causingEntity = event.getDamageSource().getCausingEntity();
                check(causingEntity != null && causingEntity.getUniqueId().equals(attacker.getUUID()),
                    "Armor stand death event lost its player damage source");
                event.setCancelled(true);
            }
        }, EventPriority.NORMAL, plugin, false);
        gameEvents.register(vibrations);
        try {
            deaths.register(veto);
            try {
                stand.hurtServer(world, damage, 5);
                check(!stand.isRemoved() && emitters.isEmpty() && SculkShriekerBlockEntity.tryGetPlayer(stand) == null,
                    "Canceled armor stand death still produced a shrieker attribution");
            } finally {
                deaths.unregister(veto);
            }
            frame.hurtServer(world, damage, 1);
            stand.hurtServer(world, damage, 5);
            cushion.thunderHit(world, lightning);
            check(emitters.equals(List.of(frame.getUUID(), stand.getUUID(), cushion.getUUID())),
                "Decoration death vibrations did not originate from the dying entities");
            check(SculkShriekerBlockEntity.tryGetPlayer(frame) == attacker && SculkShriekerBlockEntity.tryGetPlayer(stand) == attacker,
                "The shrieker could not resolve the player from a decoration's fatal damage");
            var lightningDamage = ((DecorationDeathSourceBridge) cushion).bridge$getDecorationDeathSource();
            check(lightningDamage != null && lightningDamage.is(net.minecraft.world.damagesource.DamageTypes.LIGHTNING_BOLT)
                && SculkShriekerBlockEntity.tryGetPlayer(cushion) == null,
                "Lightning damage did not produce an unattributed cushion death");
        } finally {
            gameEvents.unregister(vibrations);
        }
        System.out.println("Paper sculk attribution: decoration vibrations retained player damage, lightning and canceled death semantics");
    }

    static void verifyRespawnWorld(net.minecraft.server.MinecraftServer server) {
        var craft = org.bukkit.Bukkit.getServer();
        var original = craft.getRespawnWorld();
        var overworld = server.overworld();
        var nether = server.getLevel(Level.NETHER);
        var overworldBukkit = overworld.getWorld();
        var netherBukkit = nether.getWorld();
        var overworldSpawn = overworldBukkit.getSpawnLocation();
        var netherSpawn = netherBukkit.getSpawnLocation();
        var expectedPos = new BlockPos(12 * 16 + 8, 70, -12 * 16 + 8);
        try {
            check(netherBukkit.setSpawnLocation(new Location(netherBukkit, expectedPos.getX(), expectedPos.getY(), expectedPos.getZ(), 31, 8)),
                "Could not set a separate Nether spawn");
            check(craft.getRespawnWorld().equals(original), "Changing one world's local Bukkit spawn moved the server's respawn world");
            craft.setRespawnWorld(netherBukkit);
            check(server.findRespawnDimension() == nether && server.getRespawnData().pos().equals(expectedPos)
                && server.getRespawnData().dimension() == Level.NETHER && server.getRespawnData().yaw() == 31,
                "The selected respawn world did not supply the server's effective spawn");
            check(overworldBukkit.getSpawnLocation().equals(overworldSpawn), "Selecting the Nether overwrote the Overworld spawn");
            var root = (net.minecraft.world.level.storage.PrimaryLevelData) server.getWorldData().overworldData();
            var tag = root.createTag(null);
            var parsed = net.minecraft.world.level.storage.PrimaryLevelData.parse(new com.mojang.serialization.Dynamic<>(NbtOps.INSTANCE, tag),
                root.getLevelSettings(), net.minecraft.world.level.storage.PrimaryLevelData.SpecialWorldProperty.NONE, com.mojang.serialization.Lifecycle.stable());
            var bridge = (gr1mly4memes.felt.bridge.core.world.level.storage.PrimaryLevelDataBridge) parsed;
            check(bridge.bridge$getRespawnDimension() == Level.NETHER && bridge.bridge$getDimensionSpawn(Level.NETHER).pos().equals(expectedPos)
                && parsed.getRespawnData().equals(root.getRespawnData())
                && tag.read("spawn", net.minecraft.world.level.storage.LevelData.RespawnData.CODEC).orElseThrow().dimension() == Level.NETHER,
                "The server respawn world or its local coordinates were lost in level.dat");
            craft.setRespawnWorld(overworldBukkit);
            check(server.findRespawnDimension() == overworld && netherBukkit.getSpawnLocation().getBlockX() == expectedPos.getX(),
                "Switching back to the Overworld changed the Nether spawn");
        } finally {
            netherBukkit.setSpawnLocation(netherSpawn);
            craft.setRespawnWorld(original);
        }
        System.out.println("Paper world respawn API: independent world spawn, native selection and level.dat round trip passed");
    }

    static void verifyDragon(ServerLevel world, BlockPos podium) {
        var dragon = EntityTypes.ENDER_DRAGON.create(world, EntitySpawnReason.COMMAND);
        dragon.setPos(Vec3.atBottomCenterOf(podium.above(20)));
        var api = (org.bukkit.entity.EnderDragon) dragon.getBukkitEntity();
        var defaultPodium = api.getPodium();
        api.setPodium(new Location(null, podium.getX(), podium.getY(), podium.getZ()));
        var landing = new DragonLandingPhase(dragon);
        landing.doServerTick(world);
        var target = landing.getFlyTargetLocation();
        check(target.x == podium.getX() + 0.5 && target.z == podium.getZ() + 0.5,
            "Native landing phase did not target the custom dragon podium");
        api.setPodium(null);
        check(defaultPodium.equals(api.getPodium()), "Resetting the podium did not restore the native location");

        var fight = new EnderDragonFight(false, true, true, java.util.Optional.empty(), 0, java.util.Optional.empty(),
            java.util.Optional.empty(), java.util.List.of(), java.util.List.of());
        fight.init(world, 0L, BlockPos.ZERO);
        fight.skipArenaLoadedCheck();
        var battle = new CraftDragonBattle(fight);
        check(battle.initiateRespawn(java.util.List.of()) && battle.getRespawnPhase() == DragonBattle.RespawnPhase.START,
            "The dragon battle API did not start an empty-crystal respawn");
        check(!battle.initiateRespawn(java.util.List.of()), "An active dragon respawn was restarted");
        var profile = new GameProfile(UUID.randomUUID(), "DragonFixture");
        var player = new ServerPlayer(world.getServer(), world, profile, ClientInformation.createDefault());
        player.connection = new ServerGamePacketListenerImpl(world.getServer(), new Connection(PacketFlow.SERVERBOUND), player,
            CommonListenerCookie.createInitial(profile, false));
        player.setPos(0.5, 128, 0.5);
        world.players().add(player);
        try {
            fight.tick();
            check(battle.getRespawnPhase() == DragonBattle.RespawnPhase.PREPARING_TO_SUMMON_PILLARS,
                "The native dragon fight aborted instead of progressing the crystal-free respawn");
        } finally {
            world.players().remove(player);
        }
        System.out.println("Paper dragon API: custom landing podium, reset and crystal-free respawn progression passed");
    }

    static Attribution verify(ServerLevel world, BlockPos marker, LivingEntity source, Plugin plugin) {
        verifyGameplayLimits(world);
        double y = world.getMaxY() - 8;
        verifyPotion(world, marker, y, plugin);
        verifyTradeCount(world, marker, y, plugin);
        verifyRaid(world, marker);
        verifyExplosionSource(world, marker, y, plugin);
        return verifyAttribution(world, marker, y, source, verifyExperience(world, marker, y, source, plugin));
    }

    private static void verifyGameplayLimits(ServerLevel world) {
        var misc = io.papermc.paper.configuration.GlobalConfiguration.get().misc;
        var previousEntries = misc.maxTrackingCombatEntries;
        var previousDemand = misc.preventNegativeVillagerDemand;
        try {
            misc.maxTrackingCombatEntries = new io.papermc.paper.configuration.type.number.IntOr.Disabled(java.util.OptionalInt.of(2));
            var pig = EntityTypes.PIG.create(world, EntitySpawnReason.COMMAND);
            var tracker = pig.getCombatTracker();
            for (int damage = 1; damage <= 6; damage++) {
                tracker.recordDamage(pig.damageSources().generic(), damage);
            }
            var bridge = (gr1mly4memes.felt.bridge.core.world.damagesource.CombatTrackerBridge) tracker;
            var entries = bridge.bridge$getEntries();
            check(entries.size() == 3 && entries.getFirst().damage() == 4.0F && entries.getLast().damage() == 6.0F,
                "Native combat history did not discard its oldest entries before appending");
            bridge.bridge$recordDamageAndCheckCombatState(new net.minecraft.world.damagesource.CombatEntry(pig.damageSources().generic(), 7, null, 0));
            check(entries.size() == 3 && entries.getFirst().damage() == 5.0F && entries.getLast().damage() == 7.0F,
                "The Paper combat API bypassed the same history limit");
            var offer = new net.minecraft.world.item.trading.MerchantOffer(new net.minecraft.world.item.trading.ItemCost(Items.EMERALD),
                new ItemStack(Items.BREAD), 10, 1, 0.1F);
            misc.preventNegativeVillagerDemand = false;
            offer.updateDemand();
            check(offer.getDemand() < 0, "The disabled trade demand fix changed vanilla demand");
            misc.preventNegativeVillagerDemand = true;
            offer.updateDemand();
            check(offer.getDemand() == 0, "The enabled trade demand fix did not clamp negative demand");
        } finally {
            misc.maxTrackingCombatEntries = previousEntries;
            misc.preventNegativeVillagerDemand = previousDemand;
        }
        System.out.println("Paper gameplay limits: combat history eviction and nonnegative villager demand passed");
    }

    private static ExperienceFixture verifyExperience(ServerLevel world, BlockPos marker, double y, LivingEntity trigger, Plugin plugin) {
        var bottle = EntityTypes.EXPERIENCE_BOTTLE.create(world, EntitySpawnReason.COMMAND);
        var position = new Vec3(marker.getX() - 4.5, y, marker.getZ() + 4.5);
        bottle.setPos(position);
        bottle.setOwner(trigger);
        check(world.addFreshEntity(bottle), "Could not add experience bottle fixture");
        var handler = new RegisteredListener(new Listener() {}, (listener, rawEvent) -> {
            var event = (org.bukkit.event.entity.ExpBottleEvent) rawEvent;
            if (event.getEntity().getUniqueId().equals(bottle.getUUID())) {
                event.setExperience(8);
                event.setShowEffect(false);
            }
        }, EventPriority.NORMAL, plugin, false);
        var handlers = org.bukkit.event.entity.ExpBottleEvent.getHandlerList();
        handlers.register(handler);
        try {
            var hit = bottle.getClass().getDeclaredMethod("onHit", HitResult.class);
            hit.setAccessible(true);
            hit.invoke(bottle, BlockHitResult.miss(position, net.minecraft.core.Direction.UP, BlockPos.containing(position)));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not trigger native experience bottle hit", exception);
        } finally {
            handlers.unregister(handler);
        }
        var orbs = world.getEntitiesOfClass(ExperienceOrb.class, AABB.ofSize(position, 1, 1, 1));
        check(bottle.isRemoved() && orbs.size() == 2 && orbs.stream().mapToInt(ExperienceOrb::getValue).sum() == 8,
            "Experience attribution changed the event's native XP denominations");
        var fixture = new ExperienceFixture(bottle.getUUID(), orbs.stream().map(ExperienceOrb::getUUID).toList());
        for (var orb : orbs) {
            fixture.checkOrb(orb, trigger.getUUID());
        }
        var unattributed = EntityTypes.EXPERIENCE_ORB.create(world, EntitySpawnReason.COMMAND);
        var api = (org.bukkit.entity.ExperienceOrb) unattributed.getBukkitEntity();
        check(api.getSpawnReason() == org.bukkit.entity.ExperienceOrb.SpawnReason.UNKNOWN
            && api.getTriggerEntityId() == null && api.getSourceEntityId() == null,
            "An unattributed experience orb did not retain the legacy defaults");
        return fixture;
    }

    private static void verifyExplosionSource(ServerLevel world, BlockPos marker, double y, Plugin plugin) {
        var pig = EntityTypes.PIG.create(world, EntitySpawnReason.COMMAND);
        pig.setPos(marker.getX() + 0.5, y, marker.getZ() + 0.5);
        pig.setNoGravity(true);
        check(world.addFreshEntity(pig), "Could not add explosion source");
        var source = pig.getBukkitEntity();
        var location = source.getLocation();
        float health = pig.getHealth();
        var damage = new AtomicInteger();
        var handler = new RegisteredListener(new Listener() {}, (listener, rawEvent) -> {
            if (rawEvent instanceof org.bukkit.event.entity.EntityDamageByEntityEvent event
                && event.getEntity().equals(source) && event.getDamager().equals(source)) {
                damage.incrementAndGet();
            }
        }, EventPriority.NORMAL, plugin, false);
        var handlers = org.bukkit.event.entity.EntityDamageEvent.getHandlerList();
        handlers.register(handler);
        try {
            check(location.getWorld().createExplosion(source, location, 2.0F, false, false, true) && pig.getHealth() == health,
                "The normal explosion path damaged its excluded source");
            check(location.getWorld().createExplosion(source, location, 2.0F, false, false, false)
                && pig.getHealth() < health && damage.get() == 1,
                "Explosion source inclusion lost damage or source attribution");
        } finally {
            handlers.unregister(handler);
            pig.discard();
        }
        System.out.println("Paper explosion API: source inclusion, exclusion and damage attribution passed");
    }

    private static void verifyPotion(ServerLevel world, BlockPos marker, double y, Plugin plugin) {
        var pig = EntityTypes.PIG.create(world, EntitySpawnReason.COMMAND);
        pig.setPos(marker.getX() + 0.5, y, marker.getZ() + 0.5);
        pig.setNoGravity(true);
        check(world.addFreshEntity(pig), "Could not add potion target");
        var events = new AtomicInteger();
        var waterEvents = new AtomicInteger();
        var handler = new RegisteredListener(new Listener() {}, (listener, rawEvent) -> {
            var event = (PotionSplashEvent) rawEvent;
            check(event.getHitBlock() == null && event.getHitEntity() == null, "API splash reported an impact");
            // The pinned Paper splash pipeline dispatches water and potion phases separately.
            if (event instanceof WaterBottleSplashEvent) {
                waterEvents.incrementAndGet();
                return;
            }
            if (events.incrementAndGet() == 1) {
                event.setCancelled(true);
            }
        }, EventPriority.NORMAL, plugin, false);
        PotionSplashEvent.getHandlerList().register(handler);
        try {
            for (int attempt = 1; attempt <= 2; attempt++) {
                var potion = EntityTypes.SPLASH_POTION.create(world, EntitySpawnReason.COMMAND);
                potion.setPos(pig.position());
                potion.setItem(PotionContents.createItemStack(Items.SPLASH_POTION, Potions.FIRE_RESISTANCE));
                check(world.addFreshEntity(potion), "Could not add test splash potion");
                ((ThrownPotion) potion.getBukkitEntity()).splash();
                check(potion.isRemoved() && waterEvents.get() == attempt && events.get() == attempt,
                    "API splash did not fire one event per phase and remove the potion");
                check(pig.hasEffect(MobEffects.FIRE_RESISTANCE) == (attempt == 2), "API splash ignored event acceptance");
            }
        } finally {
            PotionSplashEvent.getHandlerList().unregister(handler);
            pig.discard();
        }
        System.out.println("Paper potion API: collision-free splash, effects and event cancellation passed");
    }

    private static void verifyTradeCount(ServerLevel world, BlockPos marker, double y, Plugin plugin) {
        var villager = EntityTypes.VILLAGER.create(world, EntitySpawnReason.COMMAND);
        villager.setPos(marker.getX() + 0.5, y, marker.getZ() + 0.5);
        villager.setNoAi(true);
        check(world.addFreshEntity(villager), "Could not add trade fixture");
        var api = (Villager) villager.getBukkitEntity();
        try {
            api.setProfession(Villager.Profession.NONE);
            check(!api.addTrades(1), "A villager with no trade set accepted the request");
            api.setProfession(Villager.Profession.FARMER);
            var offers = villager.getOffers();
            int initial = offers.size();
            check(api.addTrades(1) && offers.size() == initial + 1, "The villager API added the wrong number of trades");

            var events = new AtomicInteger();
            var handler = new RegisteredListener(new Listener() {}, (listener, rawEvent) -> {
                var event = (VillagerAcquireTradeEvent) rawEvent;
                if (event.getEntity().getUniqueId().equals(villager.getUUID())) {
                    events.incrementAndGet();
                    event.setRecipe(new org.bukkit.inventory.MerchantRecipe(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BREAD), 1));
                }
            }, EventPriority.NORMAL, plugin, false);
            VillagerAcquireTradeEvent.getHandlerList().register(handler);
            try {
                check(api.addTrades(1) && events.get() == 1 && offers.size() == initial + 1,
                    "Trade acquisition did not safely reject a replacement without ingredients");
            } finally {
                VillagerAcquireTradeEvent.getHandlerList().unregister(handler);
            }
            check(api.addTrades(1) && offers.size() == initial + 2, "An invalid event replacement leaked into the next trade batch");
        } finally {
            villager.discard();
        }
        System.out.println("Paper villager API: requested trade count, absent trade sets and invalid event replacements passed");
    }

    private static void verifyRaid(ServerLevel world, BlockPos marker) {
        var player = new ServerPlayer(world.getServer(), world, new GameProfile(UUID.randomUUID(), "RaidFixture"), ClientInformation.createDefault());
        var raids = new Raids();
        var raid = raids.createOrExtendRaid(player, marker);
        check(raid != null, "Failed to register test raid");
        var craft = new CraftRaid(raid, world);
        int id = craft.getId();
        check(id >= 0 && raids.get(id) == raid, "Raid API did not expose the native registration ID");
        var key = new NamespacedKey("felt", "raid_fixture");
        craft.getPersistentDataContainer().set(key, PersistentDataType.STRING, "saved");
        var encoded = Raids.CODEC.encodeStart(NbtOps.INSTANCE, raids).getOrThrow();
        var reloaded = Raids.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        var restored = new CraftRaid(reloaded.get(id), world);
        check(restored.getId() == id && "saved".equals(restored.getPersistentDataContainer().get(key, PersistentDataType.STRING)),
            "Raid ID or plugin data was lost during the native saved-data round trip");
        System.out.println("Paper raid API: stable registration ID and persistent plugin data passed");
    }

    private static Attribution verifyAttribution(ServerLevel world, BlockPos marker, double y, LivingEntity source, ExperienceFixture experience) {
        FireworkRocketEntity firework;
        try {
            var create = CrossbowItem.class.getDeclaredMethod("createProjectile", Level.class, LivingEntity.class, ItemStack.class, ItemStack.class, boolean.class);
            create.setAccessible(true);
            firework = (FireworkRocketEntity) create.invoke(Items.CROSSBOW, world, source, new ItemStack(Items.CROSSBOW), new ItemStack(Items.FIREWORK_ROCKET), false);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not exercise native crossbow projectile creation", exception);
        }
        firework.setPos(marker.getX() + 0.5, y, marker.getZ() + 0.5);
        check(world.addFreshEntity(firework), "Could not add crossbow firework");
        check(source.getUUID().equals(((Firework) firework.getBukkitEntity()).getSpawningEntity()), "Crossbow rocket lost its shooter UUID");
        var phantom = EntityTypes.PHANTOM.create(world, EntitySpawnReason.COMMAND);
        check(((Phantom) phantom.getBukkitEntity()).getSpawningEntity() == null, "A command-spawned phantom received an attribution");
        ((SpawningEntityBridge) phantom).bridge$setSpawningEntity(source.getUUID());
        phantom.setPos(firework.position());
        phantom.setNoAi(true);
        check(world.addFreshEntity(phantom), "Could not add phantom with attribution data");
        return new Attribution(firework.getUUID(), phantom.getUUID(), experience);
    }

    record Attribution(UUID firework, UUID phantom, ExperienceFixture experience) {
        void verifyReload(EntityLookup lookup, UUID source) {
            var rocketApi = (Firework) lookup.get(this.firework).getBukkitEntity();
            var phantomApi = (Phantom) lookup.get(this.phantom).getBukkitEntity();
            check(source.equals(rocketApi.getSpawningEntity()) && source.equals(phantomApi.getSpawningEntity()),
                "Firework or phantom attribution was lost across native chunk reload");
            for (UUID id : this.experience.orbs) {
                this.experience.checkOrb((ExperienceOrb) lookup.get(id), source);
            }
            System.out.println("Paper spawn attribution: crossbow firework and phantom UUID persistence passed");
            System.out.println("Paper experience orb API: bottle reason, trigger and source UUIDs survived native chunk reload");
        }
    }

    private record ExperienceFixture(UUID bottle, List<UUID> orbs) {
        void checkOrb(ExperienceOrb orb, UUID trigger) {
            var api = (org.bukkit.entity.ExperienceOrb) orb.getBukkitEntity();
            check(api.getSpawnReason() == org.bukkit.entity.ExperienceOrb.SpawnReason.EXP_BOTTLE
                && trigger.equals(api.getTriggerEntityId()) && this.bottle.equals(api.getSourceEntityId()),
                "Experience orb lost its native bottle, trigger, or spawn reason");
        }
    }

    private static void check(boolean result, String message) {
        if (!result) {
            throw new AssertionError(message);
        }
    }
}
