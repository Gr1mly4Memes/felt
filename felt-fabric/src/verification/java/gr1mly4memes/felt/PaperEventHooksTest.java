package gr1mly4memes.felt;

import gr1mly4memes.felt.bridge.core.world.entity.LivingEntityBridge;
import io.papermc.paper.event.block.PaperBlockDispenseEvent;
import io.papermc.paper.event.entity.EntityBreakByEntityEvent;
import io.papermc.paper.event.entity.EntityBreakEvent;
import io.papermc.paper.event.entity.EntityCollideWithEntityEvent;
import io.papermc.paper.event.entity.EntityConstructEvent;
import io.papermc.paper.event.entity.EntityIgniteEvent;
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import io.papermc.paper.event.entity.EntityPotentSulfurLaunchEvent;
import io.papermc.paper.event.entity.ItemTransportingEntitySortEvent;
import io.papermc.paper.event.entity.SulfurCubeSwallowItemEvent;
import io.papermc.paper.event.server.AsyncServerDataFixerRemoveBlockEntityEvent;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.PortalProcessor;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.entity.PotentSulfurBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import org.bukkit.Material;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockShearEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPortalExitEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

final class PaperEventHooksTest {
    static void verify(ServerLevel world, BlockPos pos, Plugin plugin) throws Exception {
        verifyInput(world, plugin);
        verifyConstruction(world, pos, plugin);
        verifyBreakAndCollision(world, pos, plugin);
        verifyBoatUnleash(world, pos, plugin);
        verifyPortal(world, pos, plugin);
        verifySulfur(world, pos, plugin);
        verifyDispenserShearing(world, pos, plugin);
        verifySulfurDispensing(world, pos, plugin);
        verifyItemSorting(world, pos, plugin);
        verifyWindChargePrime(world, pos, plugin);
        verifyMinecartIgnition(world, pos, plugin);
        verifyMobBow(world, pos.above(3), plugin, EntityTypes.ILLUSIONER);
        verifyMobBow(world, pos.above(3), plugin, EntityTypes.SKELETON);
        System.out.println("Paper mob bow API: skeleton and Illusioner hand, ammunition, cancellation and replacement passed");
        verifyDataFixer(world, pos, plugin);
        System.out.println("Paper event API: input, construction, break, collision, portal, sulfur, unleash and block-entity data fixing passed");
    }

    private static <T extends Monster & RangedAttackMob> void verifyMobBow(ServerLevel world, BlockPos pos,
                                                                          Plugin plugin, EntityType<T> type) throws Exception {
        var shooter = Objects.requireNonNull(type.create(world, EntitySpawnReason.COMMAND));
        shooter.setPos(Vec3.atBottomCenterOf(pos));
        shooter.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.BOW));
        var target = Objects.requireNonNull(EntityTypes.PIG.create(world, EntitySpawnReason.COMMAND));
        target.setPos(Vec3.atBottomCenterOf(pos.east(4)));
        var alternate = Objects.requireNonNull(EntityTypes.SNOWBALL.create(world, EntitySpawnReason.COMMAND));
        alternate.setPos(shooter.position());
        var area = new AABB(pos).inflate(2);
        var calls = new AtomicInteger();
        try (var listener = listen(EntityShootBowEvent.getHandlerList(), EntityShootBowEvent.class, plugin, event -> {
            check(event.getEntity().getUniqueId().equals(shooter.getUUID()) && event.getBow() != null
                && event.getBow().getType() == Material.BOW && event.getConsumable() != null
                && event.getConsumable().getType() == Material.ARROW && event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND
                && event.getForce() == 0.65F && event.getProjectile().getVelocity().getX() > 0.0,
                type + " shot event lost its shooter, hand, ammunition, power, or initialized velocity");
            int shot = calls.getAndIncrement();
            if (shot == 0) {
                event.setCancelled(true);
            } else if (shot == 1) {
                check(world.addFreshEntity(alternate), "Could not add the plugin's replacement projectile");
                event.setProjectile(alternate.getBukkitEntity());
            }
        })) {
            shooter.performRangedAttack(target, 0.65F);
            check(calls.get() == 1 && world.getEntitiesOfClass(AbstractArrow.class, area).isEmpty(), "Canceled " + type + " shot spawned an arrow");
            shooter.performRangedAttack(target, 0.65F);
            check(calls.get() == 2 && alternate.getBukkitEntity().isValid() && world.getEntitiesOfClass(AbstractArrow.class, area).isEmpty(),
                type + " spawned its native arrow alongside the plugin replacement");
            shooter.performRangedAttack(target, 0.65F);
            var arrows = world.getEntitiesOfClass(AbstractArrow.class, area);
            check(calls.get() == 3 && arrows.size() == 1, "Accepted " + type + " shot did not spawn one arrow");
            arrows.getFirst().discard();
        } finally {
            alternate.discard();
        }
    }

    private static void verifyInput(ServerLevel world, Plugin plugin) throws Exception {
        var player = PaperReconfigurationTest.createTestPlayer(world, "InputFixture");
        for (int i = 0; i <= net.minecraft.server.network.ServerGamePacketListenerImpl.CLIENT_LOADED_TIMEOUT_TIME; ++i) {
            player.connection.tickClientLoadTimeout();
        }
        check(player.connection.hasClientLoaded(), "Input fixture did not complete client load");
        var seen = new AtomicInteger();
        var toggles = new AtomicInteger();
        try (var inputListener = listen(PlayerInputEvent.getHandlerList(), PlayerInputEvent.class, plugin, event -> {
            check(event.getPlayer().getUniqueId().equals(player.getUUID()) && event.getInput().isForward() && event.getInput().isSneak(),
                "Input event lost the new native input");
            check(seen.getAndIncrement() == 0, "Identical packets emitted duplicate input events");
        }); var sneakListener = listen(PlayerToggleSneakEvent.getHandlerList(), PlayerToggleSneakEvent.class, plugin, event -> {
            check(seen.get() == 1 && event.isSneaking(), "Sneak event ran before the input event or lost the new state");
            toggles.incrementAndGet();
            event.setCancelled(true);
        })) {
            var input = new Input(true, false, false, false, false, true, false);
            var packet = new ServerboundPlayerInputPacket(input);
            player.connection.handlePlayerInput(packet);
            player.connection.handlePlayerInput(packet);
            check(seen.get() == 1 && toggles.get() == 1 && !player.isShiftKeyDown() && input.equals(player.getLastClientInput()),
                "Canceled sneak state or input history diverged from Paper");
        }
    }

    private static void verifyConstruction(ServerLevel world, BlockPos pos, Plugin plugin) throws Exception {
        var built = new AtomicReference<org.bukkit.entity.Entity>();
        var calls = new AtomicInteger();
        world.setBlockAndUpdate(pos.below(2), Blocks.SNOW_BLOCK.defaultBlockState());
        world.setBlockAndUpdate(pos.below(), Blocks.SNOW_BLOCK.defaultBlockState());
        try (var listener = listen(EntityConstructEvent.getHandlerList(), EntityConstructEvent.class, plugin, event -> {
            check(event.getEntity() instanceof org.bukkit.entity.Snowman && event.getBlocks().size() == 3,
                "Construction event did not contain the golem and its three source blocks");
            built.set(event.getEntity());
            event.setCancelled(calls.getAndIncrement() == 0);
        })) {
            world.setBlockAndUpdate(pos, Blocks.CARVED_PUMPKIN.defaultBlockState());
            check(calls.get() == 1 && !built.get().isValid() && world.getBlockState(pos.below()).is(Blocks.SNOW_BLOCK),
                "Canceling construction consumed blocks or inserted the golem");
            world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            world.setBlockAndUpdate(pos, Blocks.CARVED_PUMPKIN.defaultBlockState());
            check(calls.get() == 2 && built.get().isValid() && world.getBlockState(pos.below()).isAir()
                && built.get().getEntitySpawnReason() == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.BUILD_SNOWMAN,
                "Successful construction lost the golem, original spawn reason or block consumption");
            built.get().remove();
        }
    }

    private static void verifyBreakAndCollision(ServerLevel world, BlockPos pos, Plugin plugin) throws Exception {
        var player = PaperReconfigurationTest.createTestPlayer(world, "BreakFixture");
        var frame = Objects.requireNonNull(EntityTypes.ITEM_FRAME.create(world, EntitySpawnReason.COMMAND));
        frame.setPos(Vec3.atBottomCenterOf(pos));
        var breakCount = new AtomicInteger();
        try (var legacy = listen(HangingBreakEvent.getHandlerList(), HangingBreakEvent.class, plugin, event -> event.setCancelled(true));
             var paper = listen(EntityBreakEvent.getHandlerList(), EntityBreakEvent.class, plugin, event -> {
                 check(event instanceof EntityBreakByEntityEvent damage && damage.getRemover().getUniqueId().equals(player.getUUID())
                     && event.isCancelled(), "Paper's break event lost its remover or inherited cancellation");
                 event.setCancelled(breakCount.getAndIncrement() == 0);
             })) {
            var damage = world.damageSources().playerAttack(player);
            frame.hurtServer(world, damage, 1);
            check(!frame.isRemoved(), "Canceled break still removed the item frame");
            frame.hurtServer(world, damage, 1);
            check(frame.isRemoved() && breakCount.get() == 2, "Paper's break event could not override legacy cancellation");
        }

        var pig = Objects.requireNonNull(EntityTypes.PIG.create(world, EntitySpawnReason.COMMAND));
        var cart = Objects.requireNonNull(EntityTypes.MINECART.create(world, EntitySpawnReason.COMMAND));
        pig.setPos(Vec3.atBottomCenterOf(pos));
        cart.setPos(pig.position().add(0.7, 0, 0));
        var collisions = new AtomicInteger();
        try (var listener = listen(EntityCollideWithEntityEvent.getHandlerList(), EntityCollideWithEntityEvent.class, plugin, event -> {
            check(event.getEntities().size() == 2, "Collision payload lost one of the entities");
            collisions.incrementAndGet();
            event.setCancelled(true);
        })) {
            pig.push(cart);
            cart.push(pig);
            check(collisions.get() == 2 && pig.getDeltaMovement().equals(Vec3.ZERO) && cart.getDeltaMovement().equals(Vec3.ZERO),
                "Collision cancellation did not prevent both native pushes");
        }
    }

    private static void verifyPortal(ServerLevel world, BlockPos pos, Plugin plugin) throws Exception {
        var pig = Objects.requireNonNull(EntityTypes.PIG.create(world, EntitySpawnReason.COMMAND));
        pig.setPos(Vec3.atBottomCenterOf(pos));
        check(world.addFreshEntity(pig), "Could not spawn the portal fixture");
        var gateway = Blocks.END_GATEWAY.defaultBlockState();
        world.setBlockAndUpdate(pos, gateway);
        var enters = new AtomicInteger();
        try (var listener = listen(EntityPortalEnterEvent.getHandlerList(), EntityPortalEnterEvent.class, plugin, event -> {
            check(event.getPortalType() == org.bukkit.PortalType.END_GATEWAY, "Gateway entry did not report its portal type");
            event.setCancelled(enters.getAndIncrement() == 0);
        })) {
            gateway.entityInside(world, pos, pig, InsideBlockEffectApplier.NOOP, true);
            check(enters.get() == 1 && pig.portalProcess == null, "Canceled gateway entry still submitted a teleport");
            gateway.entityInside(world, pos, pig, InsideBlockEffectApplier.NOOP, true);
            check(enters.get() == 2 && pig.portalProcess != null, "Gateway entry did not respect an allowed portal event");
        } finally {
            world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        pig.portalProcess = new PortalProcessor((Portal) Blocks.NETHER_PORTAL, pos);
        var calls = new AtomicInteger();
        var expected = new Vec3(0.2, 0.4, 0.6);
        try (var listener = listen(EntityPortalExitEvent.getHandlerList(), EntityPortalExitEvent.class, plugin, event -> {
            if (event.getEntity().getUniqueId().equals(pig.getUUID())) {
                calls.incrementAndGet();
                event.setAfter(new org.bukkit.util.Vector(expected.x, expected.y, expected.z));
            }
        })) {
            var destination = pig.position().add(0, 2, 0);
            pig.teleport(new TeleportTransition(world, destination, Vec3.ZERO, 0, 0, TeleportTransition.DO_NOTHING));
            check(calls.get() == 1 && pig.position().equals(destination) && pig.getDeltaMovement().equals(expected),
                "Portal exit did not apply the plugin's velocity to the native teleport");
        } finally {
            pig.discard();
        }
    }

    private static void verifyBoatUnleash(ServerLevel world, BlockPos pos, Plugin plugin) throws Exception {
        var boat = Objects.requireNonNull(EntityTypes.OAK_BOAT.create(world, EntitySpawnReason.COMMAND));
        var player = PaperReconfigurationTest.createTestPlayer(world, "UnleashFixture");
        boat.setPos(Vec3.atBottomCenterOf(pos));
        boat.setLeashedTo(player, false);
        var calls = new AtomicInteger();
        try (var listener = listen(EntityUnleashEvent.getHandlerList(), EntityUnleashEvent.class, plugin, event -> {
            check(event.getEntity().getUniqueId().equals(boat.getUUID())
                && event.getReason() == EntityUnleashEvent.UnleashReason.LEASHED_GONE && event.isDropLeash(),
                "Boat destruction provided the wrong unleash payload");
            event.setDropLeash(false);
            calls.incrementAndGet();
        })) {
            boat.remove(net.minecraft.world.entity.Entity.RemovalReason.KILLED);
            check(calls.get() == 1 && !boat.isLeashed() && boat.isRemoved(), "Destroying a leashed boat skipped the Paper event");
        }
    }

    private static void verifySulfur(ServerLevel world, BlockPos pos, Plugin plugin) throws Exception {
        var cube = Objects.requireNonNull(EntityTypes.SULFUR_CUBE.create(world, EntitySpawnReason.COMMAND));
        cube.setPos(Vec3.atBottomCenterOf(pos));
        var player = PaperReconfigurationTest.createTestPlayer(world, "SulfurFixture");
        var items = world.registryAccess().lookupOrThrow(Registries.ITEM).getOrThrow(ItemTags.SULFUR_CUBE_SWALLOWABLE).stream().toList();
        var held = new ItemStack(items.getFirst().value(), 2);
        var replacement = new ItemStack(items.getLast().value());
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        var calls = new AtomicInteger();
        try (var listener = listen(SulfurCubeSwallowItemEvent.getHandlerList(), SulfurCubeSwallowItemEvent.class, plugin, event -> {
            if (calls.getAndIncrement() == 0) {
                event.setCancelled(true);
            } else {
                event.setNewItem(org.bukkit.craftbukkit.inventory.CraftItemStack.asBukkitCopy(replacement));
            }
        })) {
            cube.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
            check(held.getCount() == 2 && cube.getItemBySlot(EquipmentSlot.BODY).isEmpty(), "Canceled swallow consumed a held item");
            cube.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
            check(calls.get() == 2 && held.getCount() == 2
                && ItemStack.isSameItemSameComponents(cube.getItemBySlot(EquipmentSlot.BODY), replacement),
                "Replacement swallow consumed the real held item or equipped the wrong variant");
        }

        var shears = new ItemStack(Items.SHEARS);
        player.setItemInHand(InteractionHand.MAIN_HAND, shears);
        var shearsEvents = new AtomicInteger();
        try (var listener = listen(PlayerShearEntityEvent.getHandlerList(), PlayerShearEntityEvent.class, plugin, event -> {
            check(event.getEntity().getUniqueId().equals(cube.getUUID()) && event.getDrops().size() == 1
                && event.getDrops().getFirst().equals(org.bukkit.craftbukkit.inventory.CraftItemStack.asBukkitCopy(replacement)),
                "Sulfur cube shearing event lost its native equipment drop");
            if (shearsEvents.getAndIncrement() == 0) {
                event.setCancelled(true);
            } else {
                event.setDrops(List.of(new org.bukkit.inventory.ItemStack(Material.DIAMOND, 2)));
            }
        })) {
            cube.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
            check(cube.readyForShearing() && shears.getDamageValue() == 0,
                "Canceled player shearing removed the equipment or damaged the shears");
            cube.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
            check(shearsEvents.get() == 2 && !cube.readyForShearing() && shears.getDamageValue() == 1,
                "Successful player shearing did not clear the equipment and damage its tool");
            verifyShearedDrops(world, pos, 2, "player and sulfur cube");
        }

        var hotCube = Objects.requireNonNull(EntityTypes.SULFUR_CUBE.create(world, EntitySpawnReason.COMMAND));
        hotCube.setPos(Vec3.atBottomCenterOf(pos));
        check(((org.bukkit.entity.SulfurCube) hotCube.getBukkitEntity()).swallow(new org.bukkit.inventory.ItemStack(Material.MAGMA_BLOCK)),
            "The hot sulfur cube fixture rejected its archetype item");
        hotCube.tick();
        var victim = Objects.requireNonNull(EntityTypes.PIG.create(world, EntitySpawnReason.COMMAND));
        victim.setPos(Vec3.atBottomCenterOf(pos));
        var contact = hotCube.getClass().getDeclaredMethod("applyContactDamage", net.minecraft.world.entity.Entity.class);
        contact.setAccessible(true);
        var contactCount = new AtomicInteger();
        try (var listener = listen(EntityDamageEvent.getHandlerList(), EntityDamageByEntityEvent.class, plugin, event -> {
            if (event.getEntity().getUniqueId().equals(victim.getUUID())) {
                check(event.getDamager().getUniqueId().equals(hotCube.getUUID())
                    && event.getCause() == EntityDamageEvent.DamageCause.CONTACT,
                    "Sulfur cube contact damage lost the event damager or cause");
                contactCount.incrementAndGet();
                event.setCancelled(true);
            }
        })) {
            contact.invoke(hotCube, victim);
            check(contactCount.get() == 1 && victim.getHealth() == victim.getMaxHealth(),
                "Sulfur cube contact damage ignored cancellation or bypassed the event");
        }

        Vec3 before = new Vec3(1, 0, 0);
        Vec3 replacementKnockback = new Vec3(0, 0.25, 0.5);
        var damage = world.damageSources().mobAttack(victim);
        var knockbackEvents = new AtomicInteger();
        try (var listener = listen(EntityKnockbackEvent.getHandlerList(), EntityKnockbackEvent.class, plugin, event -> {
            check(event instanceof com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent byEntity
                && byEntity.getHitBy().getUniqueId().equals(victim.getUUID())
                && event.getCause() == EntityKnockbackEvent.Cause.ENTITY_ATTACK,
                "Sulfur cube knockback lost its attacker or cause");
            if (knockbackEvents.getAndIncrement() == 0) {
                event.setCancelled(true);
            } else {
                event.setKnockback(new org.bukkit.util.Vector(replacementKnockback.x, replacementKnockback.y, replacementKnockback.z));
            }
        })) {
            hotCube.setDeltaMovement(before);
            hotCube.needsSync = false;
            var bridge = (LivingEntityBridge) hotCube;
            bridge.bridge$pushKnockbackCause(victim, org.bukkit.event.entity.EntityKnockbackEvent.KnockbackCause.ENTITY_ATTACK);
            hotCube.knockback(1.0, 1.0, 0.0, damage, 1.0F, false);
            check(hotCube.getDeltaMovement().equals(before) && !hotCube.needsSync,
                "Canceled sulfur cube knockback still changed its movement or scheduled a velocity update");
            bridge.bridge$pushKnockbackCause(victim, org.bukkit.event.entity.EntityKnockbackEvent.KnockbackCause.ENTITY_ATTACK);
            hotCube.knockback(1.0, 1.0, 0.0, damage, 1.0F, false);
            check(knockbackEvents.get() == 2 && hotCube.needsSync && hotCube.getDeltaMovement().equals(before.add(replacementKnockback)),
                "Sulfur cube knockback ignored the plugin's replacement impulse");
        }

        var explosive = Objects.requireNonNull(EntityTypes.SULFUR_CUBE.create(world, EntitySpawnReason.COMMAND));
        explosive.setPos(Vec3.atBottomCenterOf(pos));
        var explosiveApi = (org.bukkit.entity.SulfurCube) explosive.getBukkitEntity();
        check(explosiveApi.swallow(new org.bukkit.inventory.ItemStack(org.bukkit.Material.TNT)),
            "The sulfur cube rejected TNT through the Bukkit swallow API");
        explosive.tick();
        check(explosiveApi.canExplode(), "The sulfur cube did not apply the explosive archetype after its equipment tick");
        var ignitions = new AtomicInteger();
        var primes = new AtomicInteger();
        try (var ignite = listen(EntityIgniteEvent.getHandlerList(), EntityIgniteEvent.class, plugin, event -> {
            event.setCancelled(ignitions.getAndIncrement() == 0);
            event.setFuseTime(1);
        }); var prime = listen(ExplosionPrimeEvent.getHandlerList(), ExplosionPrimeEvent.class, plugin, event -> {
            check(event.getEntity().getUniqueId().equals(explosive.getUUID()), "Wrong entity in sulfur cube explosion prime");
            primes.incrementAndGet();
            event.setCancelled(true);
        })) {
            check(!explosiveApi.ignite(false) && explosiveApi.getFuseTicks() == PrimedTnt.NO_FUSE,
                "Canceled ignition still started the sulfur cube fuse");
            check(explosiveApi.ignite(false) && explosiveApi.getFuseTicks() == 1, "Sulfur cube ignition ignored the plugin's fuse");
            explosive.tick();
            check(ignitions.get() == 2 && primes.get() == 1 && !explosive.isRemoved()
                && explosiveApi.getFuseTicks() == PrimedTnt.NO_FUSE, "Canceled explosion removed the sulfur cube or left it primed");
        }

        world.setBlockAndUpdate(pos, Blocks.POTENT_SULFUR.defaultBlockState());
        world.setBlockAndUpdate(pos.above(), Blocks.WATER.defaultBlockState());
        world.setBlockAndUpdate(pos.above(2), Blocks.AIR.defaultBlockState());
        var sulfur = (PotentSulfurBlockEntity) world.getBlockEntity(pos);
        var pig = Objects.requireNonNull(EntityTypes.PIG.create(world, EntitySpawnReason.COMMAND));
        pig.setPos(Vec3.atBottomCenterOf(pos.above()));
        check(world.addFreshEntity(pig), "Could not spawn the geyser fixture");
        var launches = new AtomicInteger();
        try (var listener = listen(EntityPotentSulfurLaunchEvent.getHandlerList(), EntityPotentSulfurLaunchEvent.class, plugin, event -> {
            if (event.getEntity().getUniqueId().equals(pig.getUUID())) {
                check(event.getSulfurBlock().getY() == pos.getY() && event.getSourceLocation().getBlockY() == pos.getY() + 2
                    && event.getLaunchVelocity().getY() > 0, "Geyser event lost the sulfur, water surface or impulse");
                launches.incrementAndGet();
            }
        })) {
            PotentSulfurBlockEntity.LAUNCH_ENTITY_TICKER.tick(world, pos, world.getBlockState(pos), sulfur);
            check(launches.get() == 1 && pig.getDeltaMovement().y > 0, "Geyser did not notify plugins and launch the entity");
        } finally {
            pig.discard();
            world.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
            world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    private static void verifyDispenserShearing(ServerLevel world, BlockPos pos, Plugin plugin) throws Exception {
        BlockPos target = pos.east();
        var state = Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST);
        world.setBlockAndUpdate(pos, state);
        var source = new BlockSource(world, pos, state, (DispenserBlockEntity) world.getBlockEntity(pos));
        var dispenser = (DispenserBlock) state.getBlock();
        var types = List.of(EntityTypes.SHEEP, EntityTypes.MOOSHROOM, EntityTypes.BOGGED, EntityTypes.SNOW_GOLEM,
            EntityTypes.COPPER_GOLEM, EntityTypes.SULFUR_CUBE);
        try {
            for (var type : types) {
                var entity = Objects.requireNonNull(type.create(world, EntitySpawnReason.COMMAND));
                if (entity instanceof CopperGolem golem) {
                    golem.setItemSlot(CopperGolem.EQUIPMENT_SLOT_ANTENNA, new ItemStack(Items.POPPY));
                } else if (entity instanceof net.minecraft.world.entity.monster.cubemob.SulfurCube cube) {
                    cube.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.MAGMA_BLOCK));
                }
                entity.setPos(Vec3.atBottomCenterOf(target));
                check(world.addFreshEntity(entity), "Could not add the dispenser shearing fixture for " + type);
                var shearable = (Shearable) entity;
                var tool = new ItemStack(Items.SHEARS);
                var behavior = dispenser.getDispenseMethod(world, tool);
                var dispenseCalls = new AtomicInteger();
                var shearCalls = new AtomicInteger();
                try (var dispense = listen(BlockDispenseEvent.getHandlerList(), BlockDispenseEvent.class, plugin, event -> {
                    if (event.getBlock().getX() == pos.getX() && event.getBlock().getY() == pos.getY()) {
                        event.setCancelled(dispenseCalls.getAndIncrement() == 0);
                    }
                }); var shear = listen(BlockShearEntityEvent.getHandlerList(), BlockShearEntityEvent.class, plugin, event -> {
                    check(event.getEntity().getUniqueId().equals(entity.getUUID()) && event.getBlock().getX() == pos.getX()
                        && event.getBlock().getZ() == pos.getZ() && event.getTool().getType() == Material.SHEARS
                        && !event.getDrops().isEmpty(), "Dispenser shearing lost the native drops, tool or block for " + type);
                    if (shearCalls.getAndIncrement() == 0) {
                        event.setCancelled(true);
                    } else {
                        event.setDrops(List.of(new org.bukkit.inventory.ItemStack(Material.DIAMOND, 3)));
                    }
                })) {
                    behavior.dispense(source, tool);
                    check(shearCalls.get() == 0 && shearable.readyForShearing() && tool.getDamageValue() == 0,
                        "Canceled dispenser activation still sheared " + type);
                    behavior.dispense(source, tool);
                    check(shearCalls.get() == 1 && shearable.readyForShearing() && tool.getDamageValue() == 0,
                        "Canceled dispenser shearing altered the entity or tool for " + type);
                    behavior.dispense(source, tool);
                    check(dispenseCalls.get() == 3 && shearCalls.get() == 2
                        && (!entity.isAlive() || !shearable.readyForShearing()) && tool.getDamageValue() == 1,
                        "Dispenser did not complete native shearing with the plugin's drops for " + type);
                    verifyShearedDrops(world, target, 3, type.toString());
                } finally {
                    entity.discard();
                }
            }
        } finally {
            world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        System.out.println("Paper shear API: player shearing, six dispenser shearables, plugin drops, cancellation and tool durability passed");
    }

    private static void verifyShearedDrops(ServerLevel world, BlockPos pos, int count, String fixture) {
        var drops = world.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2),
            item -> item.getItem().is(Items.DIAMOND));
        check(drops.size() == 1 && drops.getFirst().getItem().getCount() == count,
            "Native shearing did not spawn the plugin-selected diamond stack exactly once for " + fixture
                + ". Found " + drops.stream().map(ItemEntity::getItem).toList());
        drops.getFirst().discard();
    }

    private static void verifySulfurDispensing(ServerLevel world, BlockPos pos, Plugin plugin) throws Exception {
        BlockPos target = pos.east();
        var state = Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST);
        world.setBlockAndUpdate(pos, state);
        var source = new BlockSource(world, pos, state, (DispenserBlockEntity) world.getBlockEntity(pos));
        var dispenser = (DispenserBlock) state.getBlock();
        var cube = Objects.requireNonNull(EntityTypes.SULFUR_CUBE.create(world, EntitySpawnReason.COMMAND));
        cube.setPos(Vec3.atBottomCenterOf(target));
        check(world.addFreshEntity(cube), "Could not spawn the sulfur cube dispenser fixture");
        try {
            for (var item : List.of(Items.MAGMA_BLOCK, Items.TNT)) {
                ItemStack stack = new ItemStack(item, 2);
                var expected = CraftItemStack.asBukkitCopy(stack.copyWithCount(1));
                var meta = expected.getItemMeta();
                meta.customName(net.kyori.adventure.text.Component.text("Dispensed by Paper"));
                expected.setItemMeta(meta);
                var behavior = dispenser.getDispenseMethod(world, stack);
                var calls = new AtomicInteger();
                try (var listener = listen(BlockDispenseEvent.getHandlerList(), BlockDispenseEvent.class, plugin, event -> {
                    check(event instanceof PaperBlockDispenseEvent && event.getItem().getType() == expected.getType()
                        && event.getItem().getAmount() == 1 && event.getBlock().getX() == pos.getX()
                        && event.getVelocity().getX() == target.getX(), "Wrong Paper sulfur cube dispense payload");
                    if (calls.getAndIncrement() == 0) {
                        event.setCancelled(true);
                    } else {
                        event.setItem(expected);
                    }
                })) {
                    behavior.dispense(source, stack);
                    check(calls.get() == 1 && stack.getCount() == 2 && !cube.readyForShearing(),
                        "Canceled cube dispense equipped its block or consumed the stack");
                    behavior.dispense(source, stack);
                    check(calls.get() == 2 && stack.getCount() == 1
                        && CraftItemStack.asBukkitCopy(cube.getItemBySlot(EquipmentSlot.BODY)).equals(expected),
                        "Sulfur cube dispenser ignored the event's replacement components");
                    check(world.getEntitiesOfClass(PrimedTnt.class, new AABB(target).inflate(1)).isEmpty(),
                        "TNT was primed by the fallback after the sulfur cube handled the dispense");
                    cube.setItemSlot(EquipmentSlot.BODY, ItemStack.EMPTY);
                }
            }
        } finally {
            cube.discard();
            world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        System.out.println("Paper sulfur dispense API: ordinary blocks, TNT, cancellation and plugin item changes passed");
    }

    private static void verifyItemSorting(ServerLevel world, BlockPos pos, Plugin plugin) throws Exception {
        var golem = Objects.requireNonNull(EntityTypes.COPPER_GOLEM.create(world, EntitySpawnReason.COMMAND));
        golem.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.POPPY));
        world.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
        var chest = (Container) world.getBlockEntity(pos);
        var matching = TransportItemsBetweenContainers.class.getDeclaredMethod("matchesLeavingItemsRequirement", PathfinderMob.class, Container.class);
        matching.setAccessible(true);
        var result = new AtomicReference<>(Event.Result.DEFAULT);
        try (var listener = listen(ItemTransportingEntitySortEvent.getHandlerList(), ItemTransportingEntitySortEvent.class, plugin, event -> {
            check(event.getEntity().getUniqueId().equals(golem.getUUID()) && event.getItemStack().getType() == Material.POPPY
                && event.getContainerInventory().getSize() == chest.getContainerSize(),
                "Copper golem sorting event lost the entity, item or destination inventory");
            event.setResult(result.get());
        })) {
            chest.setItem(0, new ItemStack(Items.STONE));
            check(!(boolean) matching.invoke(null, golem, chest), "Default copper golem sorting accepted mismatched items");
            result.set(Event.Result.ALLOW);
            check((boolean) matching.invoke(null, golem, chest), "Plugin could not allow a mismatched sorting destination");
            chest.setItem(0, new ItemStack(Items.POPPY));
            result.set(Event.Result.DENY);
            check(!(boolean) matching.invoke(null, golem, chest), "Plugin could not veto a matching sorting destination");
            result.set(Event.Result.DEFAULT);
            check((boolean) matching.invoke(null, golem, chest), "Default copper golem sorting rejected matching items");
        } finally {
            chest.clearContent();
            world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        System.out.println("Paper item sorting API: live inventory, default rules and plugin overrides passed");
    }

    private static void verifyWindChargePrime(ServerLevel world, BlockPos pos, Plugin plugin) throws Exception {
        var point = Vec3.atCenterOf(pos);
        var victim = Objects.requireNonNull(EntityTypes.PIG.create(world, EntitySpawnReason.COMMAND));
        victim.setPos(point.add(7.0, 0.0, 0.0));
        check(world.addFreshEntity(victim), "Could not place the wind charge fixture");
        try {
            var types = List.of(EntityTypes.WIND_CHARGE, EntityTypes.BREEZE_WIND_CHARGE);
            for (var type : types) {
                var charge = Objects.requireNonNull(type.create(world, EntitySpawnReason.COMMAND));
                charge.setPos(point);
                victim.setDeltaMovement(Vec3.ZERO);
                var events = new AtomicInteger();
                try (var listener = listen(ExplosionPrimeEvent.getHandlerList(), ExplosionPrimeEvent.class, plugin, event -> {
                    check(event.getEntity().getUniqueId().equals(charge.getUUID())
                        && event.getRadius() == (type == EntityTypes.WIND_CHARGE ? 1.2F : 3.0F)
                        && !event.getFire(), "Wrong wind charge prime payload for " + type);
                    if (events.getAndIncrement() == 0) {
                        event.setCancelled(true);
                    } else {
                        event.setRadius(5.0F);
                    }
                })) {
                    charge.explode(point);
                    check(events.get() == 1 && victim.getDeltaMovement().equals(Vec3.ZERO),
                        "Canceled wind charge affected nearby entities for " + type);
                    charge.explode(point);
                    check(events.get() == 2 && victim.getDeltaMovement().x > 0,
                        "Wind charge did not use the plugin's expanded radius for " + type);
                }
            }
        } finally {
            victim.discard();
        }
        System.out.println("Paper wind charge API: both explosion types honor cancellation and plugin radius changes");
    }

    private static void verifyMinecartIgnition(ServerLevel world, BlockPos pos, Plugin plugin) throws Exception {
        var minecart = Objects.requireNonNull(EntityTypes.TNT_MINECART.create(world, EntitySpawnReason.COMMAND));
        minecart.setPos(Vec3.atCenterOf(pos));
        var api = (org.bukkit.entity.minecart.ExplosiveMinecart) minecart.getBukkitEntity();
        var events = new AtomicInteger();
        try (var listener = listen(EntityIgniteEvent.getHandlerList(), EntityIgniteEvent.class, plugin, event -> {
            check(event.getEntity().getUniqueId().equals(minecart.getUUID()), "Minecart ignition event lost its source");
            int phase = events.getAndIncrement();
            check(phase < 2 ? event.getFuseTime() == 80 : event.getFuseTime() >= 0 && event.getFuseTime() <= 38,
                "Minecart ignition event did not use its native regular or shortened fuse");
            event.setCancelled(phase % 2 == 0);
            event.setFuseTime(17);
        })) {
            api.ignite();
            check(!api.isIgnited(), "Canceled API minecart ignition still set the fuse");
            api.ignite();
            check(api.isIgnited() && api.getFuseTicks() == 17, "API minecart ignition ignored the replacement fuse");
            api.setFuseTicks(PrimedTnt.NO_FUSE);
            minecart.setDeltaMovement(new Vec3(0.2, 0.0, 0.0));
            minecart.destroy(world, world.damageSources().generic());
            check(!api.isIgnited(), "Canceled moving minecart ignition still set the fuse");
            minecart.destroy(world, world.damageSources().generic());
            check(events.get() == 4 && api.isIgnited() && api.getFuseTicks() == 17,
                "Moving minecart destruction overwrote the plugin's replacement fuse");
        }
        System.out.println("Paper minecart ignition API: manual and shortened native fuses passed");
    }

    private static void verifyDataFixer(ServerLevel world, BlockPos pos, Plugin plugin) throws Exception {
        var oldChunk = new CompoundTag();
        oldChunk.putInt("DataVersion", 4884); // Immediately before the bed block-entity removal fix.
        oldChunk.putInt("xPos", pos.getX() >> 4);
        oldChunk.putInt("zPos", pos.getZ() >> 4);
        oldChunk.putString("Status", "minecraft:full");
        var oldBed = new CompoundTag();
        oldBed.putString("id", "minecraft:bed");
        oldBed.putInt("x", pos.getX());
        oldBed.putInt("y", pos.getY());
        oldBed.putInt("z", pos.getZ());
        var pluginData = new CompoundTag();
        pluginData.putString("felt:old_bed", "retained");
        oldBed.put("PublicBukkitValues", pluginData);
        var blockEntities = new ListTag();
        blockEntities.add(oldBed);
        oldChunk.put("block_entities", blockEntities);
        var seen = new AtomicReference<AsyncServerDataFixerRemoveBlockEntityEvent>();
        try (var listener = listen(AsyncServerDataFixerRemoveBlockEntityEvent.getHandlerList(), AsyncServerDataFixerRemoveBlockEntityEvent.class,
            plugin, event -> check(seen.compareAndSet(null, event), "A single data fix fired more than one block-entity removal"))) {
            var result = CompletableFuture.supplyAsync(() -> world.getChunkSource().chunkMap.upgradeChunkTag(oldChunk),
                net.minecraft.util.Util.backgroundExecutor()).get(20, TimeUnit.SECONDS);
            var event = seen.get();
            check(result.getListOrEmpty("block_entities").isEmpty() && event != null && event.isAsynchronous(),
                "The native chunk data fix did not publish its asynchronous removal");
            check(event.getWorldKey().asString().equals(world.dimension().identifier().toString())
                && event.getBlockEntityType().asString().equals("minecraft:bed")
                && event.getBlockPosition().equals(io.papermc.paper.math.Position.block(pos.getX(), pos.getY(), pos.getZ()))
                && "retained".equals(event.getPersistentDataContainerView().get(new org.bukkit.NamespacedKey("felt", "old_bed"),
                    org.bukkit.persistence.PersistentDataType.STRING)),
                "Removed block entity event lost its world, position, type or persistent data");
        }
    }

    static <E extends Event> AutoCloseable listen(HandlerList handlers, Class<E> type, Plugin plugin, Consumer<E> action) {
        var listener = new RegisteredListener(new Listener() {}, (owner, event) -> action.accept(type.cast(event)), EventPriority.NORMAL, plugin, false);
        handlers.register(listener);
        return () -> handlers.unregister(listener);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
