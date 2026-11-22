package gr1mly4memes.felt.mod.permission;

import gr1mly4memes.felt.bridge.core.commands.CommandSourceStackBridge;
import gr1mly4memes.felt.mod.config.FeltConfig;
import me.lucko.fabric.api.permissions.v0.PermissionCheckEvent;
import net.fabricmc.fabric.api.util.TriState;

public class FeltPermissionImpl {
    public static void init() {
        if (!FeltConfig.isForwardPermission()) {
            return;
        }

        PermissionCheckEvent.EVENT.register((provider, permission) -> {
            if (provider instanceof CommandSourceStackBridge stack) {
                var sender = stack.bridge$getBukkitSender();
                if (sender != null) {
                    return TriState.of(sender.hasPermission(permission));
                }
            }
            return TriState.DEFAULT;
        });

        // Fixme: Bukkit didn't support offline player's permission.
//        OfflinePermissionCheckEvent.EVENT.register((uuid, permission) -> {
//            return CompletableFuture.completedFuture(TriState.FALSE);
//        });
    }
}
