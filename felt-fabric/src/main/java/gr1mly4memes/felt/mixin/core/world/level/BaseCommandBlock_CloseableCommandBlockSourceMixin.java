package gr1mly4memes.felt.mixin.core.world.level;

import gr1mly4memes.felt.bridge.core.command.CommandSourceBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.level.BaseCommandBlock;
import org.bukkit.command.CommandSender;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net/minecraft/world/level/BaseCommandBlock$CloseableCommandBlockSource")
public abstract class BaseCommandBlock_CloseableCommandBlockSourceMixin implements CommandSourceBridge {

    @Shadow(aliases = {"this$0"}, remap = false) @Final
    private BaseCommandBlock outerThis;

    @Override
    public CommandSender bridge$getBukkitSender(CommandSourceStack wrapper) {
        return ((CommandSourceBridge) this.outerThis).bridge$getBukkitSender(wrapper);
    }
}
