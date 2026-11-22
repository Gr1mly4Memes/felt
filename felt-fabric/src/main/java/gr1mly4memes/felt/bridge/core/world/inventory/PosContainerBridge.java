package gr1mly4memes.felt.bridge.core.world.inventory;

import gr1mly4memes.felt.bridge.core.world.inventory.ContainerLevelAccessBridge;
import net.minecraft.world.inventory.ContainerLevelAccess;
import org.bukkit.Location;

public interface PosContainerBridge extends AbstractContainerMenuBridge {

    ContainerLevelAccess bridge$getWorldPos();

    default Location bridge$getWorldLocation() {
        return ((ContainerLevelAccessBridge) bridge$getWorldPos()).bridge$getLocation();
    }
}
