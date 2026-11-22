package gr1mly4memes.felt.mod.command;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.jetbrains.annotations.NotNull;

public class ModListCommand extends BukkitCommand {

    public ModListCommand(@NotNull String name) {
        super(name);

        this.description = "Gets the version of this server including any plugins in use";
        this.usageMessage = "/mods";
        this.setPermission("felt.command.mods");
        this.setAliases(List.of("fabricmods"));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!sender.hasPermission("felt.command.mods")) {
            sender.sendMessage("No Permission for command!");
            return true;
        }
        List<String> mods = new ArrayList<>();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            String name = mod.getMetadata().getName();

            if (name.startsWith("Fabric") && name.endsWith(")")) continue; // Don't list all modules of FAPI
            if (name.startsWith("Felt") && !name.endsWith("Mod")) continue;// Don't list all modules of Felt
            if (name.startsWith("Fabric API Base")) name = "Fabric API";
            if (name.startsWith("OpenJDK")) name = name.replace(" 64-Bit Server VM",""); // Shorten
            if (name.startsWith("Minecraft")) continue;

            if (!mods.contains(name)) {
                mods.add(name);
            }
        }
        sender.sendMessage("Mods: " + String.join(", ", mods));
        return true;
    }
}
