package gr1mly4memes.felt.fabric.mixin.bukkit;

import gr1mly4memes.felt.fabric.mod.permission.FeltFabricPermissible;
import gr1mly4memes.felt.i18n.FeltConfig;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.ServerOperator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CraftHumanEntity.class, remap = false)
public abstract class CraftHumanEntityMixin_Fabric {

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "(Lorg/bukkit/permissions/ServerOperator;)Lorg/bukkit/permissions/PermissibleBase;"))
    private PermissibleBase arclight$mod$forwardPerm(ServerOperator opable) {
        if (FeltConfig.spec().getCompat().isForwardPermissionReverse()) {
            return new FeltFabricPermissible(opable);
        } else {
            return new PermissibleBase(opable);
        }
    }
}
