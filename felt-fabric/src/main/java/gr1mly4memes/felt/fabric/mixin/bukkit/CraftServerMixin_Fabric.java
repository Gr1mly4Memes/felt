package gr1mly4memes.felt.fabric.mixin.bukkit;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import gr1mly4memes.felt.common.mod.server.FeltServer;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.command.CraftBlockCommandSender;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = CraftServer.class, remap = false)
public abstract class CraftServerMixin_Fabric {
    @ModifyVariable(method = "dispatchCommand", remap = false, index = 2, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lorg/spigotmc/AsyncCatcher;catchOp(Ljava/lang/String;)V"))
    private String arclight$mod$commandEvent(String commandLine, CommandSender sender) {
        CommandSourceStack commandSource;
        if (sender instanceof CraftEntity) {
            Entity entity = ((CraftEntity) sender).getHandle();
            if (entity instanceof ServerPlayer player) {
                commandSource = player.createCommandSourceStack();
            } else if (entity.level() instanceof ServerLevel level) {
                commandSource = entity.createCommandSourceStackForNameResolution(level);
            } else {
                return commandLine;
            }
        } else if (sender == Bukkit.getConsoleSender()) {
            commandSource = FeltServer.getMinecraftServer().createCommandSourceStack();
        } else if (sender instanceof CraftBlockCommandSender) {
            commandSource = ((CraftBlockCommandSender) sender).getSourceStack();
        } else {
            return commandLine;
        }
        StringReader stringreader = new StringReader("/" + commandLine);
        if (stringreader.canRead() && stringreader.peek() == '/') {
            stringreader.skip();
        }
        ParseResults<CommandSourceStack> parse = FeltServer.getMinecraftServer().getCommands()
                .getDispatcher().parse(stringreader, commandSource);
        // Todo: Command event.
        var str = parse.getReader().getString();
        return str.startsWith("/") ? str.substring(1) : str;
    }
}
