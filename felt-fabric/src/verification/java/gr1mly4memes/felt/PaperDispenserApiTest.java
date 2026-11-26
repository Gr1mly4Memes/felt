package gr1mly4memes.felt;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.util.Vector;

final class PaperDispenserApiTest {
    static void verify(ServerLevel world, BlockPos pos, Plugin plugin) throws Exception {
        BlockPos target = pos.east();
        var state = Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST);
        world.setBlockAndUpdate(pos, state);
        var source = new BlockSource(world, pos, state, (DispenserBlockEntity) world.getBlockEntity(pos));
        try {
            world.setBlockAndUpdate(target.below(), Blocks.STONE.defaultBlockState());
            world.setBlockAndUpdate(target, Blocks.WATER.defaultBlockState());
            verifyVehicle(source, target, Items.OAK_BOAT, AbstractBoat.class, plugin);
            world.setBlockAndUpdate(target, Blocks.RAIL.defaultBlockState());
            verifyVehicle(source, target, Items.MINECART, AbstractMinecart.class, plugin);
            world.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
            verifyArrow(source, target, plugin);
            verifyShulkerBox(source, target, plugin);
            verifyBoneMeal(source, target, plugin);
            verifyBlockPlacements(source, target, plugin);
            verifyEquipment(source, target, plugin);
            verifyAnimalEquipment(source, target, plugin);
            verifyLiquidDispensing(source, target, plugin);
            verifyDirectEntityDispense(source, target, plugin);
            verifySpawnEggDispense(source, target, plugin);
            verifyFlintAndSteel(source, target, plugin);
        } finally {
            world.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
            world.setBlockAndUpdate(target.below(), Blocks.AIR.defaultBlockState());
            world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        System.out.println("Paper dispenser API: vehicles, projectiles, shulker boxes, bone meal, block placements, equipment, liquids, ignition and spawn eggs passed");
    }

    private static void verifyVehicle(BlockSource source, BlockPos target, Item item, Class<? extends Entity> type, Plugin plugin)
        throws Exception {
        var stack = new ItemStack(item, 2);
        var area = new AABB(target).inflate(2);
        Vector selected = new Vector(target.getX() + 0.75, target.getY() + 1.0, target.getZ() + 0.75);
        var calls = new AtomicInteger();
        try (var dispense = PaperEventHooksTest.listen(BlockDispenseEvent.getHandlerList(), BlockDispenseEvent.class, plugin, event -> {
            checkItem(source, stack, event);
            event.setCancelled(calls.getAndIncrement() == 0);
            event.setVelocity(selected);
        })) {
            dispense(source, stack);
            check(calls.get() == 1 && stack.getCount() == 2 && source.level().getEntitiesOfClass(type, area).isEmpty(),
                "Canceling a vehicle dispense still spawned or consumed " + item);
            var spawnCalls = new AtomicInteger();
            try (var spawn = PaperEventHooksTest.listen(VehicleCreateEvent.getHandlerList(), VehicleCreateEvent.class, plugin, event -> {
                spawnCalls.incrementAndGet();
                event.setCancelled(true);
            })) {
                dispense(source, stack);
                check(calls.get() == 2 && spawnCalls.get() == 1 && stack.getCount() == 2 && source.level().getEntitiesOfClass(type, area).isEmpty(),
                    "Canceling a vehicle spawn still consumed " + item);
            }
            dispense(source, stack);
            var vehicles = source.level().getEntitiesOfClass(type, area);
            check(calls.get() == 3 && stack.getCount() == 1 && vehicles.size() == 1,
                "Vehicle dispense did not spawn one vehicle and consume one " + item);
            var vehicle = vehicles.getFirst();
            check(vehicle.getX() == selected.getX() && vehicle.getY() == selected.getY() && vehicle.getZ() == selected.getZ(),
                "Vehicle dispense ignored the event position for " + item);
            vehicle.discard();
        }
    }

    private static void verifyArrow(BlockSource source, BlockPos target, Plugin plugin) throws Exception {
        var stack = new ItemStack(Items.ARROW, 2);
        var area = new AABB(target).inflate(2);
        var calls = new AtomicInteger();
        try (var listener = PaperEventHooksTest.listen(BlockDispenseEvent.getHandlerList(), BlockDispenseEvent.class, plugin, event -> {
            checkItem(source, stack, event);
            event.setCancelled(calls.getAndIncrement() == 0);
            event.setVelocity(new Vector(0, 1, 0));
        })) {
            dispense(source, stack);
            check(calls.get() == 1 && stack.getCount() == 2 && source.level().getEntitiesOfClass(Arrow.class, area).isEmpty(),
                "Canceling an arrow dispense still launched or consumed it");
            dispense(source, stack);
            var arrows = source.level().getEntitiesOfClass(Arrow.class, area);
            check(calls.get() == 2 && stack.getCount() == 1 && arrows.size() == 1, "Dispenser did not launch a single arrow");
            var arrow = arrows.getFirst();
            var projectile = (org.bukkit.entity.Projectile) arrow.getBukkitEntity();
            check(arrow.getDeltaMovement().y > Math.abs(arrow.getDeltaMovement().x)
                && projectile.getShooter() instanceof BlockProjectileSource shooter && shooter.getBlock().getX() == source.pos().getX(),
                "Dispensed arrow lost its launch vector or block shooter");
            arrow.discard();
        }
    }

    private static void verifyShulkerBox(BlockSource source, BlockPos target, Plugin plugin) throws Exception {
        var stack = new ItemStack(Items.SHULKER_BOX, 2);
        var item = CraftItemStack.asBukkitCopy(stack.copyWithCount(1));
        var meta = item.getItemMeta();
        meta.customName(Component.text("Plugin shulker"));
        item.setItemMeta(meta);
        var calls = new AtomicInteger();
        try (var listener = PaperEventHooksTest.listen(BlockDispenseEvent.getHandlerList(), BlockDispenseEvent.class, plugin, event -> {
            checkItem(source, stack, event);
            event.setCancelled(calls.getAndIncrement() == 0);
            event.setItem(item);
        })) {
            dispense(source, stack);
            check(calls.get() == 1 && stack.getCount() == 2 && source.level().isEmptyBlock(target),
                "Canceling a shulker box dispense still consumed or placed it");
            dispense(source, stack);
            check(calls.get() == 2 && stack.getCount() == 1,
                "Shulker dispense did not consume one item. Events " + calls.get() + ", remaining " + stack.getCount());
            check(source.level().getBlockEntity(target) instanceof ShulkerBoxBlockEntity,
                "Shulker was not placed at " + target + ". Found " + source.level().getBlockState(target));
            var shulker = (ShulkerBoxBlockEntity) source.level().getBlockEntity(target);
            check(shulker.getCustomName() != null && shulker.getCustomName().getString().equals("Plugin shulker"),
                "Placed shulker lost the plugin's item components. Custom name " + shulker.getCustomName());
        }
    }

    private static void verifyBoneMeal(BlockSource source, BlockPos target, Plugin plugin) throws Exception {
        var level = source.level();
        var stack = new ItemStack(Items.BONE_MEAL, 64);
        var crop = Blocks.WHEAT.defaultBlockState();
        level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(target.below(), Blocks.FARMLAND.defaultBlockState());
        level.setBlockAndUpdate(target, crop);
        try (var veto = PaperEventHooksTest.listen(BlockDispenseEvent.getHandlerList(), BlockDispenseEvent.class, plugin, event -> {
            checkItem(source, stack, event);
            check(event.getVelocity().equals(new Vector(target.getX(), target.getY(), target.getZ())), "Incorrect bone meal target");
            event.setCancelled(true);
        })) {
            dispense(source, stack);
            check(level.getBlockState(target) == crop && stack.getCount() == 64, "Canceled dispense used bone meal");
        }
        var fertilizations = new AtomicInteger();
        try (var veto = PaperEventHooksTest.listen(BlockFertilizeEvent.getHandlerList(), BlockFertilizeEvent.class, plugin, event -> {
            check(event.getPlayer() == null && event.getBlock().getX() == target.getX() && event.getBlock().getY() == target.getY()
                && event.getBlocks().size() == 1 && level.getBlockState(target) == crop, "Crop event lacked an uncommitted growth snapshot");
            fertilizations.incrementAndGet();
            event.setCancelled(true);
        })) {
            dispense(source, stack);
            check(fertilizations.get() == 1 && stack.getCount() == 63 && level.getBlockState(target) == crop,
                "Canceled fertilization committed growth or refunded native bone meal");
        }

        var sapling = Blocks.BIRCH_SAPLING.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
        level.setBlockAndUpdate(target.below(), Blocks.DIRT.defaultBlockState());
        level.setBlockAndUpdate(target, sapling);
        var structureEvent = new AtomicBoolean();
        var committed = new AtomicBoolean();
        try (var structure = PaperEventHooksTest.listen(StructureGrowEvent.getHandlerList(), StructureGrowEvent.class, plugin, event -> {
            check(event.getSpecies() == TreeType.BIRCH && event.getPlayer() == null && !event.isFromBonemeal()
                && level.getBlockState(target) == sapling, "Incorrect dispenser tree event");
            structureEvent.set(true);
            event.setCancelled(true);
        }); var fertilize = PaperEventHooksTest.listen(BlockFertilizeEvent.getHandlerList(), BlockFertilizeEvent.class, plugin, event -> {
            if (structureEvent.get()) {
                check(event.isCancelled() && event.getBlocks().size() > 1, "Fertilize event lost the structure veto or tree snapshots");
                var chosen = event.getBlock().getState();
                chosen.setType(Material.STONE);
                event.getBlocks().clear();
                event.getBlocks().add(chosen);
                event.setCancelled(false);
                committed.set(true);
            }
        })) {
            for (int tries = 0; tries < 40 && !committed.get(); tries++) {
                dispense(source, stack);
            }
        }
        check(committed.get() && level.getBlockState(target).is(Blocks.STONE)
            && level.isEmptyBlock(target.above()), "Plugin overrides did not govern dispenser tree growth");
    }

    private static void verifyBlockPlacements(BlockSource source, BlockPos target, Plugin plugin) throws Exception {
        var level = source.level();
        level.setBlockAndUpdate(target, Blocks.COPPER_BLOCK.weathering().unaffected().defaultBlockState());
        verifyCancellablePlacement(source, target, Items.HONEYCOMB, plugin);
        check(level.getBlockState(target).is(Blocks.COPPER_BLOCK.waxed().unaffected()), "Honeycomb failed to wax the target");

        level.setBlockAndUpdate(target, Blocks.RESPAWN_ANCHOR.defaultBlockState());
        verifyCancellablePlacement(source, target, Items.GLOWSTONE, plugin);
        check(level.getBlockState(target).getValue(RespawnAnchorBlock.CHARGE) == 1, "Glowstone failed to charge the anchor");
        level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());

        var center = target.below();
        var base = center.below();
        try {
            level.setBlockAndUpdate(center, Blocks.SNOW_BLOCK.defaultBlockState());
            level.setBlockAndUpdate(base, Blocks.SNOW_BLOCK.defaultBlockState());
            verifyCancellablePlacement(source, target, Items.CARVED_PUMPKIN, plugin);
            var golems = level.getEntitiesOfClass(SnowGolem.class, new AABB(base).inflate(2));
            check(golems.size() == 1 && level.isEmptyBlock(center), "Pumpkin did not construct a snow golem");
            golems.getFirst().discard();

            level.setBlockAndUpdate(center, Blocks.SOUL_SAND.defaultBlockState());
            level.setBlockAndUpdate(base, Blocks.SOUL_SAND.defaultBlockState());
            level.setBlockAndUpdate(center.north(), Blocks.SOUL_SAND.defaultBlockState());
            level.setBlockAndUpdate(center.south(), Blocks.SOUL_SAND.defaultBlockState());
            verifyCancellablePlacement(source, target, Items.WITHER_SKELETON_SKULL, plugin);
            check(level.getBlockState(target).is(Blocks.WITHER_SKELETON_SKULL), "Wither skull was not placed on the base");
        } finally {
            level.setBlockAndUpdate(base, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(center.north(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(center.south(), Blocks.AIR.defaultBlockState());
        }
    }

    private static void verifyCancellablePlacement(BlockSource source, BlockPos target, Item item, Plugin plugin) throws Exception {
        var stack = new ItemStack(item, 2);
        var before = source.level().getBlockState(target);
        var calls = new AtomicInteger();
        try (var listener = PaperEventHooksTest.listen(BlockDispenseEvent.getHandlerList(), BlockDispenseEvent.class, plugin, event -> {
            checkItem(source, stack, event);
            check(event.getVelocity().equals(new Vector(target.getX(), target.getY(), target.getZ())),
                "Incorrect dispense position for " + item);
            event.setCancelled(calls.getAndIncrement() == 0);
        })) {
            dispense(source, stack);
            check(calls.get() == 1 && stack.getCount() == 2 && source.level().getBlockState(target) == before,
                "Canceling dispense mutated the target or stack for " + item);
            dispense(source, stack);
            check(calls.get() == 2 && stack.getCount() == 1, "Accepted dispense did not consume one " + item);
        }
    }

    private static void verifyEquipment(BlockSource source, BlockPos target, Plugin plugin) throws Exception {
        var level = source.level();
        level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
        var stand = java.util.Objects.requireNonNull(EntityTypes.ARMOR_STAND.create(level, EntitySpawnReason.COMMAND));
        stand.setPos(Vec3.atBottomCenterOf(target));
        check(level.addFreshEntity(stand), "Could not spawn dispenser equipment fixture");
        try {
            for (Item item : new Item[]{Items.IRON_HELMET, Items.CARVED_PUMPKIN, Items.WITHER_SKELETON_SKULL}) {
                var stack = new ItemStack(item);
                var selected = CraftItemStack.asBukkitCopy(stack);
                var meta = selected.getItemMeta();
                meta.customName(Component.text("Plugin equipment"));
                selected.setItemMeta(meta);
                var calls = new AtomicInteger();
                try (var listener = PaperEventHooksTest.listen(BlockDispenseEvent.getHandlerList(), BlockDispenseEvent.class, plugin, event -> {
                    if (event instanceof BlockDispenseArmorEvent armor) {
                        checkItem(source, stack, armor);
                        check(armor.getTargetEntity().getUniqueId().equals(stand.getUUID()), "Armor dispense chose the wrong entity");
                        int call = calls.getAndIncrement();
                        if (call == 0) {
                            armor.setCancelled(true);
                        } else if (call == 1) {
                            armor.setItem(selected);
                        }
                    } else {
                        event.setCancelled(true);
                    }
                })) {
                    dispense(source, stack);
                    check(calls.get() == 1 && stack.getCount() == 1 && stand.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),
                        "Vetoed armor event equipped or consumed " + item);
                    dispense(source, stack);
                    check(calls.get() == 2 && stack.getCount() == 1
                        && CraftItemStack.asBukkitCopy(stand.getItemBySlot(EquipmentSlot.HEAD)).equals(selected),
                        "Same-behavior item replacement lost the original or the selected metadata for " + item);
                    stand.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                    dispense(source, stack);
                    check(calls.get() == 3 && stack.isEmpty() && stand.getItemBySlot(EquipmentSlot.HEAD).is(item),
                        "Successful equipment dispense did not consume and equip " + item);
                    stand.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                }
            }
        } finally {
            stand.discard();
        }
    }

    private static void verifyAnimalEquipment(BlockSource source, BlockPos target, Plugin plugin) throws Exception {
        var level = source.level();
        var donkey = java.util.Objects.requireNonNull(EntityTypes.DONKEY.create(level, EntitySpawnReason.COMMAND));
        donkey.setTamed(true);
        donkey.setPos(Vec3.atBottomCenterOf(target));
        check(level.addFreshEntity(donkey), "Could not spawn donkey dispenser fixture");
        try {
            var chest = new ItemStack(Items.CHEST, 2);
            verifyAnimalDispense(source, donkey, chest, () -> !donkey.hasChest() && chest.getCount() == 2,
                () -> donkey.hasChest() && chest.getCount() == 1, plugin);
        } finally {
            donkey.discard();
        }

        var armadillo = java.util.Objects.requireNonNull(EntityTypes.ARMADILLO.create(level, EntitySpawnReason.COMMAND));
        armadillo.setPos(Vec3.atBottomCenterOf(target));
        check(level.addFreshEntity(armadillo), "Could not spawn brush dispenser fixture");
        try {
            var brush = new ItemStack(Items.BRUSH);
            verifyAnimalDispense(source, armadillo, brush, () -> brush.getDamageValue() == 0,
                () -> brush.getDamageValue() == 16, plugin);
        } finally {
            armadillo.discard();
            level.getEntitiesOfClass(ItemEntity.class, new AABB(target).inflate(2), item -> item.getItem().is(Items.ARMADILLO_SCUTE))
                .forEach(Entity::discard);
        }
    }

    private static void verifyAnimalDispense(BlockSource source, net.minecraft.world.entity.LivingEntity animal, ItemStack stack,
                                             java.util.function.BooleanSupplier vetoed, java.util.function.BooleanSupplier accepted,
                                             Plugin plugin) throws Exception {
        var calls = new AtomicInteger();
        try (var listener = PaperEventHooksTest.listen(BlockDispenseEvent.getHandlerList(), BlockDispenseEvent.class, plugin, event -> {
            check(event instanceof BlockDispenseArmorEvent armor && armor.getTargetEntity().getUniqueId().equals(animal.getUUID()),
                "Animal dispense did not identify the target");
            checkItem(source, stack, event);
            event.setCancelled(calls.getAndIncrement() == 0);
        })) {
            dispense(source, stack);
            check(calls.get() == 1 && vetoed.getAsBoolean(), "Canceling dispenser equipment altered " + animal.getType());
            dispense(source, stack);
            check(calls.get() == 2 && accepted.getAsBoolean(), "Successful dispenser equipment did not complete for " + animal.getType());
        }
    }

    private static void verifyLiquidDispensing(BlockSource source, BlockPos target, Plugin plugin) throws Exception {
        var level = source.level();
        var stairs = Blocks.OAK_STAIRS.defaultBlockState();
        level.setBlockAndUpdate(target, stairs);
        verifyLiquidDispense(source, target, new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.BUCKET), plugin);
        check(level.getBlockState(target).getValue(BlockStateProperties.WATERLOGGED), "Filled bucket did not waterlog the target");

        verifyLiquidDispense(source, target, new ItemStack(Items.BUCKET), new ItemStack(Items.WATER_BUCKET), plugin);
        check(level.getBlockState(target) == stairs, "Empty bucket did not collect the waterlogged fluid");

        level.setBlockAndUpdate(target, Blocks.WATER.defaultBlockState());
        var water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        verifyLiquidDispense(source, target, new ItemStack(Items.GLASS_BOTTLE), water, plugin);
        check(level.getBlockState(target).is(Blocks.WATER), "Water bottle removed the source");
        verifyLiquidDispense(source, target, new ItemStack(Items.BUCKET), new ItemStack(Items.WATER_BUCKET), plugin);
        check(level.isEmptyBlock(target), "Empty bucket did not remove the water source");

        var hive = Blocks.BEE_NEST.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL, 5);
        level.setBlockAndUpdate(target, hive);
        verifyLiquidDispense(source, target, new ItemStack(Items.GLASS_BOTTLE), new ItemStack(Items.HONEY_BOTTLE), plugin);
        check(level.getBlockState(target).getValue(BeehiveBlock.HONEY_LEVEL) == 0, "Honey bottle did not drain the hive");

        level.setBlockAndUpdate(target, Blocks.DIRT.defaultBlockState());
        verifyLiquidDispense(source, target, water, new ItemStack(Items.GLASS_BOTTLE), plugin);
        check(level.getBlockState(target).is(Blocks.MUD), "Water potion did not convert dirt into mud");

        level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
        verifyLiquidDispense(source, target, new ItemStack(Items.POWDER_SNOW_BUCKET), new ItemStack(Items.BUCKET), plugin);
        check(level.getBlockState(target).is(Blocks.POWDER_SNOW), "Powder snow bucket did not place its contents");

        level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
        var original = new ItemStack(Items.WATER_BUCKET);
        try (var listener = PaperEventHooksTest.listen(BlockDispenseEvent.getHandlerList(), BlockDispenseEvent.class, plugin, event -> {
            checkItem(source, original, event);
            var salmon = CraftItemStack.asBukkitCopy(new ItemStack(Items.SALMON_BUCKET));
            var meta = salmon.getItemMeta();
            meta.customName(Component.text("Plugin fish"));
            salmon.setItemMeta(meta);
            event.setItem(salmon);
        })) {
            check(dispense(source, original).is(Items.BUCKET), "Replacing a filled bucket did not return its empty bucket");
            var fish = level.getEntitiesOfClass(Entity.class, new AABB(target).inflate(2), entity -> entity.getType() == EntityTypes.SALMON);
            check(fish.size() == 1 && fish.getFirst().getName().getString().equals("Plugin fish"),
                "The dispensed bucket lost the plugin-selected animal or its custom name");
            fish.getFirst().discard();
        }
    }

    private static void verifyLiquidDispense(BlockSource source, BlockPos target, ItemStack stack, ItemStack expected, Plugin plugin)
        throws Exception {
        var before = source.level().getBlockState(target);
        var calls = new AtomicInteger();
        try (var listener = PaperEventHooksTest.listen(BlockDispenseEvent.getHandlerList(), BlockDispenseEvent.class, plugin, event -> {
            checkItem(source, stack, event);
            check(event.getVelocity().equals(new Vector(target.getX(), target.getY(), target.getZ())), "Wrong liquid dispense target");
            event.setCancelled(calls.getAndIncrement() == 0);
        })) {
            check(dispense(source, stack) == stack && stack.getCount() == 1 && source.level().getBlockState(target) == before
                && calls.get() == 1, "Canceled liquid dispense mutated the world or stack for " + stack);
            ItemStack result = dispense(source, stack);
            check(calls.get() == 2 && result.getCount() == 1 && ItemStack.isSameItemSameComponents(result, expected),
                "Wrong remainder after dispensing liquid. Expected " + expected + " but received " + result);
        }
    }

    private static void verifyDirectEntityDispense(BlockSource source, BlockPos target, Plugin plugin) throws Exception {
        var level = source.level();
        level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
        var area = new AABB(target).inflate(2);
        var armorStand = new ItemStack(Items.ARMOR_STAND, 2);
        var standEvents = new AtomicInteger();
        try (var listener = PaperEventHooksTest.listen(BlockDispenseEvent.getHandlerList(), BlockDispenseEvent.class, plugin, event -> {
            checkItem(source, armorStand, event);
            if (standEvents.getAndIncrement() == 0) {
                event.setCancelled(true);
            } else {
                var selected = event.getItem();
                var meta = selected.getItemMeta();
                meta.customName(Component.text("Plugin stand"));
                selected.setItemMeta(meta);
                event.setItem(selected);
            }
        })) {
            dispense(source, armorStand);
            check(standEvents.get() == 1 && armorStand.getCount() == 2 && level.getEntitiesOfClass(ArmorStand.class, area).isEmpty(),
                "Canceled armor stand dispense spawned or consumed an item");
            dispense(source, armorStand);
            var stands = level.getEntitiesOfClass(ArmorStand.class, area);
            check(standEvents.get() == 2 && armorStand.getCount() == 2 && stands.size() == 1
                && stands.getFirst().getName().getString().equals("Plugin stand"), "Armor stand dispense lost the replacement data");
            stands.getFirst().discard();
        }

        var tnt = new ItemStack(Items.TNT, 2);
        var tntEvents = new AtomicInteger();
        var position = new Vec3(target.getX() + 0.75, target.getY() + 0.5, target.getZ() + 0.25);
        try (var listener = PaperEventHooksTest.listen(BlockDispenseEvent.getHandlerList(), BlockDispenseEvent.class, plugin, event -> {
            checkItem(source, tnt, event);
            event.setCancelled(tntEvents.getAndIncrement() == 0);
            event.setVelocity(new Vector(position.x, position.y, position.z));
        })) {
            dispense(source, tnt);
            check(tntEvents.get() == 1 && tnt.getCount() == 2 && level.getEntitiesOfClass(PrimedTnt.class, area).isEmpty(),
                "Canceled TNT dispense primed or consumed the item");
            dispense(source, tnt);
            var primed = level.getEntitiesOfClass(PrimedTnt.class, area);
            check(tntEvents.get() == 2 && tnt.getCount() == 1 && primed.size() == 1 && primed.getFirst().position().equals(position),
                "Dispensed TNT ignored the selected position or item count");
            primed.getFirst().discard();
        }
    }

    private static void verifySpawnEggDispense(BlockSource source, BlockPos target, Plugin plugin) throws Exception {
        var level = source.level();
        var area = new AABB(target).inflate(2);
        var eggs = new ItemStack(Items.PIG_SPAWN_EGG, 2);
        var selected = CraftItemStack.asBukkitCopy(new ItemStack(Items.COW_SPAWN_EGG));
        var meta = selected.getItemMeta();
        meta.customName(Component.text("Plugin cow"));
        selected.setItemMeta(meta);
        var calls = new AtomicInteger();
        try (var listener = PaperEventHooksTest.listen(BlockDispenseEvent.getHandlerList(), BlockDispenseEvent.class, plugin, event -> {
            checkItem(source, eggs, event);
            int attempt = calls.getAndIncrement();
            if (attempt == 0) {
                event.setCancelled(true);
            } else if (attempt == 1) {
                event.setItem(selected);
            }
        })) {
            dispense(source, eggs);
            check(calls.get() == 1 && eggs.getCount() == 2 && level.getEntitiesOfClass(Entity.class, area,
                entity -> entity.getType() == EntityTypes.PIG || entity.getType() == EntityTypes.COW).isEmpty(),
                "Canceled spawn egg changed the stack or spawned an animal");
            dispense(source, eggs);
            var cows = level.getEntitiesOfClass(Entity.class, area, entity -> entity.getType() == EntityTypes.COW);
            check(calls.get() == 2 && eggs.getCount() == 2 && cows.size() == 1
                && cows.getFirst().getName().getString().equals("Plugin cow"),
                "Spawn egg lost the plugin's replacement type or custom name");
            cows.getFirst().discard();
            dispense(source, eggs);
            var pigs = level.getEntitiesOfClass(Entity.class, area, entity -> entity.getType() == EntityTypes.PIG);
            check(calls.get() == 3 && eggs.getCount() == 1 && pigs.size() == 1, "Ordinary spawn egg did not spawn and consume one item");
            pigs.getFirst().discard();
        }
    }

    private static void verifyFlintAndSteel(BlockSource source, BlockPos target, Plugin plugin) throws Exception {
        var level = source.level();
        level.setBlockAndUpdate(target.below(), Blocks.STONE.defaultBlockState());
        var tool = new ItemStack(Items.FLINT_AND_STEEL);
        var dispenses = new AtomicInteger();
        var ignitions = new AtomicInteger();
        try (var dispense = PaperEventHooksTest.listen(BlockDispenseEvent.getHandlerList(), BlockDispenseEvent.class, plugin, event -> {
            checkItem(source, tool, event);
            event.setCancelled(dispenses.getAndIncrement() == 0);
        }); var ignitionsListener = PaperEventHooksTest.listen(BlockIgniteEvent.getHandlerList(), BlockIgniteEvent.class, plugin, event -> {
            check(event.getCause() == BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL
                && event.getIgnitingBlock() != null && event.getIgnitingBlock().getX() == source.pos().getX()
                && event.getBlock().getX() == target.getX(), "Block ignition lost its dispenser or target");
            event.setCancelled(ignitions.getAndIncrement() == 0);
        })) {
            dispense(source, tool);
            check(ignitions.get() == 0 && tool.getDamageValue() == 0 && level.isEmptyBlock(target), "Dispense veto allowed block ignition");
            dispense(source, tool);
            check(ignitions.get() == 1 && tool.getDamageValue() == 1 && level.isEmptyBlock(target), "Block ignite veto lost Paper's durability rules");
            dispense(source, tool);
            check(ignitions.get() == 2 && tool.getDamageValue() == 2 && level.getBlockState(target).getBlock() instanceof BaseFireBlock,
                "Flint and steel did not ignite the target");
        }

        level.setBlockAndUpdate(target, Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, false));
        dispense(source, tool);
        check(level.getBlockState(target).getValue(BlockStateProperties.LIT) && tool.getDamageValue() == 3,
            "Flint and steel did not light a campfire");

        level.setBlockAndUpdate(target, Blocks.TNT.defaultBlockState());
        var primes = new AtomicInteger();
        var area = new AABB(target).inflate(2);
        try (var listener = PaperEventHooksTest.listen(TNTPrimeEvent.getHandlerList(), TNTPrimeEvent.class, plugin, event -> {
            check(event.getCause() == TNTPrimeEvent.PrimeCause.DISPENSER && event.getPrimingEntity() == null
                && event.getPrimingBlock() != null && event.getPrimingBlock().getX() == source.pos().getX(),
                "TNT prime event lost its dispenser cause");
            event.setCancelled(primes.getAndIncrement() == 0);
        })) {
            dispense(source, tool);
            check(primes.get() == 1 && tool.getDamageValue() == 3 && level.getBlockState(target).is(Blocks.TNT)
                && level.getEntitiesOfClass(PrimedTnt.class, area).isEmpty(), "TNT veto still primed the block or damaged the tool");
            dispense(source, tool);
            var primed = level.getEntitiesOfClass(PrimedTnt.class, area);
            check(primes.get() == 2 && tool.getDamageValue() == 4 && level.isEmptyBlock(target) && primed.size() == 1,
                "Dispenser ignition did not prime and replace the TNT block");
            primed.getFirst().discard();
        }

        var cube = java.util.Objects.requireNonNull(EntityTypes.SULFUR_CUBE.create(level, EntitySpawnReason.COMMAND));
        cube.setPos(Vec3.atBottomCenterOf(target));
        var api = (org.bukkit.entity.SulfurCube) cube.getBukkitEntity();
        check(api.swallow(new org.bukkit.inventory.ItemStack(Material.TNT)), "Sulfur fixture rejected TNT");
        cube.tick();
        check(level.addFreshEntity(cube) && api.canExplode(), "Could not spawn an explosive sulfur cube");
        try (var listener = PaperEventHooksTest.listen(BlockDispenseEvent.getHandlerList(), BlockDispenseEvent.class, plugin, event ->
            event.setCancelled(true))) {
            dispense(source, tool);
            check(!cube.isPrimed() && tool.getDamageValue() == 4 && level.isEmptyBlock(target),
                "Canceled sulfur cube ignition fell back to lighting a block");
        }
        try {
            dispense(source, tool);
            check(cube.isPrimed() && tool.getDamageValue() == 5, "Flint and steel did not ignite the sulfur cube");
        } finally {
            cube.discard();
        }
    }

    private static ItemStack dispense(BlockSource source, ItemStack stack) {
        var block = (DispenserBlock) source.state().getBlock();
        return block.getDispenseMethod(source.level(), stack).dispense(source, stack);
    }

    private static void checkItem(BlockSource source, ItemStack stack, BlockDispenseEvent event) {
        check(event.getBlock().getX() == source.pos().getX() && event.getBlock().getY() == source.pos().getY()
            && event.getItem().equals(CraftItemStack.asBukkitCopy(stack.copyWithCount(1))), "Wrong block or single item in dispenser event");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
