package gr1mly4memes.felt.mod.server.event;

import gr1mly4memes.felt.bridge.core.world.entity.LivingEntityBridge;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.entity.monster.skeleton.Bogged;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.jetbrains.annotations.Nullable;

public final class FeltShearing {
    private static final ThreadLocal<Selection> SELECTED_DROPS = new ThreadLocal<>();

    private FeltShearing() {}

    public static List<ItemStack> defaultDrops(Entity entity, ServerLevel level, ItemStack tool) {
        return switch (entity) {
            case Sheep sheep -> ((LivingEntityBridge) sheep).bridge$collectShearDrops(level, BuiltInLootTables.SHEAR_SHEEP, tool, true);
            case MushroomCow cow -> ((LivingEntityBridge) cow).bridge$collectShearDrops(level, BuiltInLootTables.SHEAR_MOOSHROOM, tool, true);
            case Bogged bogged -> ((LivingEntityBridge) bogged).bridge$collectShearDrops(level, BuiltInLootTables.BOGGED_SHEAR, tool, false);
            case SnowGolem golem -> ((LivingEntityBridge) golem).bridge$collectShearDrops(level, BuiltInLootTables.SHEAR_SNOW_GOLEM, tool, false);
            case CopperGolem golem -> List.of(golem.getItemBySlot(CopperGolem.EQUIPMENT_SLOT_ANTENNA).copy());
            case SulfurCube cube -> List.of(cube.getItemBySlot(EquipmentSlot.BODY).copy());
            default -> List.of();
        };
    }

    public static Selection captureDrops(Entity entity, ItemStack tool, List<ItemStack> drops) {
        Selection previous = SELECTED_DROPS.get();
        SELECTED_DROPS.set(new Selection(entity, tool, drops));
        return previous;
    }

    public static void restoreDrops(@Nullable Selection previous) {
        if (previous == null) {
            SELECTED_DROPS.remove();
        } else {
            SELECTED_DROPS.set(previous);
        }
    }

    public static @Nullable List<ItemStack> takeDrops(Entity entity, ItemInstance tool) {
        Selection selection = SELECTED_DROPS.get();
        if (selection == null || selection.entity != entity || selection.tool != tool) {
            return null;
        }
        List<ItemStack> drops = selection.drops;
        selection.drops = List.of();
        return drops;
    }

    public static final class Selection {
        private final Entity entity;
        private final ItemStack tool;
        private List<ItemStack> drops;

        private Selection(Entity entity, ItemStack tool, List<ItemStack> drops) {
            this.entity = entity;
            this.tool = tool;
            this.drops = drops;
        }
    }
}
