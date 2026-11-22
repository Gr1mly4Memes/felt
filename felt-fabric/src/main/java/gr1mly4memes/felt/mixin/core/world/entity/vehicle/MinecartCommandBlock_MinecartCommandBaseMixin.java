package gr1mly4memes.felt.mixin.core.world.entity.vehicle;

import gr1mly4memes.felt.bridge.core.command.CommandSourceBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.vehicle.minecart.MinecartCommandBlock;
import org.bukkit.command.CommandSender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net/minecraft/world/entity/vehicle/minecart/MinecartCommandBlock$MinecartCommandBase")
public abstract class MinecartCommandBlock_MinecartCommandBaseMixin implements CommandSourceBridge {
    @SuppressWarnings("target")
    @Shadow(aliases = {"this$0", "f_38537_", "field_7745"}, remap = false)
    private MinecartCommandBlock outerThis;

    public CommandSender getBukkitSender(CommandSourceStack wrapper) {
        return outerThis.bridge$getBukkitEntity();
    }

    @Override
    public CommandSender bridge$getBukkitSender(CommandSourceStack wrapper) {
        return getBukkitSender(wrapper);
    }
}