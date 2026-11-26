package gr1mly4memes.felt.mod.server.block;

import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.util.Vector;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.BooleanSupplier;

public class DispenserBlockHooks {

    public static final class Context {
        public final Context parent;
        public final net.minecraft.core.dispenser.BlockSource source;
        public final net.minecraft.world.item.ItemStack original;
        public boolean refund;
        public boolean inEvent;

        public Context(Context parent, net.minecraft.core.dispenser.BlockSource source, net.minecraft.world.item.ItemStack original) {
            this.parent = parent;
            this.source = source;
            this.original = original;
        }
    }

    private static final VarHandle H_EVENT_FIRED;
    private static final ThreadLocal<DispenseCall> DISPENSE_CALL = new ThreadLocal<>();

    static {
        try {
            var field = DispenserBlock.class.getDeclaredField("eventFired");
            H_EVENT_FIRED = MethodHandles.lookup().unreflectVarHandle(field);
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }

    public static boolean isEventFired() {
        return (boolean) H_EVENT_FIRED.get();
    }

    public static void setEventFired(boolean b) {
        H_EVENT_FIRED.set(b);
    }

    public record DispenseCall(BlockSource source, DispenseItemBehavior behavior) {}

    public static DispenseCall currentDispense() {
        return DISPENSE_CALL.get();
    }

    public static boolean withSource(BlockSource source, DispenseItemBehavior behavior, BooleanSupplier action) {
        DispenseCall previous = DISPENSE_CALL.get();
        DISPENSE_CALL.set(new DispenseCall(source, behavior));
        try {
            return action.getAsBoolean();
        } finally {
            if (previous == null) {
                DISPENSE_CALL.remove();
            } else {
                DISPENSE_CALL.set(previous);
            }
        }
    }

    public static DispenseSelection selectDispensedItem(BlockSource source, DispenseItemBehavior current,
                                                       ItemStack stack, Vector velocity) {
        var original = CraftItemStack.asBukkitCopy(stack.copyWithCount(1));
        var event = new BlockDispenseEvent(CraftBlock.at(source.level(), source.pos()), original.clone(), velocity);
        return selectDispensedItem(source, current, original, event);
    }

    public static DispenseSelection selectEquipment(BlockSource source, DispenseItemBehavior current,
                                                     ItemStack stack, LivingEntity target) {
        var original = CraftItemStack.asBukkitCopy(stack);
        var event = new BlockDispenseArmorEvent(CraftBlock.at(source.level(), source.pos()), original.clone(),
            (org.bukkit.entity.LivingEntity) target.getBukkitEntity());
        return selectDispensedItem(source, current, original, event);
    }

    private static DispenseSelection selectDispensedItem(BlockSource source, DispenseItemBehavior current,
                                                       org.bukkit.inventory.ItemStack original, BlockDispenseEvent event) {
        if (!event.callEvent()) {
            return new DispenseSelection(event, false, false);
        }
        boolean changed = !original.equals(event.getItem());
        if (changed) {
            ItemStack replacement = CraftItemStack.asNMSCopy(event.getItem());
            var behavior = ((DispenserBlock) source.state().getBlock()).getDispenseMethod(source.level(), replacement);
            if (behavior != DispenseItemBehavior.NOOP && behavior != current) {
                behavior.dispense(source, replacement);
                return new DispenseSelection(event, false, false);
            }
        }
        return new DispenseSelection(event, true, !changed);
    }

    public record DispenseSelection(BlockDispenseEvent event, boolean proceed, boolean consumeOriginal) {}
}
